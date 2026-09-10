package mx.gestorsalon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.equipo.ActualizarTrabajadorRequest;
import mx.gestorsalon.dto.equipo.TrabajadorRequest;
import mx.gestorsalon.dto.equipo.TrabajadorResponse;
import mx.gestorsalon.security.CustomUserDetails;
import mx.gestorsalon.service.EquipoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/equipo")
@RequiredArgsConstructor
public class EquipoController {

    private final EquipoService equipoService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_EQUIPO_VER')")
    public List<TrabajadorResponse> listarEquipo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return equipoService.listarEquipo(userDetails.getUsuario().getNegocio().getId());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_EQUIPO_GESTIONAR')")
    public TrabajadorResponse registrarTrabajador(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TrabajadorRequest request) {
        return equipoService.registrarTrabajador(userDetails, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_EQUIPO_GESTIONAR')")
    public TrabajadorResponse actualizarTrabajador(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ActualizarTrabajadorRequest request) {
        return equipoService.actualizarTrabajador(id, userDetails, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('PERM_EQUIPO_GESTIONAR')")
    public TrabajadorResponse cambiarEstado(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam boolean activo) {
        return equipoService.cambiarEstado(id, userDetails, activo);
    }
}
