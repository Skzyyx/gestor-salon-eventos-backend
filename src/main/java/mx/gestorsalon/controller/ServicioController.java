package mx.gestorsalon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.ServicioDTO;
import mx.gestorsalon.service.ServicioService;
import mx.gestorsalon.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_SERVICIO_VER')")
    public ResponseEntity<List<ServicioDTO>> getAll() {
        return ResponseEntity.ok(servicioService.getAllByNegocio(securityUtils.getNegocioActual().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_SERVICIO_CREAR')")
    public ResponseEntity<ServicioDTO> create(@Valid @RequestBody ServicioDTO dto) {
        return new ResponseEntity<>(servicioService.create(securityUtils.getNegocioActual(), dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_SERVICIO_EDITAR')")
    public ResponseEntity<ServicioDTO> update(@PathVariable Long id, @Valid @RequestBody ServicioDTO dto) {
        return ResponseEntity.ok(servicioService.update(securityUtils.getNegocioActual(), id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_SERVICIO_ELIMINAR')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        servicioService.deactivate(securityUtils.getNegocioActual(), id);
        return ResponseEntity.noContent().build();
    }

}
