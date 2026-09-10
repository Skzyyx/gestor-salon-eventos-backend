package mx.gestorsalon.exception;

/**
 * Se lanza cuando se pide un recurso que no existe (por id inexistente).
 * El GlobalExceptionHandler la traduce a HTTP 404.
 *
 * La capa de Servicio la lanza SIN saber nada de HTTP: el mapeo a 404 vive
 * únicamente en el handler, manteniendo las capas separadas.
 */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
