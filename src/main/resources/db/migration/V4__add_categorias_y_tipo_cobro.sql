-- 1. Crear tabla de categorías
CREATE TABLE categorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    negocio_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_categoria_negocio FOREIGN KEY (negocio_id) REFERENCES negocios(id)
);

-- 2. Añadir columnas a la tabla servicios
ALTER TABLE servicios
ADD COLUMN categoria_id BIGINT,
ADD COLUMN tipo_cobro VARCHAR(50);

-- 3. Crear llave foránea en servicios hacia categorías
ALTER TABLE servicios
ADD CONSTRAINT fk_servicio_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id);
