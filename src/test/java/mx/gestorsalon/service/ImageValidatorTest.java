package mx.gestorsalon.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageValidatorTest {

    // Primeros bytes ("magic bytes") de cada formato. Tika decide por ellos, no por el nombre del archivo.
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
    private static final byte[] GIF = "GIF89a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PDF = "%PDF-1.7\n%".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0, (byte) 0xFF, (byte) 0xFF, 0, 0};

    private final ImageValidator validator = new ImageValidator();

    static Stream<Arguments> imagenesPermitidas() {
        return Stream.of(
                Arguments.of(PNG, "image/png"),
                Arguments.of(JPEG, "image/jpeg"),
                Arguments.of(WEBP, "image/webp"));
    }

    static Stream<Arguments> archivosRechazados() {
        return Stream.of(
                Arguments.of("GIF, imagen fuera de la lista", GIF),
                Arguments.of("PDF", PDF),
                Arguments.of("ejecutable de Windows", EXE));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("imagenesPermitidas")
    @DisplayName("validar acepta JPEG, PNG y WEBP y devuelve el tipo que detecta en el contenido")
    void validar_formatoPermitido_devuelveTipoDetectado(byte[] contenido, String tipoEsperado) {
        // Arrange: el contenido llega de imagenesPermitidas()

        // Act
        String tipo = validator.validar(contenido);

        // Assert
        assertEquals(tipoEsperado, tipo);
    }

    @ParameterizedTest(name = "{0} se rechaza")
    @MethodSource("archivosRechazados")
    @DisplayName("validar rechaza por su contenido lo que no es JPEG, PNG o WEBP")
    void validar_tipoNoPermitido_lanzaTipoNoPermitido(String caso, byte[] contenido) {
        // Arrange: el contenido llega de archivosRechazados()

        // Act
        ImageValidator.ImagenRechazadaException error = assertThrows(ImageValidator.ImagenRechazadaException.class,
                () -> validator.validar(contenido));

        // Assert
        assertEquals(ImageValidator.Motivo.TIPO_NO_PERMITIDO, error.getMotivo());
    }

    @ParameterizedTest(name = "contenido {0}")
    @NullAndEmptySource
    @DisplayName("validar rechaza un archivo nulo o vacío")
    void validar_archivoNuloOVacio_lanzaVacia(byte[] contenido) {
        // Arrange: el contenido llega de @NullAndEmptySource

        // Act
        ImageValidator.ImagenRechazadaException error = assertThrows(ImageValidator.ImagenRechazadaException.class,
                () -> validator.validar(contenido));

        // Assert
        assertEquals(ImageValidator.Motivo.VACIA, error.getMotivo());
        assertEquals("El archivo está vacío", error.getMessage());
    }

    @Test
    @DisplayName("validar acepta una imagen de exactamente 4 MB")
    void validar_exactamente4MB_devuelveTipo() {
        // Arrange: cabecera PNG rellenada con ceros hasta el límite
        byte[] contenido = Arrays.copyOf(PNG, (int) ImageValidator.MAX_BYTES);

        // Act
        String tipo = validator.validar(contenido);

        // Assert
        assertEquals("image/png", tipo);
    }

    @Test
    @DisplayName("validar rechaza una imagen de 4 MB más un byte")
    void validar_unByteMasDe4MB_lanzaDemasiadoGrande() {
        // Arrange
        byte[] contenido = Arrays.copyOf(PNG, (int) ImageValidator.MAX_BYTES + 1);

        // Act
        ImageValidator.ImagenRechazadaException error = assertThrows(ImageValidator.ImagenRechazadaException.class,
                () -> validator.validar(contenido));

        // Assert
        assertEquals(ImageValidator.Motivo.DEMASIADO_GRANDE, error.getMotivo());
        assertEquals("La imagen pesa más de 4 MB", error.getMessage());
    }
}