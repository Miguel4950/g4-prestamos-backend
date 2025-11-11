package co.edu.javeriana.prestamos.service;

import co.edu.javeriana.prestamos.model.Prestamo;
import co.edu.javeriana.prestamos.repository.PrestamoRepository; // <-- REAL
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importante para la BD

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service // Le dice a Spring que esto es la lógica de negocio
public class LoanService {

    // Constantes de G1 para estados de préstamo
    public static final int ESTADO_SOLICITADO = 1;
    public static final int ESTADO_ACTIVO = 2;
    public static final int ESTADO_DEVUELTO = 3;
    public static final int ESTADO_VENCIDO = 4;

    // Cliente hacia el Catálogo (G3) en lugar de acceder a la BD de libros
    private final CatalogClient catalogClient;
    private final PrestamoRepository prestamoRepository;

    public LoanService(CatalogClient catalogClient, PrestamoRepository prestamoRepository) {
        this.catalogClient = catalogClient;
        this.prestamoRepository = prestamoRepository;
    }


    // Esta es tu LÓGICA DE NEGOCIO (US_8) - AHORA CON BD REAL
    @Transactional // Asegura que si algo falla, se hace rollback
    public Prestamo solicitarPrestamo(Integer usuarioId, Integer libroId) throws Exception {

        // 1. Validar existencia y disponibilidad con el Catálogo (G3)
        boolean reservado = catalogClient.reservarUno(String.valueOf(libroId));
        if (!reservado) {
            throw new Exception("Error 400: Libro no disponible o no encontrado en Catálogo");
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
        // Usamos null para que JPA genere el ID (AUTO_INCREMENT)
        Prestamo nuevoPrestamo = new Prestamo(null, usuarioId, libroId, ESTADO_SOLICITADO);
        
        // 6. Guardar el préstamo en la BD REAL
        return prestamoRepository.save(nuevoPrestamo);
    }

    // Esta es tu LÓGICA DE NEGOCIO (US_22) - AHORA CON BD REAL
    public List<Prestamo> getMisPrestamos(Integer usuarioId) {
        // Llama a la consulta real de la BD
        return prestamoRepository.findActivosYVencidosByUsuarioId(usuarioId);
    }

    // -------- NUEVO: Obtener detalle de préstamo --------
    public Optional<Prestamo> getPrestamoById(Integer id) {
        return prestamoRepository.findById(id);
    }

    // -------- NUEVO: Devolver préstamo --------
    @Transactional
    public Prestamo devolverPrestamo(Integer prestamoId, Integer requesterId, boolean isPrivileged) throws Exception {
        Prestamo p = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new Exception("Error 404: Préstamo no encontrado"));

        if (!isPrivileged && !p.getId_usuario().equals(requesterId)) {
            throw new Exception("Error 403: No autorizado");
        }

        if (p.getId_estado_prestamo() == ESTADO_DEVUELTO) {
            throw new Exception("Error 400: El préstamo ya está devuelto");
        }

        // Incrementar disponibilidad en Catálogo antes de cerrar el préstamo
        boolean ok = catalogClient.devolverUno(String.valueOf(p.getId_libro()));
        if (!ok) {
            throw new Exception("Error 500: No fue posible actualizar disponibilidad en Catálogo");
        }

        p.setId_estado_prestamo(ESTADO_DEVUELTO);
        p.setFecha_devolucion_real(LocalDateTime.now());
        return prestamoRepository.save(p);
    }

    // -------- NUEVO: Renovar préstamo (una sola vez, +7 días) --------
    @Transactional
    public Prestamo renovarPrestamo(Integer prestamoId, Integer requesterId) throws Exception {
        Prestamo p = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new Exception("Error 404: Préstamo no encontrado"));

        if (!p.getId_usuario().equals(requesterId)) {
            throw new Exception("Error 403: No autorizado");
        }

        if (p.getId_estado_prestamo() == ESTADO_VENCIDO) {
            throw new Exception("Error 400: No se puede renovar un préstamo vencido");
        }
        if (p.getId_estado_prestamo() == ESTADO_DEVUELTO) {
            throw new Exception("Error 400: El préstamo ya está devuelto");
        }

        // Regla: solo 1 renovación. Detectar si ya se extendió por encima de los 14 días originales.
        LocalDateTime baseDue = p.getFecha_prestamo().plusDays(14);
        boolean yaRenovado = p.getFecha_devolucion_esperada() != null && p.getFecha_devolucion_esperada().isAfter(baseDue);
        if (yaRenovado) {
            throw new Exception("Error 400: El préstamo ya fue renovado una vez");
        }

        // TODO: Verificar reservas pendientes para este libro (requiere módulo de reservas)

        p.setFecha_devolucion_esperada(baseDue.plusDays(7));
        return prestamoRepository.save(p);
    }

    // -------- NUEVO: Aprobar préstamo (SOLICITADO -> ACTIVO) --------
    @Transactional
    public Prestamo aprobarPrestamo(Integer prestamoId) throws Exception {
        Prestamo p = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new Exception("Error 404: Préstamo no encontrado"));
        if (p.getId_estado_prestamo() != ESTADO_SOLICITADO) {
            throw new Exception("Error 400: Solo se pueden aprobar préstamos en estado SOLICITADO");
        }
        p.setId_estado_prestamo(ESTADO_ACTIVO);
        return prestamoRepository.save(p);
    }

    // -------- NUEVO: Listar préstamos (opcional por estado) --------
    public List<Prestamo> listarPrestamos(Integer estado) {
        if (estado == null) {
            return prestamoRepository.findAll();
        }
        return prestamoRepository.findByIdEstadoPrestamo(estado);
    }

    // -------- NUEVO: Listar préstamos vencidos --------
    public List<Prestamo> listarVencidos() {
        return prestamoRepository.findOverdueNow();
    }
}
