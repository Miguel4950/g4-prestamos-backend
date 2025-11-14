package co.edu.javeriana.prestamos.service;

import co.edu.javeriana.prestamos.model.Reserva;
import co.edu.javeriana.prestamos.notification.NotificationService;
import co.edu.javeriana.prestamos.repository.ConfiguracionRepository;
import co.edu.javeriana.prestamos.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationServiceTest {

    @Test
    void createReservation_whenNoAvailability_createsPending() throws Exception {
        ReservaRepository repo = mock(ReservaRepository.class);
        MappingService mapping = mock(MappingService.class);
        CatalogClient client = mock(CatalogClient.class);
        NotificationService notif = mock(NotificationService.class);
        ConfiguracionRepository configuracionRepository = mock(ConfiguracionRepository.class);

        when(mapping.mapToDbId(123)).thenReturn(96);
        CatalogClient.BookDto book = new CatalogClient.BookDto();
        book.cantidadDisponible = 0;
        when(client.getBook("123")).thenReturn(book);
        when(repo.existsActiveOrNotified(7, 96, List.of(1,2))).thenReturn(false);

        ArgumentCaptor<Reserva> cap = ArgumentCaptor.forClass(Reserva.class);
        when(repo.save(cap.capture())).thenAnswer(a -> {
            Reserva r = cap.getValue();
            r.setId_reserva(1);
            return r;
        });

        when(configuracionRepository.findByClave(anyString())).thenReturn(java.util.Optional.empty());

        ReservationService service = new ReservationService(repo, mapping, client, notif, configuracionRepository);
        Reserva r = service.createReservation(7, 123);

        assertNotNull(r.getId_reserva());
        assertEquals(7, r.getId_usuario());
        assertEquals(96, r.getId_libro());
        assertEquals(1, r.getId_estado_reserva());
    }
}
