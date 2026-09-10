package mx.gestorsalon.dto.admin;

import mx.gestorsalon.dto.negocio.NegocioResponse;
import lombok.Builder;
import lombok.Data;

/**
 * Respuesta al crear un negocio: devuelve el negocio creado y, junto a él,
 * el administrador inicial que se generó. Así el SUPERADMIN ve de inmediato
 * con qué correo quedó el dueño del nuevo salón.
 *
 * Reutilizamos tu NegocioResponse existente para la parte del negocio.
 */
@Data
@Builder
public class NegocioAdminResponse {
    private NegocioResponse negocio;
    private UsuarioResponse admin;
}
