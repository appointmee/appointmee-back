-- Agrega la columna para registrar la ultima modificacion del estado del turno.
ALTER TABLE turnos ADD COLUMN IF NOT EXISTS fecha_modificacion_estado TIMESTAMP;

-- Para los turnos PENDIENTE preexistentes sin fecha de modificacion de estado,
-- se utiliza su fecha de creacion como referencia inicial.
UPDATE turnos
SET fecha_modificacion_estado = fecha_reserva
WHERE estado = 'PENDIENTE'
  AND fecha_modificacion_estado IS NULL
  AND fecha_reserva IS NOT NULL;
