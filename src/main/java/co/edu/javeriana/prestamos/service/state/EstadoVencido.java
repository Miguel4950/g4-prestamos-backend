package co.edu.javeriana.prestamos.service.state;

import co.edu.javeriana.prestamos.model.Prestamo;

import java.time.LocalDateTime;

public class EstadoVencido implements EstadoPrestamo {
    public static final int CODE = 4;

    @Override
    public Prestamo aprobar(Prestamo p) throws Exception {
        throw new Exception("No se puede aprobar un préstamo VENCIDO");
    }

    @Override
    public Prestamo devolver(Prestamo p) {
        p.setId_estado_prestamo(EstadoDevuelto.CODE);
        p.setFecha_devolucion_real(LocalDateTime.now());
        return p;
    }

    @Override
    public Prestamo renovar(Prestamo p) throws Exception {
        throw new Exception("No se puede renovar un préstamo VENCIDO");
    }

    @Override
    public int code() { return CODE; }
}

