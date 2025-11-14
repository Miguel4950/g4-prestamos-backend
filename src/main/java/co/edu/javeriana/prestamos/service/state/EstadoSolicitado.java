package co.edu.javeriana.prestamos.service.state;

import co.edu.javeriana.prestamos.model.Prestamo;

import java.time.LocalDateTime;

public class EstadoSolicitado implements EstadoPrestamo {
    public static final int CODE = 1;

    @Override
    public Prestamo aprobar(Prestamo p) {
        p.setId_estado_prestamo(EstadoActivo.CODE);
        // fecha_prestamo ya está, mantener
        return p;
    }

    @Override
    public Prestamo devolver(Prestamo p) throws Exception {
        throw new Exception("Solo se puede devolver un préstamo ACTIVO/VENCIDO");
    }

    @Override
    public Prestamo renovar(Prestamo p) throws Exception {
        throw new Exception("No se puede renovar un préstamo SOLICITADO");
    }

    @Override
    public int code() { return CODE; }
}

