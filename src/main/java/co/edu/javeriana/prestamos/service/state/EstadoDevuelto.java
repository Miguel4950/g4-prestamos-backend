package co.edu.javeriana.prestamos.service.state;

import co.edu.javeriana.prestamos.model.Prestamo;

public class EstadoDevuelto implements EstadoPrestamo {
    public static final int CODE = 3;

    @Override
    public Prestamo aprobar(Prestamo p) throws Exception {
        throw new Exception("No se puede aprobar un préstamo DEVUELTO");
    }

    @Override
    public Prestamo devolver(Prestamo p) throws Exception {
        throw new Exception("El préstamo ya está DEVUELTO");
    }

    @Override
    public Prestamo renovar(Prestamo p) throws Exception {
        throw new Exception("No se puede renovar un préstamo DEVUELTO");
    }

    @Override
    public int code() { return CODE; }
}

