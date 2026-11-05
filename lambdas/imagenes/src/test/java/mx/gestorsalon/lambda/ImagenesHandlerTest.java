package mx.gestorsalon.lambda;

import mx.gestorsalon.service.ImageValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImagenesHandlerTest {

    // Los mismos primeros bytes ("magic bytes") que usa ImageValidatorTest en el backend.
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
    private static final byte[] PDF = "%PDF-1.7\n%".getBytes(StandardCharsets.US_ASCII);

    private final ImagenesHandler handler = new ImagenesHandler("abc1234");

    /** Una petición como la arma la Function URL: método, y el cuerpo binario en base64. */
    private static Map<String, Object> peticion(String metodo, byte[] cuerpo) {
        Map<String, Object> evento = new HashMap<>();
        evento.put("requestContext", Map.of("http", Map.of("method", metodo)));
        evento.put("headers", Map.of("content-type", "image/png"));
        if (cuerpo != null) {
            evento.put("body", Base64.getEncoder().encodeToString(cuerpo));
            evento.put("isBase64Encoded", true);
        }
        return evento;
    }

    private static String cuerpo(Map<String, Object> respuesta) {
        return (String) respuesta.get("body");
    }

    static Stream<Arguments> imagenesPermitidas() {
        return Stream.of(
                Arguments.of(PNG, "image/png"),
                Arguments.of(JPEG, "image/jpeg"),
                Arguments.of(WEBP, "image/webp"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("imagenesPermitidas")
    @DisplayName("Un POST con JPEG, PNG o WEBP responde el tipo detectado, el tamaño y la versión")
    void imagenPermitida(byte[] imagen, String tipo) {
        Map<String, Object> r = handler.handleRequest(peticion("POST", imagen), null);

        assertEquals(tipo, r.get("tipo"));
        assertEquals(imagen.length, r.get("bytes"));
        assertEquals("abc1234", r.get("version"));
        assertFalse(r.containsKey("statusCode"));
    }

    @Test
    @DisplayName("Una invocación directa, sin método, cuenta como POST")
    void invocacionDirecta() {
        Map<String, Object> evento = Map.of(
                "body", Base64.getEncoder().encodeToString(PNG), "isBase64Encoded", true);

        assertEquals("image/png", handler.handleRequest(evento, null).get("tipo"));
    }

    @Test
    @DisplayName("Un PDF se rechaza con 400 por su contenido, aunque diga image/png")
    void pdfRechazado() {
        Map<String, Object> r = handler.handleRequest(peticion("POST", PDF), null);

        assertEquals(400, r.get("statusCode"));
        assertTrue(cuerpo(r).contains("Solo se permiten imágenes JPEG, PNG o WEBP"), () -> cuerpo(r));
    }

    @Test
    @DisplayName("Un cuerpo de texto, que no llega en base64, también se rechaza con 400")
    void textoRechazado() {
        Map<String, Object> evento = Map.of("body", "hola", "isBase64Encoded", false);

        Map<String, Object> r = handler.handleRequest(evento, null);

        assertEquals(400, r.get("statusCode"));
        assertTrue(cuerpo(r).contains("Solo se permiten imágenes JPEG, PNG o WEBP"), () -> cuerpo(r));
    }

    @Test
    @DisplayName("Sin cuerpo responde 400 con el mismo cuerpo { message } que el backend, más la versión")
    void sinCuerpo() {
        Map<String, Object> r = handler.handleRequest(peticion("POST", null), null);

        assertEquals(400, r.get("statusCode"));
        assertEquals(Map.of("content-type", ImagenesHandler.JSON), r.get("headers"));
        assertEquals("{\"message\":\"El archivo está vacío\",\"version\":\"abc1234\"}", cuerpo(r));
    }

    @Test
    @DisplayName("Un evento nulo se trata como un POST sin cuerpo")
    void eventoNulo() {
        assertEquals(400, handler.handleRequest(null, null).get("statusCode"));
    }

    @Test
    @DisplayName("Más de 4 MB responde 413, como el backend")
    void demasiadoGrande() {
        byte[] grande = Arrays.copyOf(PNG, (int) ImageValidator.MAX_BYTES + 1);

        Map<String, Object> r = handler.handleRequest(peticion("POST", grande), null);

        assertEquals(413, r.get("statusCode"));
        assertTrue(cuerpo(r).contains("La imagen pesa más de 4 MB"), () -> cuerpo(r));
    }

    @Test
    @DisplayName("Un cuerpo que dice venir en base64 y no lo es responde 400")
    void base64Invalido() {
        Map<String, Object> evento = Map.of("body", "esto no es base64!", "isBase64Encoded", true);

        Map<String, Object> r = handler.handleRequest(evento, null);

        assertEquals(400, r.get("statusCode"));
        assertTrue(cuerpo(r).contains(ImagenesHandler.NO_ES_BASE64), () -> cuerpo(r));
    }

    @Test
    @DisplayName("Un GET, como el navegador, dice cómo usarla y qué versión corre")
    void get() {
        Map<String, Object> r = handler.handleRequest(peticion("get", null), null);

        assertEquals(ImagenesHandler.USO, r.get("message"));
        assertEquals("abc1234", r.get("version"));
        assertFalse(r.containsKey("statusCode"));
    }

        @Test
    @DisplayName("Un método que no es GET ni POST responde 405 y dice cuáles sí")
    void metodoNoPermitido() {
        Map<String, Object> r = handler.handleRequest(peticion("DELETE", null), null);

        assertEquals(405, r.get("statusCode"));
        assertEquals(Map.of("content-type", ImagenesHandler.JSON, "allow", "GET, POST"), r.get("headers"));
        assertTrue(cuerpo(r).contains("Usa POST"), () -> cuerpo(r));
    }

    @Test
    @DisplayName("json escapa comillas, diagonales invertidas y caracteres de control")
    void json() {
        assertEquals("\"a\\\"b\\\\c\\u000a\"", ImagenesHandler.json("a\"b\\c\n"));
    }
}