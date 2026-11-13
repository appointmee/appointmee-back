# Autenticación de Appointme

## Activar en Supabase

1. Ejecutar `src/main/resources/db/auth_tokens.sql` en el SQL Editor de Supabase antes de arrancar esta versión. Crea dos tablas y agrega `usuarios.credenciales_version`; no elimina usuarios ni roles. `ddl-auto=validate` se conserva.
2. La conexión PostgreSQL del backend debe tener acceso a estas tablas (por ejemplo, el propietario de las tablas). RLS no concede acceso a los roles públicos `anon` y `authenticated`. No se usa Supabase Auth.
3. Configurar `JWT_SECRET`, `RESEND_API_KEY`, `RESEND_FROM` y `AUTH_PASSWORD_RESET_URL` con la URL HTTPS de la pantalla de recuperación del frontend. El backend agrega `?token=...` o `&token=...`.
4. Desplegar el backend. Los JWT anteriores, sin tipo ni versión, dejan de aceptarse: hay que iniciar sesión nuevamente.

## Endpoints

| Método y ruta | Cuerpo JSON | Acceso |
|---|---|---|
| POST /auth/register | Los campos actuales de UsuarioCrearDTO | Público |
| POST /auth/login | `{"email":"...","pass":"..."}` | Público |
| POST /auth/refresh | `{"refreshToken":"..."}` | Público |
| POST /auth/forgot-password | `{"email":"..."}` | Público |
| POST /auth/reset-password | `{"token":"...","newPassword":"..."}` | Público |
| POST /auth/logout | Sin cuerpo | Bearer access token |
| POST /auth/change-password | `{"currentPassword":"...","newPassword":"..."}` | Bearer access token |
| GET /usuarios/me | Sin cuerpo | Bearer access token |

Login y refresh devuelven `token`, `accessToken` (alias del mismo JWT), `refreshToken` y `expiresIn` (segundos). Se conservan `/usuarios/inicio-sesion` y `/usuarios/crear` como alias; registro devuelve 201 y un mensaje JSON.

## Sesiones y permisos

- JWT de acceso: 15 minutos por defecto (`JWT_ACCESS_TTL_SECONDS`). Exige firma, vencimiento, `type=access`, usuario activo, ID y versión de credenciales coincidentes. Los roles se leen de la base en cada petición; conserva los controles de propiedad existentes de sucursales y turnos.
- Refresh: 32 bytes aleatorios, hash SHA-256 en la base, 7 días (`AUTH_REFRESH_TTL_SECONDS`). Una sesión renovable por usuario: login reemplaza el refresh anterior. Cada renovación consume el anterior. Bloqueo por usuario serializa operaciones concurrentes.
- Logout elimina el refresh; el access puede seguir vigente hasta vencer. Cambio/restablecimiento de contraseña, baja de cuenta y cambio de email eliminan refresh y recuperación, e incrementan la versión para rechazar inmediatamente access tokens anteriores.
- Contraseñas: conserva BCrypt y la política de registro. Ya no se pueden cambiar por `/usuarios/modificar/{id}`; usar `/auth/change-password` con la contraseña actual.
- Recuperación: token aleatorio de un solo uso, guardado como hash, válido 15 minutos (`AUTH_PASSWORD_RESET_TTL_SECONDS`). Nuevo envío invalida los anteriores y espera al menos un minuto entre envíos por cuenta. Tras uso se eliminan los tokens pendientes del usuario. Respuesta pública genérica para email existente/inexistente y fallos HTTP del proveedor; sin promesa de tiempos de respuesta constantes.
- `/usuarios/recuperar-cuenta` ya no permite reactivar cuentas deshabilitadas: el estado actual no distingue una baja voluntaria de un bloqueo administrativo.
- No se agregó CredentialsEntity ni otro sistema de permisos. La verificación de email existente queda como etapa posterior; no bloquea el registro/login.

## Frontend

Este cambio corresponde al backend. El frontend actual puede seguir iniciando sesión mediante el alias y el campo `token`, pero debe incorporar la renovación, guardar el nuevo refresh después de cada rotación y descartar ambos tokens al salir. Para recuperar contraseña necesita una pantalla en `AUTH_PASSWORD_RESET_URL` que lea el token y envíe la nueva contraseña. Después de cambiar/restablecer contraseña debe pedir login nuevamente. Evitar renovaciones simultáneas desde el cliente.

## Verificación

`AuthIntegrationTest` usa H2 y un proveedor de email simulado: comprueba rotación y concurrencia, revocación, vencimiento, permisos actuales, JWT invalidado por contraseña, recuperación de un solo uso y validación HTTP. No envía correos reales ni toca Supabase.
