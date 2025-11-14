package co.edu.javeriana.prestamos.service.state;

import co.edu.javeriana.prestamos.model.Prestamo;

import java.time.LocalDateTime;

public class EstadoActivo implements EstadoPrestamo {
    public static final int CODE = 2;

    @Override
    public Prestamo aprobar(Prestamo p) throws Exception {
        throw new Exception("El préstamo ya está ACTIVO");
    }

    @Override
    public Prestamo devolver(Prestamo p) {
        p.setId_estado_prestamo(EstadoDevuelto.CODE);
        p.setFecha_devolucion_real(LocalDateTime.now());
        return p;
    }

    @Override
    public Prestamo renovar(Prestamo p) throws Exception {
        LocalDateTime baseDue = p.getFecha_prestamo().plusDays(14);
        boolean yaRenovado = p.getFecha_devolucion_esperada() != null && p.getFecha_devolucion_esperada().isAfter(baseDue);
        if (yaRenovado) {
            throw new Exception("El préstamo ya fue renovado una vez");
        }
        p.setFecha_devolucion_esperada(baseDue.plusDays(7));
        return p;
    }

    @Override
    public int code() { return CODE; }
}

