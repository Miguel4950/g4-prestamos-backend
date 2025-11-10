package co.edu.javeriana.prestamos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "prestamo") // El nombre exacto de la tabla de G1
public class Prestamo {
    @Id
    private Integer id_prestamo;
    private Integer id_usuario;
    private Integer id_libro;
    private LocalDateTime fecha_prestamo;
    private LocalDateTime fecha_devolucion_esperada;
    private LocalDateTime fecha_devolucion_real;
    private Integer id_estado_prestamo;

    // Constructor para un préstamo nuevo
    public Prestamo(Integer id_prestamo, Integer id_usuario, Integer id_libro, Integer id_estado_prestamo) {
        this.id_prestamo = id_prestamo;
        this.id_usuario = id_usuario;
        this.id_libro = id_libro;
        this.id_estado_prestamo = id_estado_prestamo;
        this.fecha_prestamo = LocalDateTime.now();
        this.fecha_devolucion_esperada = LocalDateTime.now().plusDays(14); // Regla de 14 días
        this.fecha_devolucion_real = null;
    }
}