# 06 - API REST

Base: `http://localhost:8080`. Documentación interactiva: `/swagger-ui/index.html`, `/v3/api-docs`.

## Endpoints

- `/usuarios`: registro, login, recuperar cuenta, perfil, CRUD, listar/filtrar.
- `/sucursales`: CRUD, filtrar, categorías, empleados, imágenes, reseñas.
- `/servicios`: crear, modificar, eliminar, listar por sucursal.
- `/turnos`: registrar, disponibilidad, cancelar, finalizar, listar, reseñas.
- `/solicitudes-empleado`: enviadas/recibidas, aprobar/rechazar.
- `/solicitudes-empleador`: solicitar, listar, propias, aprobar/rechazar.
- `/pagos`: crear pago por turno, webhook.
- `/`: health/home.

Códigos de error y permisos: ver `GlobalExceptionHandler` y Swagger. No se inventan contratos adicionales.
