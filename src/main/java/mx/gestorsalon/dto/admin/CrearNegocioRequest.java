package mx.gestorsalon.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Datos que el SUPERADMIN envía para dar de alta un negocio nuevo
 * JUNTO con su primer usuario administrador (el dueño del salón).
 *
 * Las validaciones (@NotBlank, @Email, @Size) las revisa Spring solo cuando
 * el controlador reciba esto con @Valid: si algo no cumple, responde 400
 * automáticamente, sin siquiera entrar al servicio.
 */
@Data
public class CrearNegocioRequest {

    // ---- Datos del NEGOCIO ----
    @NotBlank(message = "El nombre del negocio es obligatorio")
    private String nombre;

    @NotBlank(message = "El slug (enlace público) es obligatorio")
    private String slug;

    // ---- Datos del ADMINISTRADOR inicial de ese negocio ----
    @NotBlank(message = "El nombre del administrador es obligatorio")
    private String adminNombre;

    @NotBlank(message = "El correo del administrador es obligatorio")
    @Email(message = "El correo del administrador no tiene un formato válido")
    private String adminCorreo;

    @NotBlank(message = "La contraseña del administrador es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String adminPassword;
}
