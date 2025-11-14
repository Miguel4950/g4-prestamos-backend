package co.edu.javeriana.prestamos.exception;

/**
 * Business-specific exception used to signal 4xx errors while leaving
 * unexpected failures to propagate as 5xx.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

