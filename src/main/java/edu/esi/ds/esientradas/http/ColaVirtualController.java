package edu.esi.ds.esientradas.http;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoColaEstado;
import edu.esi.ds.esientradas.services.ColaVirtualService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/colas")
@Validated
public class ColaVirtualController {
    private final ColaVirtualService colaVirtualService;

    public ColaVirtualController(ColaVirtualService colaVirtualService) {
        this.colaVirtualService = colaVirtualService;
    }

    @PostMapping("/join")
    public DtoColaEstado join(HttpSession session, @RequestParam @Positive Long espectaculoId) {
        return colaVirtualService.join(espectaculoId, session.getId());
    }

    @GetMapping("/status")
    public DtoColaEstado status(HttpSession session, @RequestParam @Positive Long espectaculoId) {
        return colaVirtualService.status(espectaculoId, session.getId());
    }

    @DeleteMapping("/leave")
    public void leave(HttpSession session, @RequestParam @Positive Long espectaculoId) {
        colaVirtualService.leave(espectaculoId, session.getId());
    }
}
