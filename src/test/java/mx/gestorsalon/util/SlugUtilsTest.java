package mx.gestorsalon.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlugUtilsTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @DisplayName("normalizar produce minúsculas sin acentos separadas por un solo guion")
    @CsvSource({
            "Salón Las Palmas,     salon-las-palmas",
            "'  Quinta Real  ',    quinta-real",
            "Jardín_Los-Álamos!!,  jardin-los-alamos",
            "Peña Azul 2026,       pena-azul-2026",
            "MAYÚSCULAS,           mayusculas",
            "ya-es-un-slug,        ya-es-un-slug"
    })
    void normalizar_textoLibre_devuelveSlug(String texto, String esperado) {
        // Arrange: cada fila del CsvSource es un caso (texto de entrada, slug esperado)

        // Act
        String slug = SlugUtils.normalizar(texto);

        // Assert
        assertEquals(esperado, slug);
    }

    @ParameterizedTest(name = "\"{0}\" -> vacío")
    @DisplayName("normalizar devuelve vacío cuando no hay letras ni números")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "¡¡!!", "---"})
    void normalizar_sinLetrasNiNumeros_devuelveVacio(String texto) {
        // Act
        String slug = SlugUtils.normalizar(texto);

        // Assert
        assertEquals("", slug);
    }
}
