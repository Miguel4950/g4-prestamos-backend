package co.edu.javeriana.prestamos.service.state;

import co.edu.javeriana.prestamos.model.Prestamo;

public interface EstadoPrestamo {
    Prestamo aprobar(Prestamo p) throws Exception;
    Prestamo devolver(Prestamo p) throws Exception;
    Prestamo renovar(Prestamo p) throws Exception;

    int code();
}

