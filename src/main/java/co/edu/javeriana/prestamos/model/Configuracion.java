package co.edu.javeriana.prestamos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "configuracion")
public class Configuracion {
    @Id
    @Column(name = "clave")
    private String clave;

    @Column(name = "valor")
    private String valor;
}
