package mx.gestorsalon.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que le devolveremos al Frontend si el login es exitoso.
 */
@Data
@Builder // Patrón builder para construir el objeto fácil
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token; // El billete de avión VIP
    private String nombre; // Para decirle "Hola Juan" en la UI
    private String rol; // Para ocultar o mostrar botones en React
}
