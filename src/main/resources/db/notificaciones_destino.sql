-- SCRUM-327 / US-NOT-03 · destino concreto de las notificaciones in-app.
-- Ejecutar una vez sobre bases ya creadas con notificaciones.sql; ddl-auto=validate no altera el esquema.
-- Para instalaciones nuevas, notificaciones.sql ya incluye estas columnas.

BEGIN;

ALTER TABLE notificaciones ADD COLUMN IF NOT EXISTS destino_tipo VARCHAR(30);
ALTER TABLE notificaciones ADD COLUMN IF NOT EXISTS destino_id INTEGER;
ALTER TABLE notificaciones ADD COLUMN IF NOT EXISTS sucursal_id INTEGER;

-- Las notificaciones existentes no tenían destino: permanecen con NULL y el front
-- usa su ruta genérica por tipo (comportamiento previo a SCRUM-327).

COMMIT;
