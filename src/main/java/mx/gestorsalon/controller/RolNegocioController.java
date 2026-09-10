package mx.gestorsalon.controller;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.equipo.RolNegocioResponse;
import mx.gestorsalon.security.CustomUserDetails;
import mx.gestorsalon.service.RolNegocioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/roles-negocio")
@RequiredArgsConstructor
public class RolNegocioController {

    private final RolNegocioService rolNegocioService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_ROL_VER') or hasAuthority('PERM_EQUIPO_GESTIONAR')")
    public List<RolNegocioResponse> listarRoles(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long negocioId = userDetails.getUsuario().getNegocio().getId();
        return rolNegocioService.obtenerRolesPorNegocio(negocioId).stream()
                .map(rol -> RolNegocioResponse.builder()
                        .id(rol.getId())
                        .nombre(rol.getNombre())
                        .descripcion(rol.getDescripcion())
                        .esSistema(rol.getEsSistema())
                        .build())
                .collect(Collectors.toList());
    }
}
