package edu.esi.ds.esientradas.dao;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Estado;

public interface EntradaDao extends JpaRepository<Entrada, Long> { //Entidad que se gestiona y su clave (En tipo variable)
    List<Entrada> findByEspectaculoId(Long espectaculoId);
    List<Entrada> findByEspectaculoIdAndEstadoOrderByIdAsc(Long espectaculoId, Estado estado);

    @Query(value = "UPDATE Entrada e SET e.estado = :estado WHERE e.id = :idEntrada")
    @Modifying
    void updateEstado(@Param("idEntrada") Long idEntrada, @Param("estado") Estado estado);

    public Integer countByEspectaculoId(Long espectaculoId);

    public Integer countByEspectaculoIdAndEstado(Long espectaculoId, Estado estado);

    @Query("SELECT " +
            "COUNT(e) AS total, " +
            "SUM(CASE WHEN e.estado = 'DISPONIBLE' THEN 1 ELSE 0 END) AS libres, " +
            "SUM(CASE WHEN e.estado = 'RESERVADA' THEN 1 ELSE 0 END) AS reservadas, " +
            "SUM(CASE WHEN e.estado = 'VENDIDA' THEN 1 ELSE 0 END) AS vendidas " +
            "FROM Entrada e " +
            "WHERE e.espectaculo.id = :espectaculoId")
    Object getNumeroDeEntradasComoDto(@Param("espectaculoId") Long espectaculoId);
}
