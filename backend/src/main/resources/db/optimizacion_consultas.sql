-- Indices para las consultas optimizadas del backend.
--
-- Ejecutar una sola vez sobre la base PostgreSQL de AppointMee. Todos los
-- indices usan IF NOT EXISTS, por lo que el script se puede volver a correr sin
-- romper nada. No modifican datos ni el contrato de la API.
--
-- No se incluyeron indices de trigram porque los filtros actuales son igualdad
-- exacta (LOWER(columna) = LOWER(:valor)), no busquedas por prefijo/contiene.
-- Para validar el impacto real conviene correr EXPLAIN ANALYZE con datos de
-- produccion antes de sumar mas indices.

BEGIN;

-- Turnos:
--   listado del cliente por email, ordenado por fecha descendente con limite.
CREATE INDEX IF NOT EXISTS idx_turnos_cliente_fecha
    ON public.turnos(cliente_id, fecha_hora DESC, id DESC);

--   disponibilidad: empleado + rango de fechas (estado <> CANCELADO).
CREATE INDEX IF NOT EXISTS idx_turnos_empleado_fecha
    ON public.turnos(empleado_id, fecha_hora);

--   listado por sucursal y por empleado+estado, ordenado por fecha descendente.
CREATE INDEX IF NOT EXISTS idx_turnos_servicio_estado_fecha
    ON public.turnos(servicio_id, estado, fecha_hora DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_turnos_empleado_estado_fecha
    ON public.turnos(empleado_id, estado, fecha_hora DESC, id DESC);

-- Resenias:
--   historial de una sucursal ordenado por fecha descendente con limite.
CREATE INDEX IF NOT EXISTS idx_resenias_fecha
    ON public.resenias(fecha_resenia DESC, id DESC);

-- Solicitudes de empleado:
--   listado por sucursal y por empleado, ordenado por fecha descendente.
CREATE INDEX IF NOT EXISTS idx_solicitudes_empleado_sucursal_fecha
    ON public.solicitudes_empleado(sucursal_id, fecha_solicitud DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_solicitudes_empleado_empleado_fecha
    ON public.solicitudes_empleado(empleado_id, fecha_solicitud DESC, id DESC);

--   validacion de solicitud pendiente existente.
CREATE INDEX IF NOT EXISTS idx_solicitudes_empleado_empleado_sucursal_estado
    ON public.solicitudes_empleado(empleado_id, sucursal_id, estado);

-- Solicitudes de empleador:
--   listado de pendientes y de propias, ordenado por fecha descendente.
CREATE INDEX IF NOT EXISTS idx_solicitudes_empleador_estado_fecha
    ON public.solicitudes_empleador(estado, fecha_solicitud DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_solicitudes_empleador_usuario_fecha
    ON public.solicitudes_empleador(usuario_id, fecha_solicitud DESC, id DESC);

--   validacion de solicitud pendiente existente.
CREATE INDEX IF NOT EXISTS idx_solicitudes_empleador_usuario_estado
    ON public.solicitudes_empleador(usuario_id, estado);

-- Servicios:
--   listado por sucursal y filtro por activo.
CREATE INDEX IF NOT EXISTS idx_servicios_sucursal
    ON public.servicios(sucursal_id);
CREATE INDEX IF NOT EXISTS idx_servicios_activo
    ON public.servicios(activo);

-- Sucursales:
--   listado/filtro por activo y categoria, y busqueda por empleador.
CREATE INDEX IF NOT EXISTS idx_sucursales_activo_categoria
    ON public.sucursales(activo, categoria_id);
CREATE INDEX IF NOT EXISTS idx_sucursales_empleador
    ON public.sucursales(empleador_id);

-- Tabla puente de empleados:
--   "sucursales donde esta" y verificacion de pertenencia por email.
CREATE INDEX IF NOT EXISTS idx_sucursal_empleados_usuario
    ON public.sucursal_empleados(usuario_id, sucursal_id);

-- Imagenes:
--   galeria por sucursal y limpieza por public_id.
CREATE INDEX IF NOT EXISTS idx_imagenes_sucursal
    ON public.imagenes(sucursal_id);
CREATE INDEX IF NOT EXISTS idx_imagenes_public_id
    ON public.imagenes(public_id);

-- Usuarios:
--   listado administrativo con filtros exactos y estado.
CREATE INDEX IF NOT EXISTS idx_usuarios_activo
    ON public.usuarios(activo);
CREATE INDEX IF NOT EXISTS idx_usuarios_nombre_lower
    ON public.usuarios(LOWER(nombre));
CREATE INDEX IF NOT EXISTS idx_usuarios_apellido_lower
    ON public.usuarios(LOWER(apellido));

COMMIT;
