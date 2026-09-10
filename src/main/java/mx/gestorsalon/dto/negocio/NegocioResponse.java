package mx.gestorsalon.dto.negocio;

import mx.gestorsalon.dto.DireccionDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NegocioResponse {
    private Long id;
    private String nombre;
    private String slug;
    private String logoUrl;
    private Boolean activo;

    // CU-04: contacto y dirección
    private String telefono;
    private String email;
    private String descripcion;
    private DireccionDTO direccion;
}
