package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.esi.ds.esientradas.dto.DtoReservaRequest;
import edu.esi.ds.esientradas.dto.DtoReservaResponse;
import edu.esi.ds.esientradas.services.ReservasService;
import jakarta.servlet.http.HttpSession;



@RestController
@RequestMapping("/reservas")
public class ReservasController {

    @Autowired
    private ReservasService service;
    
    @PutMapping("/reservar")
    public Long reservar(HttpSession session, @RequestParam Long idEntrada) {
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
    public DtoReservaResponse reservarLote(HttpSession session, @RequestBody DtoReservaRequest request) {
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
}
