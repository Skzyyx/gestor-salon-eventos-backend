-- CU-07 (refinado): el paquete se relaciona con sus servicios a través de una
-- entidad intermedia con CANTIDAD y PRECIO UNITARIO congelado. Además, el paquete
-- ahora puede tener imagen.
--
-- Nota: la tabla puente simple de V10 (solo paquete_id + servicio_id) se reemplaza
-- por una con detalle. Como es una feature aún sin datos reales, se recrea.

ALTER TABLE paquetes
    ADD COLUMN imagen_url VARCHAR(500);

DROP TABLE paquete_servicios;

CREATE TABLE paquete_servicios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paquete_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_ps_paquete FOREIGN KEY (paquete_id) REFERENCES paquetes(id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_servicio FOREIGN KEY (servicio_id) REFERENCES servicios(id),
    CONSTRAINT uq_paquete_servicio UNIQUE (paquete_id, servicio_id)
);
