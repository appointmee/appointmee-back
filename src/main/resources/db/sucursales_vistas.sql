-- Historial de sucursales vistas recientemente por usuario (SCRUM-364).
--
-- Ejecutar una sola vez sobre la base PostgreSQL de AppointMee antes de
-- arrancar la aplicacion, porque spring.jpa.hibernate.ddl-auto=validate no crea
-- tablas automaticamente. El script es idempotente: se puede volver a correr
-- sin borrar ni modificar datos existentes.
--
-- Una fila por usuario y sucursal: las visitas repetidas actualizan
-- fecha_visita en la aplicacion, por eso la restriccion UNIQUE evita duplicados
-- incluso si dos dispositivos registran la misma sucursal al mismo tiempo.

BEGIN;

CREATE TABLE IF NOT EXISTS public.sucursales_vistas (
    id            SERIAL      PRIMARY KEY,
    usuario_id    INTEGER NOT NULL,
    sucursal_id   INTEGER NOT NULL,
    fecha_visita  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sucursales_vistas_usuario
        FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_sucursales_vistas_sucursal
        FOREIGN KEY (sucursal_id) REFERENCES public.sucursales(id) ON DELETE CASCADE,
    CONSTRAINT ux_sucursales_vistas_usuario_sucursal
        UNIQUE (usuario_id, sucursal_id)
);

-- Consulta de "Visto recientemente": ultimas visitas del usuario autenticado.
CREATE INDEX IF NOT EXISTS idx_sucursales_vistas_usuario_fecha
    ON public.sucursales_vistas(usuario_id, fecha_visita DESC);

-- Consulta inversa util para limpieza/auditoria por sucursal.
CREATE INDEX IF NOT EXISTS idx_sucursales_vistas_sucursal
    ON public.sucursales_vistas(sucursal_id);

COMMIT;
