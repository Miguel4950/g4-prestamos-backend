package co.edu.javeriana.prestamos.service;

import co.edu.javeriana.prestamos.security.IntegrationAuthClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
public class CatalogClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final IntegrationAuthClient integrationAuthClient;

    public CatalogClient(IntegrationAuthClient integrationAuthClient) {
        this.integrationAuthClient = integrationAuthClient;
    }

    @Value("${catalog.base-url:http://localhost:8080}")
    private String baseUrl;

    public BookDto getBook(String id) {
        try {
            String url = baseUrl + "/api/books/" + id;
            return restTemplate.getForObject(url, BookDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Intenta reservar una unidad del libro en el Catálogo (G3).
     */
    public boolean reservarUno(String id) {
        BookDto book = getBook(id);
        if (book == null || book.cantidadDisponible == null || book.cantidadDisponible <= 0) {
            return false;
        }
        return updateAvailability(id, -1);
    }

    /** Devuelve una unidad al Catálogo (incrementa disponibilidad en +1). */
    public boolean devolverUno(String id) {
        return updateAvailability(id, +1);
    }

    public static class BookDto {
        public String id;
        public String titulo;
        public String autor;
        public Integer cantidadTotal;
        public Integer cantidadDisponible;
        public String categoria;
    }

    private boolean updateAvailability(String id, int change) {
        try {
            String token = integrationAuthClient.getToken();
            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isBlank()) {
                headers.setBearerAuth(token);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            URI uri = URI.create(baseUrl + "/api/books/" + id + "/availability?change=" + change);
            ResponseEntity<Void> resp = restTemplate.exchange(uri, HttpMethod.PUT, entity, Void.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
