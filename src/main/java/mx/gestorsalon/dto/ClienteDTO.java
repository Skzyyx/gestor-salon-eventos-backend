package mx.gestorsalon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de Cliente (CU-08). Reutiliza el {@link DireccionDTO} anidado, igual que NegocioResponse.
 * El correo es opcional, pero si viene debe tener formato válido (@Email ignora null/vacío).
 */
@Data
public class ClienteDTO {

    private Long id;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombre;

    @NotBlank(message = "El teléfono del cliente es obligatorio")
    private String telefono;

    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    @Valid
    private DireccionDTO direccion;

    private Boolean activo;
}
