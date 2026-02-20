package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.services.EscenarioService;


@RestController
@RequestMapping("/escenarios")
public class EscenarioController {

    @Autowired
    private EscenarioService service;
    
    /*
    @Autowired //Si lo decalro como autowired se gestiona solo y se hacen solos
    private EscenarioDao dao;
    */
    
    @PostMapping("/insertar")
    public void insertar(@RequestBody Escenario escenario) {
        if(escenario.getNombre() == null || escenario.getNombre().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del escenario no puede ser nulo o vacío");
        }
        if(escenario.getDescripcion() == null || escenario.getDescripcion().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción del escenario no puede ser nula o vacía");
        }
        this.service.insertar(escenario);
    }

    /*
    @PostMapping("/insertar")
    public void InsertarEscenario(@RequestBody Escenario escenario) {
        System.out.println("Insertar escenario: " + escenario.getNombre());
        System.out.println("Descripcion escenario: " + escenario.getDescripcion());
    } */
}
