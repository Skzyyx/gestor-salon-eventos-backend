package mx.gestorsalon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.CategoriaDTO;
import mx.gestorsalon.service.CategoriaService;
import mx.gestorsalon.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_CATEGORIA_VER')")
    public ResponseEntity<List<CategoriaDTO>> getAll() {
        return ResponseEntity.ok(categoriaService.getAllByNegocio(securityUtils.getNegocioActual().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CATEGORIA_GESTIONAR')")
    public ResponseEntity<CategoriaDTO> create(@Valid @RequestBody CategoriaDTO dto) {
        return new ResponseEntity<>(categoriaService.create(securityUtils.getNegocioActual(), dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CATEGORIA_GESTIONAR')")
    public ResponseEntity<CategoriaDTO> update(@PathVariable @NonNull Long id, @Valid @RequestBody CategoriaDTO dto) {
        return ResponseEntity.ok(categoriaService.update(securityUtils.getNegocioActual(), id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CATEGORIA_GESTIONAR')")
    public ResponseEntity<Void> deactivate(@PathVariable @NonNull Long id) {
        categoriaService.deactivate(securityUtils.getNegocioActual(), id);
        return ResponseEntity.noContent().build();
    }
}
