package mx.gestorsalon.model;

import jakarta.persistence.*;
import lombok.Data;
import mx.gestorsalon.model.enums.Permiso;

import java.util.HashSet;
import java.util.Set;

/**
 * RolNegocio: el "paquete de permisos con nombre" que un negocio asigna a sus
 * trabajadores (EMPLEADO). Ej: "Recepcionista" = { TURNO_CREAR, CLIENTE_VER,
 * ... }.
 *
 * Es una entidad POR TENANT (cada negocio tiene sus propios roles, aislados).
 * A diferencia del enum Rol (eje de cuenta: SUPERADMIN/ADMIN/EMPLEADO), esto es
 * la capa de funcionalidad fina: define QUÉ puede hacer un empleado.
 */
@Data
@Entity
@Table(name = "roles_negocio",
        // Dos negocios pueden tener un rol "Gerente" cada uno, pero un mismo negocio
        // no puede tener dos roles con el mismo nombre. La unicidad es POR TENANT.
        uniqueConstraints = @UniqueConstraint(name = "uk_rol_negocio_nombre", columnNames = { "negocio_id", "nombre" }))
public class RolNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Igual que en Servicio/Categoria: a qué negocio pertenece este rol.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    /**
     * EL CORAZÓN DEL ROL: el conjunto de permisos que concede.
     *
     * @ElementCollection: para colecciones de valores "simples" (aquí, valores de
     *                     un enum) que NO son entidades propias. JPA crea una tabla
     *                     hija aparte.
     *                     En Java es un Set<Permiso>; en la BD es la tabla
     *                     rol_negocio_permisos con
     *                     (rol_negocio_id, permiso) — una fila por permiso del rol.
     *
     *                     Set (no List): un rol no puede tener el mismo permiso dos
     *                     veces, y el orden
     *                     da igual. Mapea natural a la semántica de "conjunto de
     *                     permisos".
     *
     *                     fetch = EAGER: cuando cargamos un rol, casi SIEMPRE
     *                     necesitamos sus permisos
     *                     (para construir las authorities en cada login). Con EAGER
     *                     evitamos el
     *                     LazyInitException al leerlos fuera de la transacción. Es
     *                     seguro porque el
     *                     set es pequeño y acotado (decenas de permisos como
     *                     máximo).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rol_negocio_permisos", joinColumns = @JoinColumn(name = "rol_negocio_id"))
    @Column(name = "permiso", length = 50)
    @Enumerated(EnumType.STRING)
    private Set<Permiso> permisos = new HashSet<>();

    /**
     * Roles-plantilla del sistema (Gerente/Coordinador/Recepcionista) que sembramos
     * en cada negocio. Son clonables pero NO borrables/editables por el tenant,
     * para
     * que siempre exista un punto de partida sano. Los roles que cree el negocio a
     * mano llevan esSistema = false.
     */
    @Column(name = "es_sistema", nullable = false)
    private Boolean esSistema = false;
}
