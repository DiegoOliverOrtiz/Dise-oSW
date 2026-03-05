package edu.esi.ds.esientradas.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esientradas.model.Token;


public interface TokenDao extends JpaRepository<Token, String> { //Entidad que se gestiona y su clave (En tipo variable)
}