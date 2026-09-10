package mx.gestorsalon.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidad Negocio: Representa a los salones de eventos (Tenants).
 * Esta es la tabla principal para la arquitectura Multi-tenant.
 */
@Data // Esto es de Lombok: crea getters, setters, toString y equals automáticamente
      // en tiempo de compilación
@Entity // Indica que esta clase se mapeará a una tabla de base de datos
@Table(name = "negocios") // Especifica el nombre exacto de la tabla en MySQL
public class Negocio {

    @Id // Marca este campo como la Llave Primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT en MySQL
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    // Slug es la versión URL-friendly del nombre (ej: "salon-la-fiesta").
    // Servirá para el portal público.
    @Column(unique = true, length = 50)
    private String slug;

    // CU-04: contacto y descripción pública del salón.
    @Column(length = 20)
    private String telefono;

    @Column(length = 255)
    private String email;

    @Column(length = 500)
    private String descripcion;

    // Dirección compuesta (objeto de valor reutilizable; sus columnas viven en esta tabla)
    @Embedded
    private Direccion direccion;

    // ¡NUEVO CAMPO PARA EL LOGO!
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(nullable = false)
    private Boolean activo = true;

    // updatable = false asegura que Hibernate nunca modifique esta fecha con un
    // UPDATE
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * @PrePersist es un "gatillo" (trigger) de JPA.
     *             Justo antes de que Hibernate haga el INSERT en la base de datos,
     *             automáticamente llenará el campo fechaCreacion con la hora
     *             actual.
     */
    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
