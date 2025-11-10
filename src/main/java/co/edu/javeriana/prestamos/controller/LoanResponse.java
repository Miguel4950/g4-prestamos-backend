package co.edu.javeriana.prestamos.controller;

import co.edu.javeriana.prestamos.model.Prestamo;
import lombok.Data;

@Data // Define el JSON de respuesta
public class LoanResponse {
    private Integer id_prestamo;
    private Integer id_libro;
    private Integer id_usuario;
    private String estado; // TRADUCIDO A STRING

    // Constructor para traducir el INT a String
    public LoanResponse(Prestamo p) {
        this.id_prestamo = p.getId_prestamo();
        this.id_libro = p.getId_libro();
        this.id_usuario = p.getId_usuario();

        switch (p.getId_estado_prestamo()) {
            case 1:
                this.estado = "SOLICITADO";
                break;
            case 2:
                this.estado = "ACTIVO";
                break;
            case 3:
                this.estado = "DEVUELTO";
                break;
            case 4:
                this.estado = "VENCIDO";
                break;
            default:
                this.estado = "DESCONOCIDO";
                break;
        }
    }
}