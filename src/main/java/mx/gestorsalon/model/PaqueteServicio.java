package mx.gestorsalon.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * CU-07: Línea de detalle de un paquete (entidad intermedia Paquete <-> Servicio).
 *
 * No basta una relación N:M directa: cada servicio dentro de un paquete necesita su
 * propia CANTIDAD y un PRECIO UNITARIO "congelado" al momento de armar el paquete,
 * para que cambiar después el precio base del servicio no altere paquetes ya definidos.
 *
 * Las anotaciones @ToString.Exclude / @EqualsAndHashCode.Exclude sobre {@code paquete}
 * evitan la recursión infinita de Lombok en la relación bidireccional.
 */
@Data
@Entity
@Table(name = "paquete_servicios", uniqueConstraints = @UniqueConstraint(name = "uq_paquete_servicio", columnNames = {
        "paquete_id", "servicio_id" }))
public class PaqueteServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paquete_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Paquete paquete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Column(nullable = false)
    private Integer cantidad = 1;

    // Precio del servicio congelado al momento de guardar el paquete.
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;
}
