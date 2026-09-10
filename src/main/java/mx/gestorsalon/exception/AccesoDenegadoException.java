package mx.gestorsalon.exception;

/**
 * Se lanza cuando un usuario intenta operar sobre un recurso que NO pertenece a
 * su negocio (violación de aislamiento multi-tenant / IDOR).
 * El GlobalExceptionHandler la traduce a HTTP 403.
 */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
