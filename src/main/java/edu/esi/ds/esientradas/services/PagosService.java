package edu.esi.ds.esientradas.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.public-key:}")
    private String stripePublicKey;

    @Autowired
    private TokenDao tokenDao;

    @Autowired
    private EntradaDao entradaDao;

    @Transactional(readOnly = true)
    public DtoPagoIntent crearIntentoPago(String sesionId) {
        List<Token> reservas = this.getReservas(sesionId);
        long amount = reservas.stream().mapToLong(token -> token.getEntrada().getPrecio()).sum();

        this.configurarStripe();

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency("eur")
                    .addPaymentMethodType("card")
                    .setDescription("Compra de entradas")
                    .putMetadata("sessionId", sesionId)
                    .putMetadata("entryIds", this.serializeEntryIds(reservas))
                    .build();

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
                    this.entradaDao.updateEstado(token.getEntrada().getId(), Estado.VENDIDA);
                }
                this.tokenDao.deleteBySesionId(sesionId);
            }
            return new DtoPagoResultado(
                    paymentIntent.getStatus(),
                    "Pago confirmado",
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
}
