package co.edu.javeriana.prestamos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: crea getters, setters, etc.
@NoArgsConstructor // Lombok: crea constructor vacío
@Entity // Le dice a JPA que esto es una tabla
@Table(name = "usuario") // El nombre exacto de la tabla de G1
public class Usuario {
    @Id
    private Integer id_usuario;
    private String username;
    private String nombre;
    private String contrasena;
    private Integer id_tipo_usuario;
    private Integer id_estado_usuario;
    private int intentos_fallidos;

    // Constructor simulado (para pruebas)
    public Usuario(Integer id, String username, String nombre, int tipo) {
        this.id_usuario = id;
        this.username = username;
        this.nombre = nombre;
        this.id_tipo_usuario = tipo;
        this.id_estado_usuario = 1; // 1 = "activo"
        this.intentos_fallidos = 0;
    }
}