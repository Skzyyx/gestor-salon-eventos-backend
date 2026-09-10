-- ============================================================
-- V6: Sembrar el primer SUPERADMIN (bootstrap de la plataforma)
-- ============================================================
-- Problema del "huevo y la gallina": nadie puede crear al primer
-- SUPERADMIN desde la app porque aún no existe ningún usuario con
-- permiso para hacerlo. Solución: lo insertamos directo en la BD.
--
-- Puntos clave de esta fila:
--   negocio_id = NULL  -> es de plataforma, no pertenece a ningún salón
--                         (esto solo es posible gracias a la migración V5)
--   rol = 'SUPERADMIN' -> coincide con el enum Rol que creamos en Java
--   password_hash      -> hash BCrypt ($2a$10$...) de la contraseña real.
--                         NUNCA se guarda la contraseña en texto plano.
--
-- Credenciales sembradas (SOLO para desarrollo/demo, cámbialas en producción):
--   correo:      super@gestorsalon.com
--   contraseña:  SuperAdmin123!
-- ============================================================
INSERT INTO usuarios (negocio_id, nombre, correo, password_hash, rol, activo)
VALUES (
    NULL,
    'Super Admin',
    'super@gestorsalon.com',
    '$2a$10$6xOy9PUbybwKp7YzrhhbSeFUT.h/NUNrBCTDShQhT8hOEzcA2o1C6',
    'SUPERADMIN',
    TRUE
);