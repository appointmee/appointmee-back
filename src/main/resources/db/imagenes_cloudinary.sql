-- Ejecutar una sola vez sobre la base PostgreSQL de AppointMee antes de
-- desplegar la version del backend que agrega Imagen.publicId.
BEGIN;

ALTER TABLE imagenes
    ADD COLUMN IF NOT EXISTS public_id VARCHAR(255);

ALTER TABLE imagenes
    ALTER COLUMN url TYPE VARCHAR(1000);

COMMIT;
