package edu.esi.ds.esientradas.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoEntradaCompra;
import edu.esi.ds.esientradas.dto.DtoEntradas;
import edu.esi.ds.esientradas.dto.DtoEspectaculo;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.services.BusquedaService;


@RestController
@RequestMapping("/busqueda")
public class BusquedaController {

    @Autowired //Cuando arranca el servicio en esta clase lo crea, si lo encuentra en otro lado no lo hace
    private BusquedaService service;

    @GetMapping("/getEspectaculos")
    public List<DtoEspectaculo> getEspectaculos(@RequestParam String artista) {
        List<Espectaculo> espectaculos = this.service.getEspectaculosPorArtista(artista);
        List<DtoEspectaculo> dtos = espectaculos.stream().map(e -> {
            DtoEspectaculo dto = new DtoEspectaculo();
            dto.setId(e.getId());
            dto.setArtista(e.getArtista());
            dto.setFecha(e.getFecha());
            dto.setEscenario(e.getEscenario().getNombre());
            return dto;
        }).toList();
        return dtos;
    }
    
    @GetMapping("/getEspectaculos/{idEscenario}")
    public List<DtoEspectaculo> getEspectaculos(@PathVariable Long idEscenario) {
        List<Espectaculo> espectaculos = this.service.getEspectaculos(idEscenario);
        List<DtoEspectaculo> dtos = espectaculos.stream().map(e -> {
            DtoEspectaculo dto = new DtoEspectaculo();
            dto.setId(e.getId());
            dto.setArtista(e.getArtista());
            dto.setFecha(e.getFecha());
            dto.setEscenario(e.getEscenario().getNombre());
            return dto;
        }).toList();
        return dtos;
    }

    @GetMapping("/getEscenarios")
    public List<Escenario> getEscenarios() {
        return this.service.getEscenarios();
    }

    @GetMapping("/getEntradas")
    public List<Entrada> getEntradas(@RequestParam Long espectaculoId) {
        return this.service.getEntradas(espectaculoId);
    }

    @GetMapping("/getEntradasDisponibles")
    public List<DtoEntradaCompra> getEntradasDisponibles(@RequestParam Long espectaculoId) {
        return this.service.getEntradasDisponibles(espectaculoId);
    }

    @GetMapping("/getNumeroDeEntradas")
    public Integer getNumeroDeEntradas(@RequestParam Long espectaculoId) {
        return this.service.getNumeroDeEntradas(espectaculoId);
    }

    @GetMapping("/getNumeroDeEntradasComoDto")
    public DtoEntradas getNumeroDeEntradasComoDto(@RequestParam Long espectaculoId) {
        DtoEntradas dto = this.service.getNumeroDeEntradasComoDto(espectaculoId);
        
        return dto;
    }

    @GetMapping("/getEntradasLibres")
    public Integer getEntradasLibres(@RequestParam Long espectaculoId) {
        return this.service.getEntradasLibres(espectaculoId);
    }

    @GetMapping("/saludar/{nombre}")
    public String saludar(@PathVariable String nombre,@RequestParam String apellido) {
        return "Hola "+ nombre + " " +apellido +" bienvenido a ESI Entradas";
    }
}
