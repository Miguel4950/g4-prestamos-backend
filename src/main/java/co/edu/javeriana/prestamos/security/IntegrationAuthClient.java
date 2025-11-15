package co.edu.javeriana.prestamos.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cliente simple para obtener un JWT desde el servicio de autenticación (G2)
 * usando un usuario técnico. El token se cachea para evitar solicitarlo en
 * cada préstamo y se renueva automáticamente cuando está por expirar.
 */
@Component
public class IntegrationAuthClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationAuthClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final AtomicReference<String> cachedToken = new AtomicReference<>(null);
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    private final String authBaseUrl;
    private final String username;
    private final String password;

    public IntegrationAuthClient(
            @Value("${auth.base-url:http://localhost:8080/api/auth}") String authBaseUrl,
            @Value("${integration.user:}") String username,
            @Value("${integration.password:}") String password) {
        this.authBaseUrl = authBaseUrl;
        this.username = username;
        this.password = password;
    }

    public String getToken() {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return null;
        }
        if (Instant.now().isAfter(tokenExpiresAt.minus(Duration.ofMinutes(1)))) {
            synchronized (this) {
                if (Instant.now().isAfter(tokenExpiresAt.minus(Duration.ofMinutes(1)))) {
                    refreshToken();
                }
            }
        }
        return cachedToken.get();
    }

    private void refreshToken() {
        try {
            String url = authBaseUrl + "/login";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> payload = Map.of(
                    "username", username,
                    "contrasena", password
            );
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(url, entity, LoginResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                cachedToken.set(response.getBody().token);
                tokenExpiresAt = Instant.now().plus(Duration.ofHours(6));
                LOGGER.info("Token de integración renovado correctamente");
            } else {
                throw new IllegalStateException("Respuesta no válida del servicio de autenticación");
            }
        } catch (Exception e) {
            LOGGER.error("No se pudo obtener token de autenticación para integración", e);
            cachedToken.set(null);
            tokenExpiresAt = Instant.EPOCH;
        }
    }

    private static class LoginResponse {
        public String token;
    }
}
