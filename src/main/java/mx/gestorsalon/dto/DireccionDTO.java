package mx.gestorsalon.dto;

import lombok.Data;

/**
 * DTO reutilizable para la dirección. Espeja al value object Direccion y se anida
 * dentro de los DTOs que la necesiten (NegocioResponse/Request y luego Cliente).
 */
@Data
public class DireccionDTO {
    private String calle;
    private String numero;
    private String colonia;
    private String ciudad;
    private String estado;
    private String codigoPostal;
}
