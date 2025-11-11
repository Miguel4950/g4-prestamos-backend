package co.edu.javeriana.prestamos.controller;

import co.edu.javeriana.prestamos.model.Prestamo;
import co.edu.javeriana.prestamos.service.LoanService;
import co.edu.javeriana.prestamos.security.CustomUserDetails; // Importa la clase de G2
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Importa de Spring Security
import org.springframework.security.core.context.SecurityContextHolder; // Importa de Spring Security
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController 
@RequestMapping("/api") 
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // ************ SIMULACIÓN BORRADA ************
    // El método getUsuarioSimuladoId() se ha eliminado.

    /**
     * API 1: Solicitar Préstamo (US_8) - INTEGRADA
     */
    @PostMapping("/loans")
    public ResponseEntity<?> solicitarPrestamo(@RequestBody LoanRequest request) {
        try {
            // --- INICIO DE INTEGRACIÓN G2 ---
            // Obtiene el usuario real desde el token JWT
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer usuarioId = userDetails.getUserId();
            // --- FIN DE INTEGRACIÓN G2 ---

            Prestamo nuevoPrestamo = loanService.solicitarPrestamo(usuarioId, request.getLibro_id());

            return ResponseEntity.ok(new LoanResponse(nuevoPrestamo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * API 2: Ver Mis Préstamos (US_22) - INTEGRADA
     */
    @GetMapping("/loans/my-loans")
    public ResponseEntity<?> getMisPrestamos() {
        try {
            // --- INICIO DE INTEGRACIÓN G2 ---
            // Obtiene el usuario real desde el token JWT
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer usuarioId = userDetails.getUserId();
            // --- FIN DE INTEGRACIÓN G2 ---
            
            List<Prestamo> prestamos = loanService.getMisPrestamos(usuarioId);

            return ResponseEntity.ok(new MyLoansResponse(prestamos));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // -------- NUEVOS ENDPOINTS --------

    @GetMapping("/loans/{id}")
    public ResponseEntity<?> getPrestamo(@PathVariable("id") Integer id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer requesterId = userDetails.getUserId();
            boolean isPrivileged = userDetails.getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().equals("ROLE_BIBLIOTECARIO") || a.getAuthority().equals("ROLE_ADMIN"));

            Prestamo p = loanService.getPrestamoById(id)
                    .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));
            if (!isPrivileged && !p.getId_usuario().equals(requesterId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
            }
            return ResponseEntity.ok(new LoanResponse(p));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener préstamo");
        }
    }

    @PutMapping("/loans/{id}/return")
    public ResponseEntity<?> devolverPrestamo(@PathVariable("id") Integer id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer requesterId = userDetails.getUserId();
            boolean isPrivileged = userDetails.getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().equals("ROLE_BIBLIOTECARIO") || a.getAuthority().equals("ROLE_ADMIN"));

            Prestamo actualizado = loanService.devolverPrestamo(id, requesterId, isPrivileged);
            return ResponseEntity.ok(new LoanResponse(actualizado));
        } catch (Exception e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().startsWith("Error 404") ? HttpStatus.NOT_FOUND
                    : (e.getMessage() != null && (e.getMessage().startsWith("Error 403") || e.getMessage().contains("No autorizado"))) ? HttpStatus.FORBIDDEN
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(e.getMessage());
        }
    }

    @PutMapping("/loans/{id}/renew")
    public ResponseEntity<?> renovarPrestamo(@PathVariable("id") Integer id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer requesterId = userDetails.getUserId();

            Prestamo actualizado = loanService.renovarPrestamo(id, requesterId);
            return ResponseEntity.ok(new LoanResponse(actualizado));
        } catch (Exception e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().startsWith("Error 404") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(e.getMessage());
        }
    }

    @PutMapping("/loans/{id}/approve")
    public ResponseEntity<?> aprobarPrestamo(@PathVariable("id") Integer id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            boolean isPrivileged = userDetails.getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().equals("ROLE_BIBLIOTECARIO") || a.getAuthority().equals("ROLE_ADMIN"));
            if (!isPrivileged) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
            }

            Prestamo actualizado = loanService.aprobarPrestamo(id);
            return ResponseEntity.ok(new LoanResponse(actualizado));
        } catch (Exception e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().startsWith("Error 404") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(e.getMessage());
        }
    }

    @GetMapping("/loans")
    public ResponseEntity<?> listarPrestamos(@RequestParam(name = "estado", required = false) Integer estado) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            boolean isPrivileged = userDetails.getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().equals("ROLE_BIBLIOTECARIO") || a.getAuthority().equals("ROLE_ADMIN"));
            if (!isPrivileged) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
            }
            List<Prestamo> prestamos = loanService.listarPrestamos(estado);
            return ResponseEntity.ok(prestamos.stream().map(LoanResponse::new).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al listar préstamos");
        }
    }

    @GetMapping("/loans/overdue")
    public ResponseEntity<?> listarVencidos() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            boolean isPrivileged = userDetails.getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().equals("ROLE_BIBLIOTECARIO") || a.getAuthority().equals("ROLE_ADMIN"));
            if (!isPrivileged) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
            }
            List<Prestamo> vencidos = loanService.listarVencidos();
            return ResponseEntity.ok(vencidos.stream().map(LoanResponse::new).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al listar vencidos");
        }
    }
}
