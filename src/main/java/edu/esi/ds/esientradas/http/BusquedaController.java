package edu.esi.ds.esientradas.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoEntradaCompra;
import edu.esi.ds.esientradas.dto.DtoEntradas;
import edu.esi.ds.esientradas.dto.DtoEspectaculo;
import edu.esi.ds.esientradas.dto.DtoEspectaculoDetalle;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.services.BusquedaService;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;


@RestController
@RequestMapping("/busqueda")
@Validated
public class BusquedaController {

    @Autowired //Cuando arranca el servicio en esta clase lo crea, si lo encuentra en otro lado no lo hace
    private BusquedaService service;

    @GetMapping("/getEspectaculos")
    public List<DtoEspectaculo> getEspectaculos(@RequestParam(required = false) @Size(max = 80) String artista) {
        List<Espectaculo> espectaculos = this.service.getEspectaculosPorArtista(safeText(artista, 80));
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
    public List<DtoEspectaculo> getEspectaculos(@PathVariable @Positive Long idEscenario) {
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
    public List<Entrada> getEntradas(@RequestParam @Positive Long espectaculoId) {
        return this.service.getEntradas(espectaculoId);
    }

    @GetMapping("/getEntradasDisponibles")
    public List<DtoEntradaCompra> getEntradasDisponibles(@RequestParam @Positive Long espectaculoId) {
        return this.service.getEntradasDisponibles(espectaculoId);
    }

    @GetMapping("/getNumeroDeEntradas")
    public Integer getNumeroDeEntradas(@RequestParam @Positive Long espectaculoId) {
        return this.service.getNumeroDeEntradas(espectaculoId);
    }

    @GetMapping("/getNumeroDeEntradasComoDto")
    public DtoEntradas getNumeroDeEntradasComoDto(@RequestParam @Positive Long espectaculoId) {
        DtoEntradas dto = this.service.getNumeroDeEntradasComoDto(espectaculoId);
        
        return dto;
    }

    @GetMapping("/getEntradasLibres")
    public Integer getEntradasLibres(@RequestParam @Positive Long espectaculoId) {
        return this.service.getEntradasLibres(espectaculoId);
    }

    @GetMapping("/espectaculos/{id}/detalle")
    public DtoEspectaculoDetalle getEspectaculoDetalle(@PathVariable @Positive Long id) {
        Espectaculo e = this.service.getEspectaculoById(id);
        DtoEspectaculoDetalle dto = new DtoEspectaculoDetalle();
        dto.setId(e.getId());
        dto.setArtista(e.getArtista());
        dto.setFecha(e.getFecha());
        dto.setEscenario(e.getEscenario().getNombre());
        dto.setEntradas(this.service.getEntradas(id));
        return dto;
    }

    @GetMapping("/saludar/{nombre}")
    public String saludar(@PathVariable @Size(max = 80) String nombre, @RequestParam @Size(max = 80) String apellido) {
        return "Hola " + safeText(nombre, 80) + " " + safeText(apellido, 80) + " bienvenido a ESI Entradas";
    }

    private String safeText(String value, int maxLength) {
        String cleaned = value == null ? "" : value.strip().replaceAll("\\p{Cntrl}", "");
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }
}
