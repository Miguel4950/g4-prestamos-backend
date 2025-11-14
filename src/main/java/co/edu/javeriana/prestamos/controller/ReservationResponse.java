package co.edu.javeriana.prestamos.controller;

import co.edu.javeriana.prestamos.model.Reserva;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationResponse {
    private Integer id_reserva;
    private Integer id_usuario;
    private Integer id_libro;
    private LocalDateTime fecha_reserva;
    private Integer id_estado_reserva;

    public ReservationResponse(Reserva r) {
        this.id_reserva = r.getId_reserva();
        this.id_usuario = r.getId_usuario();
        this.id_libro = r.getId_libro();
        this.fecha_reserva = r.getFecha_reserva();
        this.id_estado_reserva = r.getId_estado_reserva();
    }
}

