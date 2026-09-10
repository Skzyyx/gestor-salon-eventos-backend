-- CU-27 / CU-28: RBAC por tenant. Capa ADITIVA sobre el esquema existente
-- (V1-V11 quedan intactas; ddl-auto=validate seguirá cuadrando).
--
-- Esta migración SOLO crea estructura (DDL). La siembra de los roles-plantilla
-- (Gerente/Coordinador/Recepcionista) NO va aquí: se hace en Java de forma
-- idempotente, para tener UNA sola fuente de verdad reutilizable por la app y por
-- los tests (el perfil 'test' usa H2 create-drop sin Flyway).

-- 1) Los roles que cada negocio define para sus trabajadores.
CREATE TABLE roles_negocio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    negocio_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    es_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_roles_negocio_negocio FOREIGN KEY (negocio_id) REFERENCES negocios(id),
    -- Unicidad POR TENANT: cada negocio puede tener su propio "Gerente",
    -- pero no dos roles con el mismo nombre dentro del mismo negocio.
    CONSTRAINT uk_rol_negocio_nombre UNIQUE (negocio_id, nombre)
);

-- 2) Tabla hija del @ElementCollection<Permiso>: una fila por permiso de cada rol.
--    El permiso se guarda como TEXTO (EnumType.STRING), ej. 'SERVICIO_CREAR'.
CREATE TABLE rol_negocio_permisos (
    rol_negocio_id BIGINT NOT NULL,
    permiso VARCHAR(50) NOT NULL,
    -- Un permiso no puede repetirse dentro del mismo rol (refleja el Set<Permiso>).
    PRIMARY KEY (rol_negocio_id, permiso),
    CONSTRAINT fk_rnp_rol FOREIGN KEY (rol_negocio_id) REFERENCES roles_negocio(id) ON DELETE CASCADE
);

-- 3) El empleado apunta a UN rol de su negocio. NULL para ADMIN/SUPERADMIN
--    (su acceso viene del enum Rol, no de un RolNegocio).
ALTER TABLE usuarios
    ADD COLUMN rol_negocio_id BIGINT NULL,
    ADD CONSTRAINT fk_usuarios_rol_negocio FOREIGN KEY (rol_negocio_id) REFERENCES roles_negocio(id);
