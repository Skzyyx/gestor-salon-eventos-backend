package mx.gestorsalon.service;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadService {

    private final Cloudinary cloudinary;
    private final ImageValidator imageValidator = new ImageValidator();

    public String uploadImage(MultipartFile file) {
        try {
            byte[] contenido = file.getBytes();

            // OWASP: tipo real por magic bytes (Apache Tika), solo JPEG, PNG o WEBP y hasta 4 MB.
            // La regla vive en ImageValidator, sin Spring, para que la Lambda de imágenes la reutilice.
            validar(contenido);

            // OWASP: Generar un nombre seguro y aleatorio para evitar sobreescrituras accidentales o inyección
            String safeFileName = UUID.randomUUID().toString();

            // Parámetros de Cloudinary
            Map<String, Object> params = Map.of(
                    "public_id", "servicios/" + safeFileName,
                    "overwrite", true,
                    "resource_type", "image"
            );

            // Subir archivo a Cloudinary
            Map<?, ?> uploadResult = cloudinary.uploader().upload(contenido, params);

            // Retornar la URL segura (https) provista por Cloudinary
            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("Error al subir imagen a Cloudinary", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar la imagen");
        }
    }

    // Traduce el rechazo al código HTTP del endpoint: 413 si pesa de más, 400 en los demás casos.
    private void validar(byte[] contenido) {
        try {
            imageValidator.validar(contenido);
        } catch (ImageValidator.ImagenRechazadaException e) {
            HttpStatus estado = e.getMotivo() == ImageValidator.Motivo.DEMASIADO_GRANDE
                    ? HttpStatus.PAYLOAD_TOO_LARGE
                    : HttpStatus.BAD_REQUEST;
            throw new ResponseStatusException(estado, e.getMessage());
        }
    }
}