package mx.gestorsalon.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * CU-07: Paquete que agrupa servicios con un precio conjunto.
 *
 * La relación con Servicio pasa por la entidad intermedia {@link PaqueteServicio}
 * (cantidad + precio unitario congelado), no por un N:M directo.
 */
@Data
@Entity
@Table(name = "paquetes")
public class Paquete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    // Líneas de detalle: el paquete es el dueño y administra su ciclo de vida
    // (cascade + orphanRemoval) para crear/actualizar/borrar sus servicios.
    @OneToMany(mappedBy = "paquete", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<PaqueteServicio> items = new ArrayList<>();

    @Column(nullable = false)
    private Boolean activo = true;
}
