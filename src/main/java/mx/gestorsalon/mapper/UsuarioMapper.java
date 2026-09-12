package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.admin.UsuarioResponse;
import mx.gestorsalon.model.Usuario;
import org.springframework.stereotype.Component;

/**
 * Convierte la entidad Usuario en su DTO seguro (sin password_hash).
 */
@Component
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario usuario) {
        if (usuario == null)
            return null;

        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                // Protección extra: si por algún motivo el rol fuera null, no reventamos.
                .rol(usuario.getRol() != null ? usuario.getRol().name() : null)
                .build();
    }
}
