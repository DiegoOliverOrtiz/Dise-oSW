package edu.esi.ds.esientradas.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.esi.ds.esientradas.model.Token;


public interface TokenDao extends JpaRepository<Token, String> { //Entidad que se gestiona y su clave (En tipo variable)
    @Query("SELECT t FROM Token t JOIN FETCH t.entrada WHERE t.sesionId = :sesionId")
    List<Token> findBySesionId(@Param("sesionId") String sesionId);

    void deleteBySesionId(String sesionId);
}
