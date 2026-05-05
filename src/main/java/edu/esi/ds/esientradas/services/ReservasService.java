package edu.esi.ds.esientradas.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.TokenDao;
import edu.esi.ds.esientradas.dto.DtoReservaResponse;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Estado;
import edu.esi.ds.esientradas.model.Token;


@Service
public class ReservasService {
    @Autowired
    private EntradaDao dao;
    @Autowired
    private TokenDao tokenDao;

    @Transactional
    public Long reservar(Long idEntrada, String sesionId) {
        // Intentar reservar de forma condicional en la base de datos para evitar carreras
        int updated = this.dao.updateEstadoIf(idEntrada, Estado.RESERVADA, Estado.DISPONIBLE);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La entrada no está disponible para reservar");
        }
        Entrada entrada = this.dao.findById(idEntrada).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));
        Token token = new Token();
        token.setEntrada(entrada);
        token.setSesionId(sesionId);
        this.tokenDao.save(token);
        return entrada.getPrecio();
    }

    @Transactional
    public Long reservarEntradas(List<Long> entradaIds, String sesionId) {
        if (entradaIds == null || entradaIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes seleccionar al menos una entrada");
        }
        long precioTotal = 0L;
        for (Long idEntrada : entradaIds) {
            precioTotal += this.reservar(idEntrada, sesionId);
        }
        return precioTotal;
    }

    public DtoReservaResponse getResumen(String sesionId) {
        List<Token> tokens = this.tokenDao.findBySesionId(sesionId);
        long precioTotal = tokens.stream().mapToLong(t -> t.getEntrada().getPrecio()).sum();
        return new DtoReservaResponse(precioTotal, tokens.size());
    }
}
