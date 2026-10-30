package mx.gestorsalon.exception;

import mx.gestorsalon.controller.CategoriaController;
import mx.gestorsalon.dto.CategoriaDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Prueba unitaria del manejador central de errores.
 *
 * Spring elige el @ExceptionHandler cuyo tipo es el MÁS CERCANO a la excepción
 * lanzada. Para probar exactamente eso (y no solo el cuerpo de cada método), cada
 * prueba resuelve el manejador con el mismo algoritmo que usa el DispatcherServlet
 * ({@link ExceptionHandlerMethodResolver}) y lo invoca sobre una instancia real.
 * Así, si una excepción cae por error en el catch-all, la prueba lo detecta.
 */
class GlobalExceptionHandlerTest {

    private static final String MENSAJE_GENERICO = "Ocurrió un error inesperado. Inténtalo de nuevo más tarde.";
    private static final String MENSAJE_SIN_PERMISO = "No tienes permiso para realizar esta acción";
    private static final String MENSAJE_CUERPO_INVALIDO =
            "La solicitud tiene un formato inválido o campos no reconocidos";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ExceptionHandlerMethodResolver resolver =
            new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class);

    /** Lo que haría Spring: elegir el manejador más cercano e invocarlo. */
    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, String>> responder(Exception ex) {
        Method manejador = resolver.resolveMethod(ex);
        try {
            return (ResponseEntity<Map<String, String>>) manejador.invoke(handler, ex);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("No se pudo invocar " + manejador.getName(), e);
        }
    }

    // -------------------- Excepciones de negocio y de seguridad --------------------

    static Stream<Arguments> excepcionesConocidas() {
        return Stream.of(
                arguments(new IllegalArgumentException("Ya existe un servicio activo con ese nombre"),
                        HttpStatus.BAD_REQUEST, "Ya existe un servicio activo con ese nombre"),
                arguments(new RecursoNoEncontradoException("Categoría no encontrada"),
                        HttpStatus.NOT_FOUND, "Categoría no encontrada"),
                arguments(new AccesoDenegadoException("La categoría no pertenece a su negocio"),
                        HttpStatus.FORBIDDEN, "La categoría no pertenece a su negocio"),
                arguments(new RecursoDuplicadoException("Ya existe un negocio con ese slug"),
                        HttpStatus.CONFLICT, "Ya existe un negocio con ese slug"),
                // Lo que lanza @PreAuthorize cuando falta el PERM_*: el mensaje interno de
                // Spring ("Access Denied") se reemplaza por uno propio en español.
                arguments(new AccessDeniedException("Access Denied"),
                        HttpStatus.FORBIDDEN, MENSAJE_SIN_PERMISO),
                arguments(new AuthorizationDeniedException("Access Denied", new AuthorizationDecision(false)),
                        HttpStatus.FORBIDDEN, MENSAJE_SIN_PERMISO),
                // El archivo pasa de spring.servlet.multipart.max-file-size: 413, no 500.
                arguments(new MaxUploadSizeExceededException(4L * 1024 * 1024),
                        HttpStatus.PAYLOAD_TOO_LARGE, "La imagen pesa más de 4 MB"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("excepcionesConocidas")
    @DisplayName("Cada excepción conocida se traduce a su código HTTP y a un cuerpo { message }")
    void responder_excepcionConocida_devuelveSuCodigoYMensaje(Exception ex, HttpStatus esperado, String mensaje) {
        // Arrange
        Map<String, String> cuerpoEsperado = Map.of("message", mensaje);

        // Act
        ResponseEntity<Map<String, String>> respuesta = responder(ex);

        // Assert
        assertEquals(esperado, respuesta.getStatusCode());
        assertEquals(cuerpoEsperado, respuesta.getBody());
    }

    @ParameterizedTest(name = "mensaje {0} -> \"Bad Request\"")
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("Un mensaje nulo o en blanco se reemplaza por la frase estándar del código HTTP")
    void responder_mensajeNuloOEnBlanco_usaFraseDelEstado(String mensaje) {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException(mensaje);

        // Act
        ResponseEntity<Map<String, String>> respuesta = responder(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("Bad Request", respuesta.getBody().get("message"));
    }

    // -------------------- ResponseStatusException conserva su código --------------------

    @ParameterizedTest(name = "{0} conserva su código y su motivo")
    @EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "UNAUTHORIZED", "NOT_FOUND", "CONFLICT",
            "INTERNAL_SERVER_ERROR"})
    @DisplayName("ResponseStatusException conserva el código y el motivo que trae dentro")
    void responder_responseStatusException_conservaCodigoYMotivo(HttpStatus estado) {
        // Arrange
        ResponseStatusException ex = new ResponseStatusException(estado, "Motivo escrito por el servicio");

        // Act
        ResponseEntity<Map<String, String>> respuesta = responder(ex);

        // Assert
        assertEquals(estado, respuesta.getStatusCode());
        assertEquals("Motivo escrito por el servicio", respuesta.getBody().get("message"));
    }

    @ParameterizedTest(name = "{0} sin motivo usa su frase estándar")
    @EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "UNAUTHORIZED", "NOT_FOUND"})
    @DisplayName("ResponseStatusException sin motivo responde con la frase estándar de su código")
    void responder_responseStatusExceptionSinMotivo_usaFraseDelEstado(HttpStatus estado) {
        // Arrange
        ResponseStatusException ex = new ResponseStatusException(estado);

        // Act
        ResponseEntity<Map<String, String>> respuesta = responder(ex);

        // Assert
        assertEquals(estado, respuesta.getStatusCode());
        assertEquals(estado.getReasonPhrase(), respuesta.getBody().get("message"));
    }

    // -------------------- 500: catch-all opaco --------------------

    static Stream<Exception> erroresInternos() {
        return Stream.of(
                new RuntimeException("could not execute statement; SQL [insert into usuarios (password_hash) ...]"),
                new NullPointerException("Cannot invoke \"mx.gestorsalon.model.Negocio.getId()\" because null"),
                new IllegalStateException("El usuario actual no pertenece a ningún negocio (¿es SUPERADMIN?)."));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("erroresInternos")
    @DisplayName("Un error no previsto responde 500 genérico y nunca expone el mensaje interno")
    void responder_errorNoPrevisto_responde500SinFiltrarDetalles(Exception ex) {
        // Arrange
        String detalleInterno = ex.getMessage();

        // Act
        ResponseEntity<Map<String, String>> respuesta = responder(ex);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals(Map.of("message", MENSAJE_GENERICO), respuesta.getBody());
        assertFalse(respuesta.getBody().get("message").contains(detalleInterno));
    }

    // -------------------- 400: @Valid --------------------

    static Stream<Arguments> erroresDeCampo() {
        return Stream.of(
                arguments(List.of("El nombre de la categoría es obligatorio"),
                        "El nombre de la categoría es obligatorio"),
                arguments(List.of("El nombre del servicio es requerido", "El precio es requerido",
                        "El tipo de cobro es obligatorio"),
                        "El nombre del servicio es requerido | El precio es requerido | El tipo de cobro es obligatorio"),
                // Solo errores globales (no de campo): no hay texto que unir -> frase estándar
                arguments(List.of(), "Bad Request"));
    }

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @MethodSource("erroresDeCampo")
    @DisplayName("La validación de DTOs responde 400 uniendo los mensajes de campo con \" | \"")
    void responder_validacionFallida_unaLosMensajesDeCampo(List<String> mensajes, String esperado)
            throws NoSuchMethodException {
        // Arrange
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new CategoriaDTO(), "categoriaDTO");
        for (String mensaje : mensajes) {
            resultado.addError(new FieldError("categoriaDTO", "campo" + resultado.getErrorCount(), mensaje));
        }
        resultado.addError(new ObjectError("categoriaDTO", "Error global que no es de un campo"));
        MethodParameter parametro = new MethodParameter(
                CategoriaController.class.getMethod("create", CategoriaDTO.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parametro, resultado);

        // Act
        ResponseEntity<Map<String, String>> respuesta = responder(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals(esperado, respuesta.getBody().get("message"));
    }

    // -------------------- 400: cuerpo ilegible --------------------

    @Test
    @DisplayName("Un JSON ilegible responde 400 con un mensaje fijo, sin el detalle del parser")
    void responder_cuerpoIlegible_responde400ConMensajeFijo() {
        // Arrange
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Cannot deserialize value of type `java.time.DayOfWeek` from String \"LUNES\"",
                new MockHttpInputMessage(new byte[0]));

        // Act
        ResponseEntity<Map<String, String>> respuesta = responder(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals(Map.of("message", MENSAJE_CUERPO_INVALIDO), respuesta.getBody());
    }
}