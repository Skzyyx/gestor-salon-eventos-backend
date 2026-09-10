package mx.gestorsalon.dto.equipo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RolNegocioResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean esSistema;
}
