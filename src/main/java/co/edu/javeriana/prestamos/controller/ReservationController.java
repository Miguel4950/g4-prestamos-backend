package co.edu.javeriana.prestamos.controller;

import co.edu.javeriana.prestamos.exception.BusinessException;
import co.edu.javeriana.prestamos.model.Reserva;
import co.edu.javeriana.prestamos.security.CustomUserDetails;
import co.edu.javeriana.prestamos.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ReservationRequest request) {
        try {
            CustomUserDetails userDetails = currentUser();
            Reserva r = reservationService.createReservation(userDetails.getUserId(), request.getLibro_id());
            return ResponseEntity.ok(new ReservationResponse(r));
        } catch (BusinessException e) {
            HttpStatus status = resolveStatus(e.getMessage(), HttpStatus.BAD_REQUEST);
            return ResponseEntity.status(status).body(e.getMessage());
        }
    }

    @GetMapping("/my-reservations")
    public ResponseEntity<?> mine() {
        CustomUserDetails userDetails = currentUser();
        List<Reserva> list = reservationService.getMyReservations(userDetails.getUserId());
        return ResponseEntity.ok(list.stream().map(ReservationResponse::new).collect(Collectors.toList()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(@PathVariable("id") Integer id) {
        try {
            CustomUserDetails userDetails = currentUser();
            reservationService.cancelReservation(id, userDetails.getUserId(), isPrivileged(userDetails));
            return ResponseEntity.noContent().build();
        } catch (BusinessException e) {
            HttpStatus status = resolveStatus(e.getMessage(), HttpStatus.BAD_REQUEST);
            return ResponseEntity.status(status).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(name = "estado", required = false) Integer estado) {
        CustomUserDetails userDetails = currentUser();
        if (!isPrivileged(userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
        }
        List<Reserva> list = reservationService.listAll(estado);
        return ResponseEntity.ok(list.stream().map(ReservationResponse::new).collect(Collectors.toList()));
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

