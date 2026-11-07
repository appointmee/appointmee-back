# 08 - Integraciones

## Mercado Pago

Flujo general: crear pago por turno (`POST /pagos/{turnoId}`) y recibir webhook (`POST /pagos/webhook`). Requiere `MP_ACCESS_TOKEN`.

## Cloudinary

Subida de imagenes (`POST /imagenes/upload` con `multipart/form-data`: `file` + `tipo` (`perfiles`, `sucursales`, `servicios`, `galerias`)) y eliminacion de recursos propios de la app (`appointmee/...`).

- Configurar via variables de entorno: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.
- Las URLs y `public_id` se guardan en la tabla `imagenes`.
- Los endpoints de perfiles/sucursales/servicios reciben `fotoUrl` + `fotoPublicId` al modificar.

## WhatsApp

No se encontró integración en el backend revisado. Documentar cuando esté implementada.

## Configuración

No incluir API keys, tokens ni secretos. Usar variables de entorno.
Posibles errores: credenciales inválidas, webhook no alcanzable, timeouts de red.
