-- Unicidad del Google Place ID solo entre sucursales activas.
--
-- Al inactivar una sucursal se libera su Place ID (queda en NULL), por lo que
-- la restriccion no puede abarcar tambien a las inactivas: eso impediria
-- reutilizar el identificador liberado. Si en la base existe otra restriccion
-- UNIQUE sobre google_place_id con un nombre distinto, revisarla manualmente
-- antes de ejecutar este script.
--
-- Impacto sobre datos existentes:
--   * Las sucursales inactivas que aun conservaban un Place ID quedan sin el,
--     igual que ocurre al inactivar una sucursal con el codigo nuevo. El resto
--     de sus datos no se modifica.
--   * Si hubiera dos sucursales activas con el mismo Place ID, el script falla
--     y hay que resolver ese conflicto antes de reintentarlo. No se borra ni
--     se reasigna informacion automaticamente.

BEGIN;

UPDATE public.sucursales
SET google_place_id = NULL
WHERE activo = false
  AND google_place_id IS NOT NULL;

ALTER TABLE public.sucursales
    DROP CONSTRAINT IF EXISTS ux_sucursales_google_place_id;
DROP INDEX IF EXISTS public.ux_sucursales_google_place_id;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.sucursales
        WHERE activo = true
          AND google_place_id IS NOT NULL
          AND btrim(google_place_id) <> ''
        GROUP BY btrim(google_place_id)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Hay sucursales activas con el mismo Google Place ID; resolver los duplicados antes de crear el indice unico';
    END IF;
END $$;

CREATE UNIQUE INDEX ux_sucursales_google_place_id
    ON public.sucursales (btrim(google_place_id))
    WHERE activo = true
      AND google_place_id IS NOT NULL
      AND btrim(google_place_id) <> '';

COMMIT;
