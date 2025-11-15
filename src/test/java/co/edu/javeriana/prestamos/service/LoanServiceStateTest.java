package co.edu.javeriana.prestamos.service;

import co.edu.javeriana.prestamos.model.Prestamo;
import co.edu.javeriana.prestamos.repository.LibroRepository;
import co.edu.javeriana.prestamos.repository.PrestamoRepository;
import co.edu.javeriana.prestamos.repository.ConfiguracionRepository;
import co.edu.javeriana.prestamos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class LoanServiceStateTest {
    @Test
    void aprobar_transicionaDeSolicitadoAActivo() {
        CatalogClient catalog = mock(CatalogClient.class);
        LibroRepository libroRepo = mock(LibroRepository.class);
        MappingService mapping = mock(MappingService.class);
        PrestamoRepository prestamoRepo = mock(PrestamoRepository.class);
        ReservationService reservationService = mock(ReservationService.class);
        ConfiguracionRepository configuracionRepository = mock(ConfiguracionRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        Prestamo p = new Prestamo(10, 7, 96, LoanService.ESTADO_SOLICITADO, LocalDateTime.now().plusDays(7));
        when(prestamoRepo.findById(10)).thenReturn(Optional.of(p));
        ArgumentCaptor<Prestamo> cap = ArgumentCaptor.forClass(Prestamo.class);
        when(prestamoRepo.save(cap.capture())).thenAnswer(a -> cap.getValue());
        when(configuracionRepository.findByClave(anyString())).thenReturn(Optional.empty());

        LoanService service = new LoanService(catalog, libroRepo, mapping, prestamoRepo, reservationService, configuracionRepository, usuarioRepository);
        Prestamo actualizado = service.aprobarPrestamo(10);
        assertEquals(LoanService.ESTADO_ACTIVO, actualizado.getId_estado_prestamo());
    }
}
