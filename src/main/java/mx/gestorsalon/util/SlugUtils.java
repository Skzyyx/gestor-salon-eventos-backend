package mx.gestorsalon.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Convierte texto libre en la dirección pública de un negocio (slug):
 * minúsculas sin acentos, números y guiones sencillos, nunca en los extremos.
 * Ejemplo: "Salón Las Palmas" -> "salon-las-palmas".
 */
public final class SlugUtils {

    private SlugUtils() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        // NFD separa "ó" en "o" + acento; \p{M} borra el acento y deja la letra.
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = sinAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        // Tras el replaceAll solo puede quedar un guion en cada extremo.
        if (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        return slug;
    }
}
