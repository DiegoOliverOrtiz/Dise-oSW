package edu.esi.ds.esientradas.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {
    public String checkToken(String userToken) {
        String endpoint = "http://localhost:8081/external/checkToken";
        RestTemplate rest = new RestTemplate();
        try{
            String username = rest.getForObject(endpoint+"/"+userToken, String.class);
            if(username == null || username.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido");
            }
            return username;
        }
        catch(RestClientException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al validar el token");

        }
    }
}
