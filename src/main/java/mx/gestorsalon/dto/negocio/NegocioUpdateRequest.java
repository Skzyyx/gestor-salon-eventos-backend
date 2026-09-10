package mx.gestorsalon.dto.negocio;

import jakarta.validation.constraints.Email;
import mx.gestorsalon.dto.DireccionDTO;
import lombok.Data;

@Data
public class NegocioUpdateRequest {
    // Solo ponemos los campos que el usuario tiene PERMITIDO modificar.
    // Fíjate que no pusimos el ID ni el campo "activo", para evitar que un hacker
    // se desactive su negocio por accidente o intente cambiar el ID.
    private String nombre;
    private String slug;
    private String logoUrl;

    // CU-04: contacto
    private String telefono;

    @Email(message = "El correo electrónico no tiene un formato válido")
    private String email;

    private String descripcion;

    // CU-04: dirección como objeto anidado (espeja al value object Direccion)
    private DireccionDTO direccion;
}
