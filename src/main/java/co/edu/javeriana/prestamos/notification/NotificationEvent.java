package co.edu.javeriana.prestamos.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String type;
    private Map<String, Object> payload;
    private Instant timestamp;

    public static NotificationEvent of(String type, Map<String, Object> payload) {
        return new NotificationEvent(type, payload, Instant.now());
    }
}

