package mx.gestorsalon.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * CU-08: Cliente del salón, gestionado por el administrador (directorio interno).
 *
 * Es una entidad independiente: no tiene cuenta ni login (el actor Cliente del PD
 * "no requiere crear una cuenta"). Reutiliza el value object {@link Direccion} embebido,
 * tal como lo hace Negocio, para no duplicar columnas ni crear un JOIN extra.
 */
@Data
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(length = 255)
    private String email;

    @Embedded
    private Direccion direccion;

    @Column(nullable = false)
    private Boolean activo = true;
}
