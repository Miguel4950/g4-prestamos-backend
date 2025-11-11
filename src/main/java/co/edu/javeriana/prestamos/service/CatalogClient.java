package co.edu.javeriana.prestamos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class CatalogClient {

    private final RestTemplate restTemplate = new RestTemplate();

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
     * Intenta reservar una unidad del libro en el Catálogo (G3):
     * - Lee el libro; si no existe o no hay disponibilidad, retorna false.
     * - Hace PUT con cantidadDisponible decrecida en 1.
     */
    public boolean reservarUno(String id) {
        BookDto book = getBook(id);
        if (book == null || book.cantidadDisponible == null || book.cantidadDisponible <= 0) {
            return false;
        }
        int nueva = Math.max(0, book.cantidadDisponible - 1);
        try {
            String url = baseUrl + "/api/books/" + id;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("cantidadDisponible", nueva);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    /** Devuelve una unidad al Catálogo (incrementa disponibilidad en +1). */
    public boolean devolverUno(String id) {
        BookDto book = getBook(id);
        if (book == null || book.cantidadTotal == null) {
            return false;
        }
        int total = Math.max(0, book.cantidadTotal);
        int disp = book.cantidadDisponible == null ? 0 : book.cantidadDisponible;
        int nueva = Math.min(total, disp + 1);
        try {
            String url = baseUrl + "/api/books/" + id;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("cantidadDisponible", nueva);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    public static class BookDto {
        public String id;
        public String titulo;
        public String autor;
        public Integer cantidadTotal;
        public Integer cantidadDisponible;
        public String categoria;
    }
}
