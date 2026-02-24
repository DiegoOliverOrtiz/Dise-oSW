package edu.esi.ds.esientradas.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Estado;
import org.springframework.web.server.ResponseStatusException;


@Service
public class ReservasService {
    @Autowired
    private EntradaDao dao;

    @Transactional
    public Long reservar(Long idEntrada) {
        Entrada entrada = this.dao.findById(idEntrada).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));
        if(entrada.getEstado() != Estado.DISPONIBLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La entrada no está disponible para reservar");
        }
        //entrada.setEstado(Estado.RESERVADA);
        //this.dao.save(entrada);
        this.dao.updateEstado(idEntrada, Estado.RESERVADA);
        return entrada.getPrecio();
    }
}