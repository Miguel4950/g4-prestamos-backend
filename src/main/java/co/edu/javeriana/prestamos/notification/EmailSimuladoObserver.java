package co.edu.javeriana.prestamos.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailSimuladoObserver implements NotificationObserver {
    private static final Logger log = LoggerFactory.getLogger(EmailSimuladoObserver.class);

    @Override
    public void onEvent(NotificationEvent event) {
        if ("RESERVA_NOTIFICADA".equalsIgnoreCase(event.getType())) {
            log.info("[EMAIL] Enviando correo simulado: {}", event.getPayload());
        }
    }
}

