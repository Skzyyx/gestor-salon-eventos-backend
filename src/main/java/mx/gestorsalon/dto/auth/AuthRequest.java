package mx.gestorsalon.dto.auth;

import lombok.Data;

/**
 * DTO que representa lo que el Frontend nos enviará al intentar hacer Login.
 */
@Data
public class AuthRequest {
    private String correo;
    private String password;
}
