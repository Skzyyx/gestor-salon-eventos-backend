package mx.gestorsalon.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import mx.gestorsalon.service.ImageValidator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * First phase of the image upload function (docs/lambdas-plan.md): it receives an image
 * through its Function URL and checks it with the backend's ImageValidator (an identical
 * copy, checked by the pipeline). The token check and the upload to Cloudinary come in
 * week 9, on top of this same function.
 *
 * <p>POST with the image bytes as the body: 200 with the type detected in the content, or
 * the same 400 and 413 { "message" } the backend answers. GET: which version is running.
 */
public class ImagenesHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    static final String USO = "Envía con POST una imagen JPEG, PNG o WEBP de hasta 4 MB como cuerpo";
    static final String NO_ES_BASE64 = "El cuerpo debía llegar en base64 y no se pudo leer";
    static final String JSON = "application/json; charset=utf-8";

    private final ImageValidator validator = new ImageValidator();
    private final String version;

    public ImagenesHandler() {
        this(System.getenv().getOrDefault("APP_VERSION", "manual"));
    }

    ImagenesHandler(String version) {
        this.version = version;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        Map<String, Object> evento = event == null ? Map.of() : event;
        if ("GET".equals(metodo(evento))) {
            Map<String, Object> uso = new LinkedHashMap<>();
            uso.put("message", USO);
            uso.put("version", version);
            return uso;
        }

        byte[] contenido;
        try {
            contenido = contenido(evento);
        } catch (IllegalArgumentException e) {
            return error(400, NO_ES_BASE64);
        }
        try {
            String tipo = validator.validar(contenido);
            // No statusCode: a Function URL answers 200 with this map as its JSON body.
            Map<String, Object> valida = new LinkedHashMap<>();
            valida.put("tipo", tipo);
            valida.put("bytes", contenido.length);
            valida.put("version", version);
            return valida;
        } catch (ImageValidator.ImagenRechazadaException e) {
            // Same mapping as the backend's ImageUploadService: too large is 413, the rest 400.
            int estado = e.getMotivo() == ImageValidator.Motivo.DEMASIADO_GRANDE ? 413 : 400;
            return error(estado, e.getMessage());
        }
    }

    /** Method of a Function URL request. A direct invocation has none and counts as POST. */
    static String metodo(Map<String, Object> evento) {
        if (evento.get("requestContext") instanceof Map<?, ?> contexto
                && contexto.get("http") instanceof Map<?, ?> http
                && http.get("method") instanceof String metodo) {
            return metodo.toUpperCase(Locale.ROOT);
        }
        return "POST";
    }

    /** The body as bytes. A Function URL sends a binary body, like an image, in base64. */
    static byte[] contenido(Map<String, Object> evento) {
        if (!(evento.get("body") instanceof String body)) {
            return new byte[0];
        }
        return Boolean.TRUE.equals(evento.get("isBase64Encoded"))
                ? Base64.getDecoder().decode(body)
                : body.getBytes(StandardCharsets.UTF_8);
    }

    /** A status other than 200, with the same { "message": ... } body as the backend. */
    private Map<String, Object> error(int estado, String mensaje) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("statusCode", estado);
        respuesta.put("headers", Map.of("content-type", JSON));
        respuesta.put("body", "{\"message\":" + json(mensaje) + ",\"version\":" + json(version) + "}");
        return respuesta;
    }

    /** A JSON string. The message can carry the type Tika detected, so it is escaped. */
    static String json(String texto) {
        StringBuilder json = new StringBuilder("\"");
        for (char c : texto.toCharArray()) {
            if (c == '"' || c == '\\') {
                json.append('\\').append(c);
            } else if (c < 0x20) {
                json.append(String.format("\\u%04x", (int) c));
            } else {
                json.append(c);
            }
        }
        return json.append('"').toString();
    }
}