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
}