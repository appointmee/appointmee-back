-- Ejecutar una sola vez sobre la base PostgreSQL de AppointMee antes de
-- desplegar la version del backend que agrega Imagen.publicId y elimina
-- la columna url_alt (Cloudinary es el unico proveedor de imagenes).
BEGIN;

ALTER TABLE imagenes
    ADD COLUMN IF NOT EXISTS public_id VARCHAR(255);

ALTER TABLE imagenes
    ALTER COLUMN url TYPE VARCHAR(1000);

-- url_alt era un mecanismo de almacenamiento alternativo (placeholder externo).
-- Cloudinary es ahora el unico proveedor, por lo que la columna deja de usarse.
ALTER TABLE imagenes
    DROP COLUMN IF EXISTS url_alt;

COMMIT;
