package co.edu.javeriana.prestamos.notification;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final List<NotificationObserver> observers;

    public void publish(NotificationEvent event) {
        if (observers == null || observers.isEmpty()) {
            log.debug("No hay observadores registrados para notificaciones: {}", event.getType());
            return;
        }
        for (NotificationObserver obs : observers) {
            try {
                obs.onEvent(event);
            } catch (Exception ex) {
                log.warn("Observer {} fallo procesando evento {}", obs.getClass().getSimpleName(), event.getType(), ex);
            }
        }
    }
}

