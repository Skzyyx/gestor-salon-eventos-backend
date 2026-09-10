package mx.gestorsalon.model.enums;

public enum Rol {

    /**
     * Dueño de la plataforma. NO pertenece a ningún negocio (negocio_id = NULL).
     * Único autorizado para dar de alta negocios y sus usuarios.
     */
    SUPERADMIN,

    /**
     * Dueño/administrador de UN negocio (tenant). Es el rol por defecto
     * de las cuentas que opera cada salón de eventos.
     * Tiene TODOS los permisos de su negocio de forma implícita (comodín).
     */
    ADMIN,

    /**
     * Trabajador de un negocio (recepcionista, coordinador, gerente...).
     * A diferencia de ADMIN, sus permisos NO son implícitos: vienen del
     * RolNegocio que tenga asignado (el "paquete de permisos" de su puesto).
     */
    EMPLEADO
}
