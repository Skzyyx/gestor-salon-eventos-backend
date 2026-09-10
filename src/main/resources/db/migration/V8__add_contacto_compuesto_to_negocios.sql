-- CU-04 (ampliado): correo empresarial y dirección compuesta.
-- Reemplazamos el campo único 'direccion' (V7) por sus componentes.
ALTER TABLE negocios
    DROP COLUMN direccion,
    ADD COLUMN email          VARCHAR(255) NULL,
    ADD COLUMN calle          VARCHAR(150) NULL,
    ADD COLUMN numero         VARCHAR(20)  NULL,
    ADD COLUMN colonia        VARCHAR(100) NULL,
    ADD COLUMN ciudad         VARCHAR(100) NULL,
    ADD COLUMN estado         VARCHAR(100) NULL,
    ADD COLUMN codigo_postal  VARCHAR(10)  NULL;
