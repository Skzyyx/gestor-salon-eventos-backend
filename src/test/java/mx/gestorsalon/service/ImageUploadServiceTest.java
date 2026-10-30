package mx.gestorsalon.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

    private static final String URL = "https://res.cloudinary.com/demo/image/upload/servicios/foto";

    // Primeros bytes ("magic bytes") de cada formato. Tika decide por ellos, no por el nombre del archivo.
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
    private static final byte[] GIF = "GIF89a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PDF = "%PDF-1.7\n%".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0, (byte) 0xFF, (byte) 0xFF, 0, 0};

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    @InjectMocks
    private ImageUploadService imageUploadService;

    static Stream<Arguments> imagenesPermitidas() {
        return Stream.of(
                Arguments.of("PNG", PNG),
                Arguments.of("JPEG", JPEG),
                Arguments.of("WEBP", WEBP));
    }

    static Stream<Arguments> archivosRechazados() {
        return Stream.of(
                Arguments.of("GIF, imagen fuera de la lista", GIF),
                Arguments.of("PDF renombrado a .jpg", PDF),
                Arguments.of("ejecutable renombrado a .jpg", EXE));
    }

    private static MockMultipartFile archivo(byte[] contenido) {
        // El nombre y el Content-Type mienten a propósito: el servicio no debe creerles.
        return new MockMultipartFile("file", "foto.jpg", "image/jpeg", contenido);
    }

    @ParameterizedTest(name = "{0} se sube")
    @DisplayName("uploadImage acepta JPEG, PNG y WEBP por su contenido real")
    @MethodSource("imagenesPermitidas")
    void uploadImage_formatoPermitido_devuelveUrlSegura(String formato, byte[] contenido) throws IOException {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", URL));

        // Act
        String url = imageUploadService.uploadImage(archivo(contenido));

        // Assert
        assertEquals(URL, url);
    }

    @ParameterizedTest(name = "{0} se rechaza")
    @DisplayName("uploadImage rechaza con 400 lo que no es JPEG, PNG o WEBP, aunque se llame .jpg")
    @MethodSource("archivosRechazados")
    void uploadImage_contenidoNoPermitido_lanza400SinSubir(String caso, byte[] contenido) {
        // Arrange
        MockMultipartFile archivo = archivo(contenido);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> imageUploadService.uploadImage(archivo));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("uploadImage rechaza un archivo vacío sin llamar a Cloudinary")
    void uploadImage_archivoVacio_lanza400() {
        // Arrange
        MockMultipartFile vacio = archivo(new byte[0]);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> imageUploadService.uploadImage(vacio));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("uploadImage responde 500 con un mensaje genérico si Cloudinary falla")
    void uploadImage_fallaCloudinary_lanza500() throws IOException {
        // Arrange
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("timeout"));
        MockMultipartFile png = archivo(PNG);

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> imageUploadService.uploadImage(png));

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
        assertEquals("Error al procesar la imagen", error.getReason());
    }

    @Test
    @DisplayName("uploadImage responde 413 si la imagen pasa de 4 MB, sin llamar a Cloudinary")
    void uploadImage_masDe4MB_lanza413() {
        // Arrange
        MockMultipartFile grande = archivo(Arrays.copyOf(PNG, (int) ImageValidator.MAX_BYTES + 1));

        // Act
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> imageUploadService.uploadImage(grande));

        // Assert
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, error.getStatusCode());
        assertEquals("La imagen pesa más de 4 MB", error.getReason());
        verifyNoInteractions(cloudinary);
    }
}