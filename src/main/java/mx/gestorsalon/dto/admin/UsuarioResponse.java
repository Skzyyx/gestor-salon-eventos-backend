package mx.gestorsalon.dto.admin;

import lombok.Builder;
import lombok.Data;

/**
 * Resumen de un usuario para devolver al frontend.
 * REGLA DE SEGURIDAD: nunca incluye el password_hash. Solo datos públicos.
 */
@Data
@Builder
public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String correo;
    private String rol;
}
