package edu.esi.ds.esientradas.services;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.TokenDao;
import edu.esi.ds.esientradas.dto.DtoPagoIntent;
import edu.esi.ds.esientradas.dto.DtoPagoResultado;
import edu.esi.ds.esientradas.model.Estado;
import edu.esi.ds.esientradas.model.Token;

@Service
public class PagosService {

    private static final Logger log = LoggerFactory.getLogger(PagosService.class);

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.public-key:}")
    private String stripePublicKey;

    @Autowired
    private TokenDao tokenDao;

    @Autowired
    private EntradaDao entradaDao;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private edu.esi.ds.esientradas.dao.EmailQueueDao emailQueueDao;

    @Transactional(readOnly = true)
    public DtoPagoIntent crearIntentoPago(String sesionId, String userEmail) {
        List<Token> reservas = this.getReservas(sesionId);
        long amount = reservas.stream().mapToLong(token -> token.getEntrada().getPrecio()).sum();

        this.configurarStripe();

        try {
            PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency("eur")
                    .addPaymentMethodType("card")
                    .setDescription("Compra de entradas")
                    .putMetadata("sessionId", sesionId)
                    .putMetadata("entryIds", this.serializeEntryIds(reservas));

            if (userEmail != null && !userEmail.isBlank()) {
                builder.putMetadata("userEmail", userEmail);
            }

            PaymentIntentCreateParams params = builder.build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            return new DtoPagoIntent(
                    stripePublicKey,
                    paymentIntent.getClientSecret(),
                    paymentIntent.getId(),
                    paymentIntent.getAmount(),
                    paymentIntent.getCurrency());
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo crear el pago en Stripe");
        }
    }

    @Transactional
    public DtoPagoResultado confirmarPago(String sesionId, String paymentIntentId) {
        this.configurarStripe();
        PaymentIntent paymentIntent = this.recuperarPago(paymentIntentId);
        this.validarSesion(paymentIntent, sesionId);

        List<Token> reservas = this.tokenDao.findBySesionId(sesionId);
        if ("succeeded".equals(paymentIntent.getStatus())) {
            if (!reservas.isEmpty()) {
                for (Token token : reservas) {
                    int updated = this.entradaDao.updateEstadoIf(token.getEntrada().getId(), Estado.VENDIDA, Estado.RESERVADA);
                    if (updated == 0) {
                        // Alguna entrada ya no estaba reservada por esta sesión — rollback
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo confirmar la venta: alguna entrada no estaba reservada");
                    }
                }
                this.tokenDao.deleteBySesionId(sesionId);
            }

            boolean emailScheduled = false;
            String userEmail = paymentIntent.getMetadata() == null ? null : paymentIntent.getMetadata().get("userEmail");
            if (userEmail != null && !userEmail.isBlank()) {
                String html = generarHtmlTicket(reservas);
                // Persistir outbox dentro de la transacción y publicar el id para el worker
                edu.esi.ds.esientradas.model.EmailQueue q = new edu.esi.ds.esientradas.model.EmailQueue();
                q.setToAddress(userEmail);
                q.setSubject("Tus entradas - ESI Entradas");
                q.setBodyHtml(html);
                q.setReference(paymentIntentId);
                q = this.emailQueueDao.save(q);
                this.publisher.publishEvent(new edu.esi.ds.esientradas.events.EmailQueueCreatedEvent(q.getId()));
                emailScheduled = true;
            } else {
                log.info("No hay userEmail en el metadata del paymentIntent {}", paymentIntentId);
            }

                String message = "Pago confirmado" + (emailScheduled ? " - email encolado" : " - email no encolado");
            return new DtoPagoResultado(
                    paymentIntent.getStatus(),
                    message,
                    this.amountOrReceived(paymentIntent),
                    paymentIntent.getCurrency(),
                    reservas.size());
        }

        return new DtoPagoResultado(
                paymentIntent.getStatus(),
                "El pago aun no se ha completado",
                this.amountOrReceived(paymentIntent),
                paymentIntent.getCurrency(),
                0);
    }

    @Transactional
    public void cancelarPago(String sesionId) {
        List<Token> reservas = this.tokenDao.findBySesionId(sesionId);
        for (Token token : reservas) {
            this.entradaDao.updateEstado(token.getEntrada().getId(), Estado.DISPONIBLE);
        }
        if (!reservas.isEmpty()) {
            this.tokenDao.deleteBySesionId(sesionId);
        }
    }

    private List<Token> getReservas(String sesionId) {
        List<Token> reservas = this.tokenDao.findBySesionId(sesionId);
        if (reservas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay entradas reservadas para pagar");
        }
        return reservas;
    }

    private void configurarStripe() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falta configurar STRIPE_SECRET_KEY");
        }
        if (stripePublicKey == null || stripePublicKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falta configurar STRIPE_PUBLIC_KEY");
        }
        Stripe.apiKey = stripeSecretKey;
    }

    private PaymentIntent recuperarPago(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo consultar el pago en Stripe");
        }
    }

    private void validarSesion(PaymentIntent paymentIntent, String sesionId) {
        String paymentSessionId = paymentIntent.getMetadata().get("sessionId");
        if (paymentSessionId == null || !paymentSessionId.equals(sesionId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ese pago no pertenece a la sesion actual");
        }
    }

    private String serializeEntryIds(List<Token> reservas) {
        return reservas.stream()
                .map(token -> token.getEntrada().getId().toString())
                .collect(Collectors.joining(","));
    }

    private long amountOrReceived(PaymentIntent paymentIntent) {
        Long amountReceived = paymentIntent.getAmountReceived();
        if (amountReceived != null && amountReceived > 0) {
            return amountReceived;
        }
        return paymentIntent.getAmount();
    }

    private String generarHtmlTicket(List<Token> reservas) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h2>Tus entradas - ESI Entradas</h2>");
        sb.append("<ul>");
        for (Token token : reservas) {
            if (token.getEntrada() == null) continue;
            var entrada = token.getEntrada();
            var espectaculo = entrada.getEspectaculo();
            sb.append("<li>");
            sb.append("<strong>Espectáculo:</strong> ");
            sb.append(espectaculo != null ? espectaculo.getArtista() : "-");
            sb.append("<br/>");
            sb.append("<strong>Fecha:</strong> ");
            sb.append(espectaculo != null && espectaculo.getFecha() != null ? espectaculo.getFecha().toString() : "-");
            sb.append("<br/>");
            if (entrada instanceof edu.esi.ds.esientradas.model.Precisa) {
                edu.esi.ds.esientradas.model.Precisa p = (edu.esi.ds.esientradas.model.Precisa) entrada;
                sb.append("Asiento: planta " + p.getPlanta() + ", fila " + p.getFila() + ", columna " + p.getColumna());
            } else if (entrada instanceof edu.esi.ds.esientradas.model.DeZona) {
                edu.esi.ds.esientradas.model.DeZona z = (edu.esi.ds.esientradas.model.DeZona) entrada;
                sb.append("Zona: " + z.getZona());
            } else {
                sb.append("Entrada id: " + entrada.getId());
            }
            sb.append("</li>");
        }
        sb.append("</ul>");
        sb.append("</body></html>");
        return sb.toString();
    }
}
