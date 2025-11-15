package co.edu.javeriana.prestamos.service;

import co.edu.javeriana.prestamos.exception.BusinessException;
import co.edu.javeriana.prestamos.model.Prestamo;
import co.edu.javeriana.prestamos.repository.ConfiguracionRepository;
import co.edu.javeriana.prestamos.repository.LibroRepository;
import co.edu.javeriana.prestamos.repository.PrestamoRepository;
import co.edu.javeriana.prestamos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LoanServiceRenovarTest {

    @Test
    void renovarPrestamo_activoActualizaFechaEsperada() {
        CatalogClient catalog = mock(CatalogClient.class);
        LibroRepository libroRepo = mock(LibroRepository.class);
        MappingService mapping = mock(MappingService.class);
        PrestamoRepository prestamoRepo = mock(PrestamoRepository.class);
        ReservationService reservationService = mock(ReservationService.class);
        ConfiguracionRepository configuracionRepository = mock(ConfiguracionRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        LocalDateTime now = LocalDateTime.now();
        Prestamo prestamo = new Prestamo(10, 7, 96, LoanService.ESTADO_ACTIVO, now.plusDays(14));
        prestamo.setFecha_prestamo(now);

        when(prestamoRepo.findById(10)).thenReturn(Optional.of(prestamo));
        when(configuracionRepository.findByClave(anyString())).thenReturn(Optional.empty());
        when(prestamoRepo.save(prestamo)).thenReturn(prestamo);

        LoanService service = new LoanService(catalog, libroRepo, mapping, prestamoRepo, reservationService, configuracionRepository, usuarioRepository);
        Prestamo actualizado = service.renovarPrestamo(10, 7, false);

        assertEquals(now.plusDays(21), actualizado.getFecha_devolucion_esperada());
        verify(prestamoRepo, times(1)).save(prestamo);
    }

    @Test
    void renovarPrestamo_fallaSiYaSeRenovo() {
        CatalogClient catalog = mock(CatalogClient.class);
        LibroRepository libroRepo = mock(LibroRepository.class);
        MappingService mapping = mock(MappingService.class);
        PrestamoRepository prestamoRepo = mock(PrestamoRepository.class);
        ReservationService reservationService = mock(ReservationService.class);
        ConfiguracionRepository configuracionRepository = mock(ConfiguracionRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        LocalDateTime now = LocalDateTime.now();
        Prestamo prestamo = new Prestamo(11, 8, 97, LoanService.ESTADO_ACTIVO, now.plusDays(21));
        prestamo.setFecha_prestamo(now);

        when(prestamoRepo.findById(11)).thenReturn(Optional.of(prestamo));
        when(configuracionRepository.findByClave(anyString())).thenReturn(Optional.empty());

        LoanService service = new LoanService(catalog, libroRepo, mapping, prestamoRepo, reservationService, configuracionRepository, usuarioRepository);
        assertThrows(BusinessException.class, () -> service.renovarPrestamo(11, 8, false));
        verify(prestamoRepo, never()).save(any());
    }

    @Test
    void renovarPrestamo_fallaSiNoEstaActivo() {
        CatalogClient catalog = mock(CatalogClient.class);
        LibroRepository libroRepo = mock(LibroRepository.class);
        MappingService mapping = mock(MappingService.class);
        PrestamoRepository prestamoRepo = mock(PrestamoRepository.class);
        ReservationService reservationService = mock(ReservationService.class);
        ConfiguracionRepository configuracionRepository = mock(ConfiguracionRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        Prestamo prestamo = new Prestamo(12, 9, 98, LoanService.ESTADO_SOLICITADO, LocalDateTime.now().plusDays(14));
        when(prestamoRepo.findById(12)).thenReturn(Optional.of(prestamo));

        LoanService service = new LoanService(catalog, libroRepo, mapping, prestamoRepo, reservationService, configuracionRepository, usuarioRepository);
        assertThrows(BusinessException.class, () -> service.renovarPrestamo(12, 9, false));
        verify(prestamoRepo, never()).save(any());
    }
}
