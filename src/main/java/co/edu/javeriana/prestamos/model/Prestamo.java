package co.edu.javeriana.prestamos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "prestamo")
public class Prestamo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_prestamo;
    private Integer id_usuario;
    private Integer id_libro;
    private LocalDateTime fecha_prestamo;
    private LocalDateTime fecha_devolucion_esperada;
    private LocalDateTime fecha_devolucion_real;
    private Integer id_estado_prestamo;

    public Prestamo(Integer id_prestamo, Integer id_usuario, Integer id_libro,
                    Integer id_estado_prestamo, LocalDateTime fecha_devolucion_esperada) {
        this.id_prestamo = id_prestamo;
        this.id_usuario = id_usuario;
        this.id_libro = id_libro;
        this.id_estado_prestamo = id_estado_prestamo;
        this.fecha_prestamo = LocalDateTime.now();
        this.fecha_devolucion_esperada = fecha_devolucion_esperada;
        this.fecha_devolucion_real = null;
    }
}

