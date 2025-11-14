package co.edu.javeriana.prestamos.service;

import co.edu.javeriana.prestamos.model.Reserva;
import co.edu.javeriana.prestamos.notification.NotificationService;
import co.edu.javeriana.prestamos.repository.ConfiguracionRepository;
import co.edu.javeriana.prestamos.repository.ReservaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReservationServiceExpireTest {

    @Test
    void expireReservations_cancelaDisponiblesAntiguos() {
        ReservaRepository repo = mock(ReservaRepository.class);
        MappingService mapping = mock(MappingService.class);
        CatalogClient client = mock(CatalogClient.class);
        NotificationService notif = mock(NotificationService.class);
        ConfiguracionRepository configuracionRepository = mock(ConfiguracionRepository.class);

        Reserva r1 = new Reserva(1, 7, 96, ReservationService.ESTADO_DISPONIBLE);
        r1.setFecha_reserva(LocalDateTime.now().minusDays(5));
        Reserva r2 = new Reserva(2, 8, 97, ReservationService.ESTADO_DISPONIBLE);
        r2.setFecha_reserva(LocalDateTime.now().minusDays(4));

        when(repo.findOlderThanWithEstado(eq(ReservationService.ESTADO_DISPONIBLE), any(LocalDateTime.class)))
                .thenReturn(List.of(r1, r2));
        when(repo.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationService service = new ReservationService(repo, mapping, client, notif, configuracionRepository);
        int canceladas = service.expireReservations(2);

        assertEquals(2, canceladas);
        assertEquals(ReservationService.ESTADO_CANCELADA, r1.getId_estado_reserva());
        assertEquals(ReservationService.ESTADO_CANCELADA, r2.getId_estado_reserva());
        verify(repo, times(2)).save(any(Reserva.class));
    }
}

