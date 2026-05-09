package edu.esi.ds.esientradas.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {
    @Value("${app.esiusuarios.url:http://localhost:8081}")
    private String usuariosBaseUrl;

    @Value("${app.internal.api.secret:}")
    private String internalApiSecret;

    public String checkToken(String userToken) {
        String endpoint = usuariosBaseUrl + "/external/checkToken";
        RestTemplate rest = new RestTemplate();
        try{
            HttpHeaders headers = new HttpHeaders();
            if (internalApiSecret != null && !internalApiSecret.isBlank()) {
                headers.set("X-Internal-Secret", internalApiSecret);
            }
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("token", userToken), headers);
            String username = rest.postForObject(endpoint, entity, String.class);
            if(username == null || username.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
            }
            return username;
        }
        catch(HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
        catch(HttpClientErrorException.Forbidden e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No se pudo validar el token de usuario");
        }
        catch(RestClientException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al validar el token");

        }
    }
}
