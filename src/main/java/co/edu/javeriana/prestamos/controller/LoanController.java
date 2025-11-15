package co.edu.javeriana.prestamos.controller;

import co.edu.javeriana.prestamos.exception.BusinessException;
import co.edu.javeriana.prestamos.model.Prestamo;
import co.edu.javeriana.prestamos.security.CustomUserDetails;
import co.edu.javeriana.prestamos.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    @PostMapping("/loans")
    public ResponseEntity<?> solicitarPrestamo(@RequestBody LoanRequest request) {
        try {
            CustomUserDetails userDetails = currentUser();
            Prestamo nuevoPrestamo = loanService.solicitarPrestamo(userDetails.getUserId(), request.getLibro_id());
            return ResponseEntity.ok(new LoanResponse(nuevoPrestamo));
        } catch (BusinessException e) {
            return ResponseEntity.status(resolveStatus(e.getMessage(), HttpStatus.BAD_REQUEST)).body(e.getMessage());
        }
    }

    @GetMapping("/loans/my-loans")
    public ResponseEntity<?> getMisPrestamos() {
        CustomUserDetails userDetails = currentUser();
        List<Prestamo> prestamos = loanService.getMisPrestamos(userDetails.getUserId());
        return ResponseEntity.ok(new MyLoansResponse(prestamos));
    }

    @GetMapping("/loans/{id}")
    public ResponseEntity<?> getPrestamo(@PathVariable("id") Integer id) {
        try {
            CustomUserDetails userDetails = currentUser();
            boolean isPrivileged = isPrivileged(userDetails);
            Prestamo p = loanService.getPrestamoById(id)
                    .orElseThrow(() -> new BusinessException("Error 404: Préstamo no encontrado"));
            if (!isPrivileged && !p.getId_usuario().equals(userDetails.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
            }
            return ResponseEntity.ok(new LoanResponse(p));
        } catch (BusinessException e) {
            HttpStatus status = resolveStatus(e.getMessage(), HttpStatus.NOT_FOUND);
            return ResponseEntity.status(status).body(e.getMessage());
        }
    }

    @PutMapping("/loans/{id}/return")
    public ResponseEntity<?> devolverPrestamo(@PathVariable("id") Integer id) {
        try {
            CustomUserDetails userDetails = currentUser();
            Prestamo actualizado = loanService.devolverPrestamo(
                    id, userDetails.getUserId(), isPrivileged(userDetails));
            return ResponseEntity.ok(new LoanResponse(actualizado));
        } catch (BusinessException e) {
            HttpStatus status = resolveStatus(e.getMessage(), HttpStatus.BAD_REQUEST);
            return ResponseEntity.status(status).body(e.getMessage());
        }
    }

    @PutMapping("/loans/{id}/renew")
    public ResponseEntity<?> renovarPrestamo(@PathVariable("id") Integer id) {
        try {
            CustomUserDetails userDetails = currentUser();
            Prestamo actualizado = loanService.renovarPrestamo(
                    id, userDetails.getUserId(), isPrivileged(userDetails));
            return ResponseEntity.ok(new LoanResponse(actualizado));
        } catch (BusinessException e) {
            HttpStatus status = resolveStatus(e.getMessage(), HttpStatus.BAD_REQUEST);
            return ResponseEntity.status(status).body(e.getMessage());
        }
    }

    @PutMapping("/loans/{id}/approve")
    public ResponseEntity<?> aprobarPrestamo(@PathVariable("id") Integer id) {
        try {
            CustomUserDetails userDetails = currentUser();
            if (!isPrivileged(userDetails)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
            }
            Prestamo actualizado = loanService.aprobarPrestamo(id);
            return ResponseEntity.ok(new LoanResponse(actualizado));
        } catch (BusinessException e) {
            HttpStatus status = resolveStatus(e.getMessage(), HttpStatus.BAD_REQUEST);
            return ResponseEntity.status(status).body(e.getMessage());
        }
    }

    @GetMapping("/loans")
    public ResponseEntity<?> listarPrestamos(@RequestParam(name = "estado", required = false) Integer estado) {
        CustomUserDetails userDetails = currentUser();
        if (!isPrivileged(userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
        }
        List<Prestamo> prestamos = loanService.listarPrestamos(estado);
        List<Map<String, Object>> body = prestamos.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("prestamo", new LoanResponse(p));
            map.put("usuario", loanService.getUsuarioResumen(p.getId_usuario()));
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/loans/overdue")
    public ResponseEntity<?> listarVencidos() {
        CustomUserDetails userDetails = currentUser();
        if (!isPrivileged(userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
        }
        List<Prestamo> vencidos = loanService.listarVencidos();
        return ResponseEntity.ok(vencidos.stream().map(LoanResponse::new).collect(Collectors.toList()));
    }

    private CustomUserDetails currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) authentication.getPrincipal();
    }

    private boolean isPrivileged(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_BIBLIOTECARIO") || a.getAuthority().equals("ROLE_ADMIN"));
    }

    private HttpStatus resolveStatus(String message, HttpStatus defaultStatus) {
        if (message == null) {
            return defaultStatus;
        }
        if (message.startsWith("Error 404")) return HttpStatus.NOT_FOUND;
        if (message.startsWith("Error 403")) return HttpStatus.FORBIDDEN;
        if (message.startsWith("Error 409")) return HttpStatus.CONFLICT;
        return defaultStatus;
    }
}
