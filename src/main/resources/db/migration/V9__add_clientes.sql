-- CU-08: Directorio de clientes por negocio.
-- Reutiliza el value object Direccion: sus columnas viven aquí (sin tabla ni JOIN extra).
CREATE TABLE clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    negocio_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    -- Dirección compuesta (todos los campos opcionales)
    calle VARCHAR(150),
    numero VARCHAR(20),
    colonia VARCHAR(100),
    ciudad VARCHAR(100),
    estado VARCHAR(100),
    codigo_postal VARCHAR(10),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_clientes_negocio FOREIGN KEY (negocio_id) REFERENCES negocios(id)
);
