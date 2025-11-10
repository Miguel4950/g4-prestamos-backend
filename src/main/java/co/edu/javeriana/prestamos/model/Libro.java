package co.edu.javeriana.prestamos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "libro") // El nombre exacto de la tabla de G1
public class Libro {
    @Id
    private Integer id_libro;
    private String titulo;
    private String autor;
    private int cantidad_total;
    private int cantidad_disponible;

    // Constructor simulado (para pruebas)
    public Libro(Integer id, String titulo, String autor, int disponible) {
        this.id_libro = id;
        this.titulo = titulo;
        this.autor = autor;
        this.cantidad_total = disponible;
        this.cantidad_disponible = disponible;
    }
}