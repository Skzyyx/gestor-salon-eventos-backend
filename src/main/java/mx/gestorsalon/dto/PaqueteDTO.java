package mx.gestorsalon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PaqueteDTO {

    private Long id;

    @NotBlank(message = "El nombre del paquete es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El precio es requerido")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a $0")
    private BigDecimal precio;

    private String imagenUrl;

    private Boolean activo;

    // Líneas del paquete (servicio + cantidad). Al menos una (FA-07.2).
    @NotEmpty(message = "Un paquete debe incluir al menos un servicio")
    @Valid
    private List<PaqueteServicioDTO> items;
}
