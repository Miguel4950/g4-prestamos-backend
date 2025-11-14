package co.edu.javeriana.prestamos.service.state;

public class EstadoPrestamoFactory {
    public static EstadoPrestamo fromCode(int code) {
        return switch (code) {
            case 1 -> new EstadoSolicitado();
            case 2 -> new EstadoActivo();
            case 3 -> new EstadoDevuelto();
            case 4 -> new EstadoVencido();
            default -> new EstadoSolicitado();
        };
    }
}

