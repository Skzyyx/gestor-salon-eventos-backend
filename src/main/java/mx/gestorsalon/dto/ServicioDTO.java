package mx.gestorsalon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import mx.gestorsalon.model.enums.TipoCobro;

import java.math.BigDecimal;

@Data
public class ServicioDTO {
    private Long id;

    @NotBlank(message = "El nombre del servicio es requerido")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El precio es requerido")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    private String imagenUrl;

    private Boolean activo;

    @NotNull(message = "El tipo de cobro es obligatorio")
    private TipoCobro tipoCobro;
    // Solo el ID cuando el frontend mande a guardar el servicio
    private Long categoriaId;

    // El objeto completo para cuando el frontend consulte la lista de servicios
    private CategoriaDTO categoria;
}
