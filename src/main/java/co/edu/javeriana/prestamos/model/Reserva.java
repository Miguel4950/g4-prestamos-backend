package co.edu.javeriana.prestamos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "reserva")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_reserva;
    private Integer id_usuario;
    private Integer id_libro;
    private LocalDateTime fecha_reserva;
    private Integer id_estado_reserva; // 1 = PENDIENTE, 2 = NOTIFICADO, 3 = CANCELADA, 4 = EXPIRADA

    public Reserva(Integer id, Integer idUsuario, Integer idLibro, Integer estado) {
        this.id_reserva = id;
        this.id_usuario = idUsuario;
        this.id_libro = idLibro;
        this.id_estado_reserva = estado;
        this.fecha_reserva = LocalDateTime.now();
    }
}
