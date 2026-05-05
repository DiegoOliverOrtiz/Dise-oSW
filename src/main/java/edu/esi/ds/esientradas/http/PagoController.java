package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoPagoIntent;
import edu.esi.ds.esientradas.dto.DtoPagoResultado;
import edu.esi.ds.esientradas.services.PagosService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/pagos")
public class PagoController {

    @Autowired
    private PagosService pagosService;

    @PostMapping("/payment-intent")
    public DtoPagoIntent createPaymentIntent(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        return this.pagosService.crearIntentoPago(session.getId(), userEmail);
    }

    @PostMapping("/confirmar")
    public DtoPagoResultado confirmarPago(HttpSession session, @RequestParam String paymentIntentId) {
        DtoPagoResultado resultado = this.pagosService.confirmarPago(session.getId(), paymentIntentId);
        if ("succeeded".equals(resultado.getStatus())) {
            session.removeAttribute("precioTotal");
        }
        return resultado;
    }

    @PostMapping("/cancelar")
    public void cancelarPago(HttpSession session) {
        this.pagosService.cancelarPago(session.getId());
        session.removeAttribute("precioTotal");
    }
}
