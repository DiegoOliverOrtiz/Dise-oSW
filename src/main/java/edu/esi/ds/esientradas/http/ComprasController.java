package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dto.DtoPagoIntent;
import edu.esi.ds.esientradas.services.PagosService;
import edu.esi.ds.esientradas.services.UsuarioService;
import jakarta.servlet.http.HttpSession;



@RestController
@RequestMapping("/compras")
public class ComprasController {
    @Autowired 
    private UsuarioService usuariosService;
    @Autowired
    private PagosService pagosService;

    @PutMapping("/comprar")
    public DtoPagoIntent comprar(HttpSession session, @RequestParam String userToken) {
        if (userToken == null || userToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de usuario requerido para comprar");
        }

        String userEmail = this.usuariosService.checkToken(userToken);
        // Guardamos el email del usuario en la sesión para usarlo más adelante
        session.setAttribute("userEmail", userEmail);

        // Crear y devolver el intent de pago asociado a la sesión
        return this.pagosService.crearIntentoPago(session.getId(), userEmail);
    }
}
