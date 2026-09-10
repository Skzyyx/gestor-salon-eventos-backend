package mx.gestorsalon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Línea de un paquete (CU-07). De ENTRADA solo importan servicioId y cantidad;
 * el precio unitario lo congela el servidor al momento de guardar.
 */
@Data
public class PaqueteServicioDTO {

    @NotNull(message = "Cada línea debe indicar el servicio")
    private Long servicioId;

    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    // SALIDA: precio unitario congelado y servicio completo para el desglose.
    private BigDecimal precioUnitario;
    private ServicioDTO servicio;
}
