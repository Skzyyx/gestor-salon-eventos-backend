-- CU-04: Campos del perfil del negocio exigidos por el Plan de Proyecto
-- (dirección, teléfono y descripción pública del salón).
ALTER TABLE negocios
    ADD COLUMN direccion   VARCHAR(255) NULL,
    ADD COLUMN telefono    VARCHAR(20)  NULL,
    ADD COLUMN descripcion VARCHAR(500) NULL;
