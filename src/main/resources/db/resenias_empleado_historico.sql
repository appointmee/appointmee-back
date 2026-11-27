-- SCRUM-368 · empleado historico opcional en las resenias.
-- ddl-auto=validate no altera el esquema: ejecutar una vez sobre PostgreSQL.
--
-- El nombre y apellido se copian en la resenia al crearla, para que sobrevivan
-- a la baja o desvinculacion del empleado. EmpleadoId es un valor simple, sin
-- FOREIGN KEY, para que borrar la cuenta nunca modifique una resenia existente.
BEGIN;

ALTER TABLE public.resenias ADD COLUMN IF NOT EXISTS empleado_id INTEGER;
ALTER TABLE public.resenias ADD COLUMN IF NOT EXISTS empleado_nombre VARCHAR(255);
ALTER TABLE public.resenias ADD COLUMN IF NOT EXISTS empleado_apellido VARCHAR(255);

-- No se hace backfill: las resenias anteriores se crearon sin la casilla
-- "Incluir empleado en la reseña" y deben seguir mostrando solo puntuacion
-- y comentario. Las columnas nuevas quedan NULL en esas filas.

COMMIT;
