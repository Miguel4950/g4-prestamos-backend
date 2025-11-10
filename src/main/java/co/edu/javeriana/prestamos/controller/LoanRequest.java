package co.edu.javeriana.prestamos.controller;

import lombok.Data;

@Data // Define el JSON: { "libro_id": 1 }
public class LoanRequest {
    private Integer libro_id; // Nombre exacto del contrato
}