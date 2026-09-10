package mx.gestorsalon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.PaqueteDTO;
import mx.gestorsalon.service.PaqueteService;
import mx.gestorsalon.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paquetes")
@RequiredArgsConstructor
public class PaqueteController {

    private final PaqueteService paqueteService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<List<PaqueteDTO>> getAll() {
        return ResponseEntity.ok(paqueteService.getAllByNegocio(securityUtils.getNegocioActual().getId()));
    }

    @PostMapping
    public ResponseEntity<PaqueteDTO> create(@Valid @RequestBody PaqueteDTO dto) {
        return new ResponseEntity<>(paqueteService.create(securityUtils.getNegocioActual(), dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaqueteDTO> update(@PathVariable Long id, @Valid @RequestBody PaqueteDTO dto) {
        return ResponseEntity.ok(paqueteService.update(securityUtils.getNegocioActual(), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        paqueteService.deactivate(securityUtils.getNegocioActual(), id);
        return ResponseEntity.noContent().build();
    }
}
