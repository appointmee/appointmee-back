# 07 - Base de datos

Entidades principales (`Model/`): Usuario, Rol, Sucursal, Servicio, Turno, SolicitudEmpleado, SolicitudEmpleador, Resenia, Imagen, Categoria.

## Relaciones

- Usuario tiene Rol.
- Sucursal tiene empleados (Usuario), servicios, imágenes, reseñas.
- Turno vincula cliente, empleado, servicio, sucursal.
- Reseña vincula turno/usuario/sucursal.
- Solicitudes vinculan usuarios y sucursales.

## Estados

- `EstadoTurno`: PENDIENTE, REALIZADO, CANCELADO, PAGO_RECHAZADO.
- `EstadoSolicitud`: PENDIENTE, RECHAZADA, APROBADA.

## Integridad

Hibernate `ddl-auto=validate`; el esquema debe existir previamente. No se incluyen scripts SQL en este repositorio (no verificados).
Diagrama ER no disponible en el repositorio.
