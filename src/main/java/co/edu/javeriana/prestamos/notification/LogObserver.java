package co.edu.javeriana.prestamos.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogObserver implements NotificationObserver {
    private static final Logger log = LoggerFactory.getLogger(LogObserver.class);

    @Override
    public void onEvent(NotificationEvent event) {
        log.info("[NOTIF] {} -> {}", event.getType(), event.getPayload());
    }
}

