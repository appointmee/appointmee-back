-- Dias de apertura por sucursal.
--
-- Se guardan en una sola columna de texto con los nombres de los dias separados
-- por coma, siempre en orden lunes a domingo:
--     MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY
--
-- Los dias que no figuran en la lista se toman como cerrados. Si la columna esta
-- vacia (NULL o '') la sucursal se considera abierta todos los dias; eso es solo
-- para que las sucursales que ya existian sigan funcionando igual que antes.

ALTER TABLE sucursales
    ADD COLUMN IF NOT EXISTS dias_apertura VARCHAR(120);


-- Las sucursales que ya existian abren todos los dias.
UPDATE sucursales
SET dias_apertura = 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY'
WHERE dias_apertura IS NULL
   OR dias_apertura = '';


-- Si en algun momento se corrio la primera version de este script (la que creaba
-- una tabla puente con una fila por dia), se puede borrar:
--
-- DROP TABLE IF EXISTS sucursal_dias_apertura;
