package co.edu.javeriana.prestamos.service;

import co.edu.javeriana.prestamos.exception.BusinessException;
import co.edu.javeriana.prestamos.model.Reserva;
import co.edu.javeriana.prestamos.notification.NotificationEvent;
import co.edu.javeriana.prestamos.notification.NotificationService;
import co.edu.javeriana.prestamos.repository.ConfiguracionRepository;
import co.edu.javeriana.prestamos.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReservationService {
    public static final int ESTADO_PENDIENTE = 1;
    public static final int ESTADO_DISPONIBLE = 2;
    public static final int ESTADO_CANCELADA = 3;
    public static final int ESTADO_COMPLETADA = 4;

    private final ReservaRepository reservaRepository;
    private final MappingService mappingService;
    private final CatalogClient catalogClient;
    private final NotificationService notificationService;
    private final ConfiguracionRepository configuracionRepository;
    private final Map<String, Integer> configCache = new ConcurrentHashMap<>();

    private static final String CFG_DIAS_EXPIRACION = "dias_expiracion_reserva";

    public ReservationService(ReservaRepository reservaRepository,
                              MappingService mappingService,
                              CatalogClient catalogClient,
                              NotificationService notificationService,
                              ConfiguracionRepository configuracionRepository) {
        this.reservaRepository = reservaRepository;
        this.mappingService = mappingService;
        this.catalogClient = catalogClient;
        this.notificationService = notificationService;
        this.configuracionRepository = configuracionRepository;
    }

    @Transactional
    public Reserva createReservation(Integer usuarioId, Integer libroIdG3) {
        expireStaleReservations();

        CatalogClient.BookDto book = catalogClient.getBook(String.valueOf(libroIdG3));
        if (book == null) {
            throw new BusinessException("Error 400: Libro no encontrado en catálogo");
        }
        Integer disp = book.cantidadDisponible;
        if (disp != null && disp > 0) {
            throw new BusinessException("Error 400: Aún hay unidades disponibles, no se requiere reserva");
        }

        Integer libroIdDb = mappingService.mapToDbId(libroIdG3);
        if (libroIdDb == null) {
            libroIdDb = libroIdG3;
        }

        boolean yaTiene = reservaRepository.existsActiveOrNotified(
                usuarioId, libroIdDb, List.of(ESTADO_PENDIENTE, ESTADO_DISPONIBLE));
        if (yaTiene) {
            throw new BusinessException("Error 409: El usuario ya tiene una reserva activa para este libro");
        }

        Reserva r = new Reserva(null, usuarioId, libroIdDb, ESTADO_PENDIENTE);
        return reservaRepository.save(r);
    }

    public List<Reserva> getMyReservations(Integer usuarioId) {
        expireStaleReservations();
        return reservaRepository.findByUsuario(usuarioId);
    }

    @Transactional
    public void cancelReservation(Integer reservaId, Integer requesterId, boolean isPrivileged) {
        expireStaleReservations();

        Reserva r = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new BusinessException("Error 404: Reserva no encontrada"));
        if (!isPrivileged && !r.getId_usuario().equals(requesterId)) {
            throw new BusinessException("Error 403: No autorizado");
        }
        if (r.getId_estado_reserva() == ESTADO_CANCELADA) {
            return;
        }
        r.setId_estado_reserva(ESTADO_CANCELADA);
        reservaRepository.save(r);
    }

    @Transactional
    public void notifyNextInQueue(Integer libroIdDb) {
        expireStaleReservations();
        List<Reserva> queue = reservaRepository.findQueueByLibroAndEstado(libroIdDb, ESTADO_PENDIENTE);
        if (queue.isEmpty()) {
            return;
        }
        Reserva next = queue.get(0);
        next.setId_estado_reserva(ESTADO_DISPONIBLE);
        reservaRepository.save(next);

        notificationService.publish(NotificationEvent.of(
                "RESERVA_NOTIFICADA",
                Map.of(
                        "reservaId", next.getId_reserva(),
                        "usuarioId", next.getId_usuario(),
                        "libroId", next.getId_libro()
                )
        ));
    }

    @Transactional
    public int expireReservations(long dias) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(dias);
        List<Reserva> candidates = reservaRepository.findOlderThanWithEstado(ESTADO_DISPONIBLE, cutoff);
        int count = 0;
        for (Reserva r : candidates) {
            r.setId_estado_reserva(ESTADO_CANCELADA);
            reservaRepository.save(r);
            count++;
        }
        return count;
    }

    public List<Reserva> listAll(Integer estado) {
        expireStaleReservations();
        return reservaRepository.findAllByEstadoOptional(estado);
    }

    private void expireStaleReservations() {
        long diasExpiracion = getConfigInt(CFG_DIAS_EXPIRACION, 2);
        expireReservations(diasExpiracion);
    }

    private int getConfigInt(String key, int fallback) {
        return configCache.computeIfAbsent(key, k ->
                configuracionRepository.findByClave(k)
                        .map(cfg -> parseConfigValue(k, cfg.getValor(), fallback))
                        .orElse(fallback)
        );
    }

    private int parseConfigValue(String key, String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Valor inválido para configuración '" + key + "': " + raw);
        }
    }
}
