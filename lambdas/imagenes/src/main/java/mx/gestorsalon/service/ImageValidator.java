package mx.gestorsalon.service;

import org.apache.tika.Tika;

import java.util.Set;

/**
 * Checks an uploaded image by its real content, never by its name or declared type:
 * not empty, at most 4 MB and only JPEG, PNG or WEBP.
 * No Spring dependencies, so the image Lambda can reuse it as is.
 */
public class ImageValidator {

    /** 4 MB: the same limit as spring.servlet.multipart.max-file-size and the frontend. */
    public static final long MAX_BYTES = 4L * 1024 * 1024;

    static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    /** Why an image was rejected. Each caller maps it to its own error: HTTP 400 or 413. */
    public enum Motivo { VACIA, DEMASIADO_GRANDE, TIPO_NO_PERMITIDO }

    public static class ImagenRechazadaException extends RuntimeException {
        private final Motivo motivo;

        public ImagenRechazadaException(Motivo motivo, String mensaje) {
            super(mensaje);
            this.motivo = motivo;
        }

        public Motivo getMotivo() {
            return motivo;
        }
    }

    private final Tika tika = new Tika();

    /**
     * Returns the MIME type detected from the content. Throws ImagenRechazadaException
     * with a message that is safe to show to the user.
     */
    public String validar(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new ImagenRechazadaException(Motivo.VACIA, "El archivo está vacío");
        }
        if (contenido.length > MAX_BYTES) {
            throw new ImagenRechazadaException(Motivo.DEMASIADO_GRANDE, "La imagen pesa más de 4 MB");
        }
        // Tika reads the first bytes (magic bytes), so a PDF or an .exe renamed to .jpg is caught.
        String tipo = tika.detect(contenido);
        if (!TIPOS_PERMITIDOS.contains(tipo)) {
            throw new ImagenRechazadaException(Motivo.TIPO_NO_PERMITIDO,
                    "Solo se permiten imágenes JPEG, PNG o WEBP. Tipo detectado: " + tipo);
        }
        return tipo;
    }
}