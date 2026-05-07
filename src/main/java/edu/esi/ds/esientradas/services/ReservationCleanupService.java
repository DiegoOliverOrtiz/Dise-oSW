package edu.esi.ds.esientradas.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.TokenDao;
import edu.esi.ds.esientradas.model.Estado;
import edu.esi.ds.esientradas.model.Token;

@Service
public class ReservationCleanupService {

    @Autowired
    private TokenDao tokenDao;

    @Autowired
    private EntradaDao entradaDao;

    @Value("${reservas.ttl.minutes:10}")
    private int ttlMinutes;

    @Scheduled(fixedDelayString = "${reservas.cleanup.ms:60000}")
    @Transactional
    public void cleanupExpiredTokens() {
        long threshold = System.currentTimeMillis() - (long) ttlMinutes * 60L * 1000L;
        List<Token> expired = this.tokenDao.findByHoraActivaLessThanEqual(threshold);
        if (expired == null || expired.isEmpty()) return;
        for (Token t : expired) {
            Long idEntrada = t.getEntrada().getId();
            // Solo devolver a DISPONIBLE si está RESERVADA (para no sobreescribir ventas)
            this.entradaDao.updateEstadoIf(idEntrada, Estado.DISPONIBLE.name(), Estado.RESERVADA.name());
            this.tokenDao.delete(t);
        }
    }
}
