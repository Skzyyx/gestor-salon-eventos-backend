package mx.gestorsalon.exception;

/**
 * Se lanza cuando se viola una regla de unicidad (slug o correo ya en uso).
 * El GlobalExceptionHandler la traduce a HTTP 409 Conflict.
 */
public class RecursoDuplicadoException extends RuntimeException {
    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
