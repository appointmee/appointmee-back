-- Indices de apoyo para las consultas de recomendaciones de SCRUM-364.
-- Se pueden ejecutar mas de una vez (IF NOT EXISTS) y no modifican datos.

BEGIN;

-- "Las mas demandadas": filtro por estado y rango de fechas agrupando por la
-- sucursal a traves del servicio del turno.
CREATE INDEX IF NOT EXISTS idx_turnos_estado_fecha_servicio
    ON public.turnos(estado, fecha_hora DESC, servicio_id);

-- "Nuevas en AppointMee": sucursales activas ordenadas por fecha de creacion.
CREATE INDEX IF NOT EXISTS idx_sucursales_activas_fecha_creacion
    ON public.sucursales(fecha_creacion DESC, id DESC)
    WHERE activo = true;

COMMIT;
