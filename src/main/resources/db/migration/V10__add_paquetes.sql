-- CU-07: Paquetes (agrupación de servicios con precio conjunto) por negocio.
CREATE TABLE paquetes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    negocio_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    precio DECIMAL(10, 2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_paquetes_negocio FOREIGN KEY (negocio_id) REFERENCES negocios(id)
);

-- Tabla intermedia de la relación N:M Paquete <-> Servicio.
CREATE TABLE paquete_servicios (
    paquete_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    PRIMARY KEY (paquete_id, servicio_id),
    CONSTRAINT fk_ps_paquete FOREIGN KEY (paquete_id) REFERENCES paquetes(id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_servicio FOREIGN KEY (servicio_id) REFERENCES servicios(id)
);
