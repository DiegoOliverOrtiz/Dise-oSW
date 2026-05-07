package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.esi.ds.esientradas.dto.DtoReservaRequest;
import edu.esi.ds.esientradas.dto.DtoReservaResponse;
import edu.esi.ds.esientradas.services.ColaVirtualService;
import edu.esi.ds.esientradas.services.ReservasService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;



@RestController
@RequestMapping("/reservas")
@Validated
public class ReservasController {

    @Autowired
    private ReservasService service;

    @Autowired
    private ColaVirtualService colaVirtualService;
    
    @PutMapping("/reservar")
    public Long reservar(HttpSession session, @RequestParam @Positive Long idEntrada) {
        Long precioEntrada = this.service.reservar(idEntrada, session.getId());
        Long precioTotal = (Long) session.getAttribute("precioTotal");
        if(precioTotal == null) {
            precioTotal = precioEntrada;
            session.setAttribute("precioTotal", precioTotal);
        } else {
            precioTotal += precioEntrada;   
            session.setAttribute("precioTotal", precioTotal);
        }
        return precioTotal;
    }

    @PutMapping("/reservar-lote")
    public DtoReservaResponse reservarLote(
        HttpSession session,
        @Valid @RequestBody DtoReservaRequest request,
        @org.springframework.web.bind.annotation.RequestHeader(value = "X-Queue-Access", required = false) String queueAccessToken,
        @org.springframework.web.bind.annotation.RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        colaVirtualService.assertAccessForEntradas(request.getEntradaIds(), queueIdentity(session, queueClientId), queueAccessToken);
        Long precioEntrada = this.service.reservarEntradas(request.getEntradaIds(), session.getId());
        Long precioTotal = (Long) session.getAttribute("precioTotal");
        if (precioTotal == null) {
            precioTotal = precioEntrada;
        } else {
            precioTotal += precioEntrada;
        }
        session.setAttribute("precioTotal", precioTotal);
        return new DtoReservaResponse(precioTotal, request.getEntradaIds().size());
    }

    private String queueIdentity(HttpSession session, String queueClientId) {
        if (queueClientId == null || queueClientId.isBlank()) {
            return session.getId();
        }
        return session.getId() + ":" + queueClientId.strip();
    }

    @GetMapping("/summary")
    public DtoReservaResponse resumen(HttpSession session) {
        return this.service.getResumen(session.getId());
    }
}
