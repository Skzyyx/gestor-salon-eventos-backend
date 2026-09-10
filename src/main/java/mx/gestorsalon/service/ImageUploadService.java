package mx.gestorsalon.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
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
    private final Tika tika = new Tika();

    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        }

        try {
            // OWASP: Validar Magic Bytes con Apache Tika
            // Esto lee la cabecera real del archivo, ignorando la extensión (.jpg, .exe, etc)
            String mimeType = tika.detect(file.getInputStream());
            if (!mimeType.startsWith("image/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo malicioso bloqueado. Tipo detectado: " + mimeType);
            }

            // Restricción estricta de formatos permitidos
            if (!mimeType.equals("image/jpeg") && !mimeType.equals("image/png") && !mimeType.equals("image/webp")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten imágenes JPEG, PNG o WEBP.");
            }

            // OWASP: Generar un nombre seguro y aleatorio para evitar sobreescrituras accidentales o inyección
            String safeFileName = UUID.randomUUID().toString();

            // Parámetros de Cloudinary
            Map<String, Object> params = Map.of(
                    "public_id", "servicios/" + safeFileName,
                    "overwrite", true,
                    "resource_type", "image"
            );

            // Subir archivo a Cloudinary
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            
            // Retornar la URL segura (https) provista por Cloudinary
            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("Error al subir imagen a Cloudinary", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar la imagen");
        }
    }
}
