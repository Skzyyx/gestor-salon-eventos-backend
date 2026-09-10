package mx.gestorsalon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.TurnoDTO;
import mx.gestorsalon.service.TurnoService;
import mx.gestorsalon.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<List<TurnoDTO>> getAll() {
        return ResponseEntity.ok(turnoService.getAllByNegocio(securityUtils.getNegocioActual().getId()));
    }

    @PostMapping
    public ResponseEntity<TurnoDTO> create(@Valid @RequestBody TurnoDTO dto) {
        return new ResponseEntity<>(turnoService.create(securityUtils.getNegocioActual(), dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TurnoDTO> update(@PathVariable Long id, @Valid @RequestBody TurnoDTO dto) {
        return ResponseEntity.ok(turnoService.update(securityUtils.getNegocioActual(), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        turnoService.delete(securityUtils.getNegocioActual(), id);
        return ResponseEntity.noContent().build();
    }

}
