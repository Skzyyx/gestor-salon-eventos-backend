package mx.gestorsalon.dto.equipo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrabajadorResponse {
    private Long id;
    private String nombre;
    private String correo;
    private Boolean activo;
    private RolNegocioResponse rol;
}
