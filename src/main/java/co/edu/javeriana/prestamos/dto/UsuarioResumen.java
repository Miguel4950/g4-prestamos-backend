package co.edu.javeriana.prestamos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UsuarioResumen {
    private Integer id;
    private String nombre;
    private String username;
    private Integer tipo;
}
