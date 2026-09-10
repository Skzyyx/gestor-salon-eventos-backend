package mx.gestorsalon.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador central de errores de la API.
 *
 * Se diseña como una PIRÁMIDE: muchos manejadores específicos arriba (cada uno
 * con su código HTTP intencional) y un único catch-all OPACO al fondo como red
 * de seguridad. Spring elige el manejador cuyo tipo es el MÁS CERCANO en la
 * jerarquía de herencia de la excepción lanzada (el orden de los métodos en el
 * archivo es irrelevante).
 *
 * Regla de oro: el catch-all `Exception` solo atrapa lo NO previsto y NUNCA
 * expone su mensaje interno al cliente (se registra en el log del servidor).
 * Los mensajes que SÍ se exponen son únicamente los que nosotros escribimos a
 * propósito en las excepciones de negocio.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Cuerpo de error uniforme: { "message": "..." } — lo que el frontend ya
     * espera.
     */
    private ResponseEntity<Map<String, String>> build(@NonNull HttpStatus status, String message) {
        String safe = (message != null && !message.isBlank()) ? message : status.getReasonPhrase();
        return ResponseEntity.status(status).body(Map.of("message", safe));
    }

    // -------------------- 400: Validación de DTOs (@Valid) --------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(" | "));
        return build(HttpStatus.BAD_REQUEST, mensaje);
    }

    // -------------------- 400: Regla de negocio --------------------
    // El mensaje lo escribimos nosotros en el Service, así que es seguro mostrarlo.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // -------------------- 404: Recurso inexistente --------------------
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // -------------------- 403: Recurso de otro tenant / sin permiso
    // --------------------
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, String>> handleAccesoDenegado(AccesoDenegadoException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // -------------------- 409: Unicidad (slug/correo ya en uso)
    // --------------------
    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<Map<String, String>> handleDuplicado(RecursoDuplicadoException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // -------------------- Respeta el status que viaja DENTRO de la excepción
    // --------------------
    // Lo usan, por ejemplo, AuthService (401) e ImageUploadService (400). Antes el
    // catch-all las aplastaba a 500; ahora conservan su código real.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        return build(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
    }

    // -------------------- 400: Cuerpo ilegible (JSON malformado o enum inválido)
    // --------------------
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "La solicitud tiene un formato inválido o campos no reconocidos");
    }

    // -------------------- 500: ÚLTIMO RECURSO (lo no previsto)
    // --------------------
    // NUNCA exponemos ex.getMessage() aquí: podría filtrar SQL, rutas o internals.
    // El detalle real va al log; al cliente solo un mensaje genérico.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        log.error("Error no controlado en la API", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Inténtalo de nuevo más tarde.");
    }
}
