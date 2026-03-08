package edu.esi.ds.esientradas.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.EscenarioDao;
import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.dto.DtoEntradas;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;

@Service
public class BusquedaService {

    @Autowired
    private EscenarioDao dao;

    @Autowired
    private EspectaculoDao espectaculoDao;

    @Autowired
    private EntradaDao entradaDao;

    public List<Escenario> getEscenarios() {
        return this.dao.findAll();
    }

    public List<Espectaculo> getEspectaculosPorArtista(String artista) {
        return this.espectaculoDao.findByArtista(artista);
    }
    public List<Espectaculo> getEspectaculos(Long idEscenario) {
        return this.espectaculoDao.findByEscenarioId(idEscenario);
    }

    public List<Entrada> getEntradas(Long espectaculoId) {
        return this.entradaDao.findByEspectaculoId(espectaculoId);
    }

    public Integer getNumeroDeEntradas(Long espectaculoId) {
        return this.entradaDao.countByEspectaculoId(espectaculoId);
    }

    public Integer getEntradasLibres(Long espectaculoId) {
        return this.entradaDao.countByEspectaculoIdAndEstado(espectaculoId);
    }

    public DtoEntradas getNumeroDeEntradasComoDto(Long espectaculoId) {
        Object o = this.entradaDao.getNumeroDeEntradasComoDto(espectaculoId);
        // Cast the result to DtoEntradas (assuming the query returns an array of objects)
        DtoEntradas dto = new DtoEntradas();
        Object[] row = (Object[]) o;
        dto.setTotal(((Number) row[0]).intValue());
        dto.setLibres(((Number) row[1]).intValue());    
        dto.setReservadas(((Number) row[2]).intValue());
        dto.setVendidas(((Number) row[3]).intValue());
        return dto;
    }

}
