package edu.esi.ds.esientradas.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.esi.ds.esientradas.dao.EscenarioDao;
import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;

@Service
public class BusquedaService {

    @Autowired
    private EscenarioDao dao;

    @Autowired
    private EspectaculoDao espectaculoDao;

    public List<Escenario> getEscenarios() {
        return this.dao.findAll();
    }

    public List<Espectaculo> getEspectaculos(String artista) {
        return this.espectaculoDao.findByArtista(artista);
    }

}
