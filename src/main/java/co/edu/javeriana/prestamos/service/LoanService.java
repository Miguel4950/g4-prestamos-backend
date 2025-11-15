package co.edu.javeriana.prestamos.service;

import co.edu.javeriana.prestamos.exception.BusinessException;
import co.edu.javeriana.prestamos.model.Prestamo;
import co.edu.javeriana.prestamos.model.Usuario;
import co.edu.javeriana.prestamos.repository.ConfiguracionRepository;
import co.edu.javeriana.prestamos.repository.LibroRepository;
import co.edu.javeriana.prestamos.repository.PrestamoRepository;
import co.edu.javeriana.prestamos.repository.UsuarioRepository;
import co.edu.javeriana.prestamos.repository.UsuarioRepository;
import co.edu.javeriana.prestamos.service.state.EstadoPrestamo;
import co.edu.javeriana.prestamos.service.state.EstadoPrestamoFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoanService {

    public static final int ESTADO_SOLICITADO = 1;
    public static final int ESTADO_ACTIVO = 2;
    public static final int ESTADO_DEVUELTO = 3;
    public static final int ESTADO_VENCIDO = 4;

    private static final String CFG_MAX_PRESTAMOS = "max_prestamos_simultaneos";
    private static final String CFG_PERIODO_PRESTAMO = "periodo_prestamo_dias";
    private static final String CFG_DIAS_RENOVACION = "dias_renovacion";

    private final CatalogClient catalogClient;
    private final LibroRepository libroRepository;
    private final MappingService mappingService;
    private final PrestamoRepository prestamoRepository;
    private final ReservationService reservationService;
    private final ConfiguracionRepository configuracionRepository;
    private final UsuarioRepository usuarioRepository;
    private final Map<String, Integer> configuracionCache = new ConcurrentHashMap<>();

    public LoanService(CatalogClient catalogClient,
                       LibroRepository libroRepository,
                       MappingService mappingService,
                       PrestamoRepository prestamoRepository,
                       ReservationService reservationService,
                       ConfiguracionRepository configuracionRepository,
                       UsuarioRepository usuarioRepository) {
        this.catalogClient = catalogClient;
        this.libroRepository = libroRepository;
        this.mappingService = mappingService;
        this.prestamoRepository = prestamoRepository;
        this.reservationService = reservationService;
        this.configuracionRepository = configuracionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Prestamo solicitarPrestamo(Integer usuarioId, Integer libroId) {
        Integer dbLibroId = mappingService.mapToDbId(libroId);
        if (dbLibroId == null) {
            dbLibroId = libroId;
        }
        if (!libroRepository.existsById(dbLibroId)) {
            throw new BusinessException("Error 400: El id_libro=" + dbLibroId + " no existe en la BD. Configure el mapeo BOOK_ID_MAP en G4.");
        }

        markOverdueLoans();

        List<Prestamo> prestamosDelUsuario = prestamoRepository.findActivosYVencidosByUsuarioId(usuarioId);
        boolean tieneVencidos = prestamosDelUsuario.stream()
                .anyMatch(p -> p.getId_estado_prestamo() == ESTADO_VENCIDO);
        if (tieneVencidos) {
            throw new BusinessException("Error 400: El usuario tiene préstamos vencidos.");
        }

        int maxPrestamos = getConfigInt(CFG_MAX_PRESTAMOS, Integer.MAX_VALUE);
        long prestamosNoDevueltos = prestamosDelUsuario.stream()
                .filter(p -> {
                    int estado = p.getId_estado_prestamo();
                    return estado == ESTADO_SOLICITADO || estado == ESTADO_ACTIVO || estado == ESTADO_VENCIDO;
                })
                .count();
        if (prestamosNoDevueltos >= maxPrestamos) {
            throw new BusinessException("Error 400: Límite de " + maxPrestamos + " préstamos alcanzado.");
        }

        boolean reservado = false;
        try {
            reservado = catalogClient.reservarUno(String.valueOf(libroId));
            if (!reservado) {
                throw new BusinessException("Error 400: Libro no disponible o no encontrado en Catálogo.");
            }

            int prestamoDias = getConfigInt(CFG_PERIODO_PRESTAMO, 14);
            LocalDateTime fechaDevolucion = LocalDateTime.now().plusDays(prestamoDias);
            Prestamo nuevoPrestamo = new Prestamo(null, usuarioId, dbLibroId, ESTADO_SOLICITADO, fechaDevolucion);
            return prestamoRepository.save(nuevoPrestamo);
        } catch (RuntimeException e) {
            if (reservado) {
                try {
                    catalogClient.devolverUno(String.valueOf(libroId));
                } catch (Exception ignored) {
                }
            }
            throw e;
        }
    }

    public List<Prestamo> getMisPrestamos(Integer usuarioId) {
        markOverdueLoans();
        return prestamoRepository.findActivosYVencidosByUsuarioId(usuarioId);
    }

    public Optional<Prestamo> getPrestamoById(Integer id) {
        return prestamoRepository.findById(id);
    }

    @Transactional
    public Prestamo devolverPrestamo(Integer prestamoId, Integer requesterId, boolean isPrivileged) {
        Prestamo p = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new BusinessException("Error 404: Préstamo no encontrado."));

        if (!isPrivileged && !p.getId_usuario().equals(requesterId)) {
            throw new BusinessException("Error 403: No autorizado.");
        }

        if (p.getId_estado_prestamo() == ESTADO_DEVUELTO) {
            throw new BusinessException("Error 400: El préstamo ya está devuelto.");
        }

        boolean ok = catalogClient.devolverUno(String.valueOf(p.getId_libro()));
        if (!ok) {
            throw new RuntimeException("Error 500: No fue posible actualizar disponibilidad en Catálogo.");
        }

        try {
            EstadoPrestamo estado = EstadoPrestamoFactory.fromCode(p.getId_estado_prestamo());
            p = estado.devolver(p);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
        p = prestamoRepository.save(p);
        try {
            reservationService.notifyNextInQueue(p.getId_libro());
        } catch (Exception ignored) {
        }
        return p;
    }

    @Transactional
    public Prestamo renovarPrestamo(Integer prestamoId, Integer requesterId, boolean isPrivileged) {
        Prestamo p = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new BusinessException("Error 404: Préstamo no encontrado."));

        if (!isPrivileged && !p.getId_usuario().equals(requesterId)) {
            throw new BusinessException("Error 403: No autorizado.");
        }
        if (p.getId_estado_prestamo() != ESTADO_ACTIVO) {
            throw new BusinessException("Error 400: Solo se pueden renovar préstamos activos.");
        }

        int periodoPrestamoDias = getConfigInt(CFG_PERIODO_PRESTAMO, 14);
        int diasRenovacion = getConfigInt(CFG_DIAS_RENOVACION, 7);
        LocalDateTime baseDue = p.getFecha_prestamo().plusDays(periodoPrestamoDias);
        boolean yaRenovado = p.getFecha_devolucion_esperada() != null && p.getFecha_devolucion_esperada().isAfter(baseDue);
        if (yaRenovado) {
            throw new BusinessException("Error 400: El préstamo ya fue renovado una vez.");
        }
        p.setFecha_devolucion_esperada(baseDue.plusDays(diasRenovacion));
        return prestamoRepository.save(p);
    }

    @Transactional
    public Prestamo aprobarPrestamo(Integer prestamoId) {
        Prestamo p = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new BusinessException("Error 404: Préstamo no encontrado."));
        try {
            EstadoPrestamo estado = EstadoPrestamoFactory.fromCode(p.getId_estado_prestamo());
            p = estado.aprobar(p);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
        return prestamoRepository.save(p);
    }

    public List<Prestamo> listarPrestamos(Integer estado) {
        markOverdueLoans();
        if (estado == null) {
            return prestamoRepository.findAll();
        }
        return prestamoRepository.findByEstado(estado);
    }

    public List<Prestamo> listarVencidos() {
        markOverdueLoans();
        return prestamoRepository.findByEstado(ESTADO_VENCIDO);
    }

    public Map<String, Object> getUsuarioResumen(Integer idUsuario) {
        if (idUsuario == null) return Map.of();
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) return Map.of();
        return Map.of(
                "id", usuario.getId_usuario(),
                "nombre", usuario.getNombre(),
                "username", usuario.getUsername(),
                "tipo", usuario.getId_tipo_usuario()
        );
    }

    private void markOverdueLoans() {
        List<Prestamo> overdue = prestamoRepository.findOverdueNow();
        if (overdue.isEmpty()) {
            return;
        }
        overdue.forEach(p -> p.setId_estado_prestamo(ESTADO_VENCIDO));
        prestamoRepository.saveAll(overdue);
    }

    private int getConfigInt(String key, int fallback) {
        return configuracionCache.computeIfAbsent(key, k ->
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
