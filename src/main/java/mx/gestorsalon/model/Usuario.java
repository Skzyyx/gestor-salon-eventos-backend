package mx.gestorsalon.model;

import jakarta.persistence.*;
import lombok.Data;
import mx.gestorsalon.model.enums.Rol;

/**
 * Entidad Usuario: Representa a las personas que pueden iniciar sesión.
 * Generalmente serán los Administradores dueños de los salones de eventos.
 */
@Data
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * RELACIÓN MULTI-TENANT (El corazón del sistema)
     * 
     * @ManyToOne: "Muchos" usuarios pueden pertenecer a "Un" negocio.
     *             fetch = FetchType.LAZY: Optimización. No trae los datos del
     *             negocio desde la base de datos
     *             hasta que explícitamente hagamos usuario.getNegocio().getNombre()
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = true)
    private Negocio negocio;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(unique = true, nullable = false, length = 100)
    private String correo;

    // Aquí se guardará la contraseña encriptada con BCrypt (nunca en texto plano)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Rol del usuario. Ahora es un enum (no String libre) para que solo
     * puedan existir valores válidos: ADMIN o SUPERADMIN.
     *
     * @Enumerated(EnumType.STRING): le dice a JPA que en la columna de MySQL
     * se guarde el TEXTO del enum ('ADMIN'), no su número de posición (0, 1).
     * Guardar el texto es más legible y más seguro: si mañana reordenamos el
     * enum, los datos viejos no se corrompen.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Rol rol = Rol.ADMIN;

    /**
     * Rol de negocio asignado (el "paquete de permisos" de su puesto).
     *
     * Solo aplica a EMPLEADO. Para ADMIN/SUPERADMIN es NULL: su poder viene del
     * enum Rol de arriba (ADMIN = comodín; SUPERADMIN = plataforma), no de aquí.
     *
     * LAZY como el resto de @ManyToOne. OJO: con open-in-view=false hay que
     * cargarlo
     * con JOIN FETCH cuando se va a leer fuera de la transacción (ver el
     * repositorio).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_negocio_id", nullable = true)
    private RolNegocio rolNegocio;

    @Column(nullable = false)
    private Boolean activo = true;
}
