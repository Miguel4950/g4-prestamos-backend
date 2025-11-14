package co.edu.javeriana.prestamos.service;

import co.edu.javeriana.prestamos.exception.BusinessException;
import co.edu.javeriana.prestamos.model.Reserva;
import co.edu.javeriana.prestamos.notification.NotificationEvent;
import co.edu.javeriana.prestamos.notification.NotificationService;
import co.edu.javeriana.prestamos.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ReservationService {
    public static final int ESTADO_PENDIENTE = 1;
    public static final int ESTADO_NOTIFICADO = 2;
    public static final int ESTADO_CANCELADA = 3;
    public static final int ESTADO_EXPIRADA = 4;

    private static final long RESERVATION_EXPIRATION_HOURS = 48;

    private final ReservaRepository reservaRepository;
    private final MappingService mappingService;
    private final CatalogClient catalogClient;
    private final NotificationService notificationService;

    public ReservationService(ReservaRepository reservaRepository,
                              MappingService mappingService,
                              CatalogClient catalogClient,
                              NotificationService notificationService) {
        this.reservaRepository = reservaRepository;
        this.mappingService = mappingService;
        this.catalogClient = catalogClient;
        this.notificationService = notificationService;
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
                usuarioId, libroIdDb, List.of(ESTADO_PENDIENTE, ESTADO_NOTIFICADO));
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
        next.setId_estado_reserva(ESTADO_NOTIFICADO);
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
    public int expireReservations(long hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        List<Reserva> candidates = reservaRepository.findOlderThanWithEstado(ESTADO_NOTIFICADO, cutoff);
        int count = 0;
        for (Reserva r : candidates) {
            r.setId_estado_reserva(ESTADO_EXPIRADA);
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
        expireReservations(RESERVATION_EXPIRATION_HOURS);
    }
}

