package edu.esi.ds.esientradas.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esientradas.model.Espectaculo;


public interface EspectaculoDao extends JpaRepository<Espectaculo, Long> { //Entidad que se gestiona y su clave (En tipo variable)
     List<Espectaculo> findByArtista(String artista);
}
