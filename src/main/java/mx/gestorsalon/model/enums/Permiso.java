package mx.gestorsalon.model.enums;

/**
 * CATÁLOGO DE PERMISOS ATÓMICOS — la unidad mínima de autorización del backend.
 *
 * Vive en código (enum, no entidad/tabla) por la misma razón que Rol y
 * TipoCobro:
 * cada permiso nuevo SIEMPRE implica código nuevo que lo protege
 * (un @PreAuthorize
 * en algún método). No tendría sentido "crear permisos en runtime" si no existe
 * código que los respete. El catálogo es cerrado a propósito.
 *
 * Convención de nombres: RECURSO_ACCION (SERVICIO_CREAR, TURNO_CANCELAR...).
 * Se persiste como TEXTO (@Enumerated(EnumType.STRING) en el ElementCollection
 * de
 * RolNegocio) para que la tabla siga siendo legible y reordenar el enum no
 * corrompa
 * datos.
 *
 * IMPORTANTE: en Spring Security, cada valor se expone como una authority con
 * prefijo "PERM_" (ej. PERM_SERVICIO_CREAR) y se exige con
 * @PreAuthorize("hasAuthority('PERM_SERVICIO_CREAR')"). El prefijo PERM_ los
 * separa
 * de los roles, que usan el prefijo ROLE_.
 *
 * Este catálogo CRECE a medida que se agregan módulos al producto.
 */
public enum Permiso {

    // --- Servicios (CRUD del catálogo de servicios del salón) ---
    SERVICIO_VER,
    SERVICIO_CREAR,
    SERVICIO_EDITAR,
    SERVICIO_ELIMINAR,

    // --- Categorías (VER + GESTIONAR; son un catálogo pequeño, no amerita CRUD
    // fino) ---
    CATEGORIA_VER,
    CATEGORIA_GESTIONAR,

    // --- Paquetes ---
    PAQUETE_VER,
    PAQUETE_CREAR,
    PAQUETE_EDITAR,
    PAQUETE_ELIMINAR,

    // --- Clientes ---
    CLIENTE_VER,
    CLIENTE_CREAR,
    CLIENTE_EDITAR,
    CLIENTE_ELIMINAR,

    // --- Turnos / agenda (la baja de un turno es "cancelar", no "eliminar") ---
    TURNO_VER,
    TURNO_CREAR,
    TURNO_EDITAR,
    TURNO_CANCELAR,

    // --- Perfil del negocio (datos del salón: nombre, contacto, logo...) ---
    NEGOCIO_VER_PERFIL,
    NEGOCIO_EDITAR_PERFIL,

    // --- Equipo / personal (CU-28): VER la lista y GESTIONAR
    // altas/bajas/asignación de rol ---
    EQUIPO_VER,
    EQUIPO_GESTIONAR,

    // --- Roles del negocio (CU-27): VER los roles y GESTIONAR su contenido
    // (permisos) ---
    ROL_VER,
    ROL_GESTIONAR
}
