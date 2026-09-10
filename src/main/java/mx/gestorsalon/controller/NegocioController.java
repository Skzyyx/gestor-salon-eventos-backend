package mx.gestorsalon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.negocio.NegocioResponse;
import mx.gestorsalon.dto.negocio.NegocioUpdateRequest;
import mx.gestorsalon.service.NegocioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // Le dice a Spring que esto responderá con JSON
@RequestMapping("/negocios")
@RequiredArgsConstructor // Inyecta el Servicio automáticamente
public class NegocioController {

    private final NegocioService negocioService;

    // Obtener los datos de MI negocio
    // Como el servicio saca el negocio del JWT, no necesitamos pedir ningún ID en
    // la URL.
    @GetMapping("/me")
    public ResponseEntity<NegocioResponse> getMiNegocio() {
        return ResponseEntity.ok(negocioService.getMiNegocio());
    }

    // Actualizar los datos de MI negocio
    @PutMapping("/me")
    public ResponseEntity<NegocioResponse> updateMiNegocio(@Valid @RequestBody NegocioUpdateRequest request) {
        return ResponseEntity.ok(negocioService.updateMiNegocio(request));
    }
}
