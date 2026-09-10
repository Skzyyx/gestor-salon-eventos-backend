package mx.gestorsalon.controller;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.service.ImageUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageUploadService imageUploadService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String secureUrl = imageUploadService.uploadImage(file);
        // Retornamos un JSON estructurado para que sea fácil de leer en el Frontend
        return ResponseEntity.ok(Collections.singletonMap("url", secureUrl));
    }
}
