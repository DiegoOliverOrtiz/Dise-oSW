package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public Long reservar(
        HttpSession session,
        @RequestParam @Positive Long idEntrada,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        String identidad = reservaIdentity(session, queueClientId);
        this.service.reservar(idEntrada, identidad);
        return this.service.getResumen(identidad).getPrecioTotal();
    }

    @PutMapping("/reservar-lote")
    public DtoReservaResponse reservarLote(
        HttpSession session,
        @Valid @RequestBody DtoReservaRequest request,
        @RequestHeader(value = "X-Queue-Access", required = false) String queueAccessToken,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        String identidad = reservaIdentity(session, queueClientId);

        colaVirtualService.assertAccessForEntradas(
            request.getEntradaIds(),
            identidad,
            queueAccessToken
        );

        this.service.reservarEntradas(request.getEntradaIds(), identidad);
        return this.service.getResumen(identidad);
    }

    @GetMapping("/summary")
    public DtoReservaResponse resumen(
        HttpSession session,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        return this.service.getResumen(reservaIdentity(session, queueClientId));
    }

    private String reservaIdentity(HttpSession session, String queueClientId) {
        if (queueClientId == null || queueClientId.isBlank()) {
            return session.getId();
        }
        return session.getId() + ":" + queueClientId.strip();
    }
}
