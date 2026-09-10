package mx.gestorsalon.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.repository.UsuarioRepository;
import mx.gestorsalon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UsuarioRepository usuarioRepository;

    /**
     * Devuelve el Usuario autenticado, recargado fresco desde la BD.
     * Lo usamos cuando NO queremos asumir que tenga negocio (ej. el SUPERADMIN).
     */
    public Usuario getUsuarioActual() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return usuarioRepository.findById(userDetails.getUsuario().getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    /**
     * Devuelve el negocio del usuario autenticado.
     * Si es un SUPERADMIN (sin negocio), lanza un error claro: estos endpoints
     * son para usuarios de UN negocio (tenant), no para la plataforma.
     */
    public Negocio getNegocioActual() {
        Negocio negocio = getUsuarioActual().getNegocio();
        if (negocio == null) {
            throw new IllegalStateException(
                    "El usuario actual no pertenece a ningún negocio (¿es SUPERADMIN?).");
        }
        return negocio;
    }
}
