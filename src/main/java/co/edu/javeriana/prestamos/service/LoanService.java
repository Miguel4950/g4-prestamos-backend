package co.edu.javeriana.prestamos.service;

import co.edu.javeriana.prestamos.model.Libro;
import co.edu.javeriana.prestamos.model.Prestamo;
import co.edu.javeriana.prestamos.repository.LibroRepository; // <-- REAL
import co.edu.javeriana.prestamos.repository.PrestamoRepository; // <-- REAL
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importante para la BD

import java.util.List;
import java.util.Optional;

@Service // Le dice a Spring que esto es la lógica de negocio
public class LoanService {

    // Constantes de G1 para estados de préstamo
    public static final int ESTADO_SOLICITADO = 1;
    public static final int ESTADO_ACTIVO = 2;
    public static final int ESTADO_DEVUELTO = 3;
    public static final int ESTADO_VENCIDO = 4;

    // --- INICIO DE INTEGRACIÓN G1 ---
    // Inyectamos los repositorios REALES
    private final LibroRepository libroRepository;
    private final PrestamoRepository prestamoRepository;

    public LoanService(LibroRepository libroRepository, PrestamoRepository prestamoRepository) {
        this.libroRepository = libroRepository;
        this.prestamoRepository = prestamoRepository;
    }
    // --- FIN DE INTEGRACIÓN G1 ---


    // Esta es tu LÓGICA DE NEGOCIO (US_8) - AHORA CON BD REAL
    @Transactional // Asegura que si algo falla, se hace rollback
    public Prestamo solicitarPrestamo(Integer usuarioId, Integer libroId) throws Exception {

        // 1. Validar que el libro exista y esté disponible
        Optional<Libro> libroOpt = libroRepository.findById(libroId);
        if (libroOpt.isEmpty()) {
            throw new Exception("Error 404: Libro no encontrado");
        }
        Libro libro = libroOpt.get();
        if (libro.getCantidad_disponible() <= 0) {
            throw new Exception("Error 400: Libro no disponible");
        }

        // 2. Validar que el usuario pueda pedir prestado (con la consulta real)
        List<Prestamo> prestamosDelUsuario = prestamoRepository.findActivosYVencidosByUsuarioId(usuarioId);

        // 3. Regla US_22 (Must Have): Validar préstamos vencidos
        boolean tieneVencidos = prestamosDelUsuario.stream()
                .anyMatch(p -> p.getId_estado_prestamo() == ESTADO_VENCIDO);
        if (tieneVencidos) {
            throw new Exception("Error 400: El usuario tiene préstamos vencidos");
        }
        
        // 4. Regla US_8: Límite de 3 préstamos
        long prestamosActivos = prestamosDelUsuario.stream()
                .filter(p -> p.getId_estado_prestamo() == ESTADO_ACTIVO)
                .count();
        if (prestamosActivos >= 3) {
            throw new Exception("Error 400: Límite de 3 préstamos alcanzado");
        }

        // 5. ¡Todo en orden! Crear el préstamo
        // (Ponemos 0 como ID, la BD (AUTO_INCREMENT) lo asignará)
        Prestamo nuevoPrestamo = new Prestamo(0, usuarioId, libroId, ESTADO_SOLICITADO);
        
        // 6. Actualizar la BD REAL
        libro.setCantidad_disponible(libro.getCantidad_disponible() - 1);
        libroRepository.save(libro); // Actualiza el libro
        
        return prestamoRepository.save(nuevoPrestamo); // Guarda el préstamo
    }

    // Esta es tu LÓGICA DE NEGOCIO (US_22) - AHORA CON BD REAL
    public List<Prestamo> getMisPrestamos(Integer usuarioId) {
        // Llama a la consulta real de la BD
        return prestamoRepository.findActivosYVencidosByUsuarioId(usuarioId);
    }
}