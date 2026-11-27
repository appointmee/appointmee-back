# ADR 0001 — Asistente de IA con acceso a datos: usar Spring AI en el backend

- **Estado:** Aceptada
- **Fecha:** 2026-11-26
- **Ámbito:** `appointmee-back` + consumidor en `appointmee-front`
- **Relacionado:** `SKILLS.md` (backend), `AGENTS.md` (frontend), US-GEN-01 (sistema de diseño), US-GEN-04 (seguridad y calidad)

---

## 1. Contexto

AppointMee necesita un chat con IA para **clientes y visitantes** que pueda:

> «Tengo estos horarios disponibles, necesito un corte de pelo, soy hombre y quiero que encuentre un turno ideal cerca de mi casa.»

Es decir, el asistente debe **consultar datos reales y ejecutar acciones**, no solo conversar:

1. buscar sucursales por categoría/servicio y cercanía;
2. consultar disponibilidad real de turnos;
3. proponer un horario;
4. reservarlo previa confirmación del usuario.

### Hechos verificados del código actual

| Hecho | Fuente |
| --- | --- |
| Backend Java 21 / Spring Boot 4.0.5, PostgreSQL, JWT stateless, Docker en :8080 | `SKILLS.md`, `pom.xml`, `Dockerfile` |
| Frontend Angular 22 standalone, sin SSR, build estático en Vercel | `appointmee-front/AGENTS.md`, `vercel.json` |
| `Sucursal` ya persiste `latitud`, `longitud` y `googlePlaceId`; `ECategoria` incluye `BARBERIA` y `PELUQUERIA` | `features/sucursal/Sucursal.java`, `features/sucursal/categoria/ECategoria.java` |
| `Servicio` tiene `nombre`, `descripcion`, `duracion`, `precio` y `sucursal`; **no tiene campo de género** | `features/servicio/Servicio.java` |
| `GET /turnos/disponibilidad/empleado/{empleadoId}/servicio/{servicioId}` es público y devuelve `fecha + horarios` (21 días, slots de 30 min) | `TurnoController`, `TurnoService`, `DisponibilidadDTO`, `SecurityConfig` |
| `POST /turnos/registrar` ya valida solapamiento, día de apertura, horario comercial, rango de fechas y pertenencia del empleado a la sucursal | `TurnoService.registrarTurno` |
| `POST /turnos/registrar` exige rol `CLIENTE` | `TurnoController` (`@PreAuthorize`) |
| El chat existente (`/conversaciones` + WebSocket) es **humano** (cliente ↔ empleado), exige `turnoId` y autenticación; no sirve para un visitante anónimo | `SKILLS.md` §4.9, `chat-widget.ts` |
| La CSP / `Permissions-Policy` actual **deshabilita la geolocalización del navegador**; ya existe loader de Google Maps y autocomplete de Places | `appointmee-front/docs/google-maps.md` |
| Está prohibido guardar secretos en el frontend | `AGENTS.md` §8 (US-GEN-04) |

### Restricciones que condicionan la decisión

- La clave del proveedor de IA **debe vivir en el backend**, nunca en Angular ni en Vercel.
- El frontend es estático: no hay función serverless ni SSR donde ejecutar tools.
- La lógica de disponibilidad y reserva **no se puede duplicar**; ya existe y está probada.
- «Acceso al código» en este contexto significa **acceso a los datos del dominio** a través de los servicios Java, no que el modelo lea el repositorio fuente.

---

## 2. Decisión

**Implementar el asistente con Spring AI dentro de `appointmee-back`, en un feature `features/asistente/`, exponiendo `POST /asistente/mensaje` y definiendo los tools como beans Java que delegan en los servicios existentes.**

### Forma concreta

```
Angular (widget con modo "Asistente IA", sin clave)
   → POST /asistente/mensaje  [JWT cuando haga falta]
        → ChatClient de Spring AI  → proveedor de IA (API key en entorno del back)
        → tools Java:
             buscarSucursales(categoria, zona, lat, lng)
             listarServicios(sucursalId)
             verDisponibilidad(empleadoId, servicioId)
             reservar(fechaHora, empleadoId, servicioId)
```

Reglas de la decisión:

1. **Las tools son methods Java** con `@Tool` / beans `@Description` que llaman a `SucursalService`, `ServicioService` y `TurnoService`. No hay capa HTTP intermedia.
2. **El modelo nunca inventa disponibilidad**: solo elige entre los datos devueltos por las tools. La reserva se confirma antes de escribir.
3. **Se reutiliza `TurnoService.registrarTurno`** tal cual; no se reimplementan sus validaciones.
4. **La API key se agrega como variable de entorno del backend** (mismo patrón que `MP_ACCESS_TOKEN`, `RESEND_API_KEY`), nunca en el repo ni en el front.
5. **El proveedor se abstrae** detrás de la API de Spring AI para poder cambiarlo sin tocar las tools.
6. **Modelo inicial:** uno económico (Gemini Flash / `gpt-4o-mini` o equivalente), con límite de tokens y de tasa por sesión.
7. **Streaming:** opcional en una primera versión; si se habilita, se hace con SSE desde el controller y se consume en Angular con el patrón de estados ya existente.
8. Vercel AI SDK **no se usa en el producto**; queda permitido solo para un prototipo descartable que no reserve turnos reales.

---

## 3. Alternativas consideradas

| Alternativa | Por qué no |
| --- | --- |
| **Vercel AI SDK** en una función de Vercel / servicio Node | Es la mejor DX para equipos TS (`useChat`, `streamText`, gateway de modelos), pero el front es Angular estático: habría que crear un runtime Node nuevo, exponer las tools por HTTP hacia el back, duplicar contratos y repartir secretos entre dos despliegues. Es exactamente el trabajo que se quería evitar. |
| **Desplegar el modelo localmente** en Vercel | Los modelos no se despliegan en Vercel; se llama a una API. Además obligaría a gestionar inferencia, memoria y escalado. Descartado. |
| **n8n / Dify / Flowise** | Útiles para validar prompts en horas, pero añaden otra infraestructura y otra gestión de secretos; las tools terminarían llamando al mismo backend por HTTP. No aportan para el producto final. |
| **MCP + Claude Desktop / OpenCode** | Excelente si la necesidad fuera que la IA **leyera el repositorio** para el equipo de desarrollo. No sirve para clientes finales. Queda como proyecto separado si esa necesidad aparece. |

---

## 4. Consecuencias

### Positivas

- **Cero despliegue nuevo**: se reutiliza el backend ya corriendo.
- Las tools corren **en el mismo proceso** que la BD y el JWT: transacciones, excepciones (`common/exception`) y autorización ya existentes.
- Un único lugar para la lógica de reserva; el modelo no puede eludir las validaciones de `TurnoService`.
- La clave de IA queda bajo las mismas políticas de secretos que las integraciones actuales.
- Los beans de tools son reutilizables desde otro cliente (web, WhatsApp, bot interno) sin rehacer la integración.

### Negativas / costos asumidos

- No existe el equivalente a `useChat`: el estado del chat, SSE y los errores en Angular se implementan a mano (el repo ya tiene patrón de WebSocket/STOMP y estados inline).
- El catálogo de modelos y novedades de Spring AI va más lento que el gateway de Vercel; se acepta a cambio de simplicidad de despliegue.
- Hay que escribir y mantener descripciones de tools, prompt del sistema y guardas contra prompt injection.
- Cada tool que escriba en BD debe ser estrictamente confirmada y estar limitada por tasa.

---

## 5. Plan para la tarea (mañana)

> Crear la rama de implementación desde `origin/develop` con un ID real de Jira (`SCRUM-<id>-asistente-ia-spring-ai`). **No inventar el ID.**

1. **Dependencia y configuración**
   - Agregar el starter de Spring AI del proveedor elegido, en versión compatible con Spring Boot 4.0.5 / Java 21 (confirmar en el release train antes de fijarla).
   - `spring.ai.*.api-key` leída desde variable de entorno. Nada de valores en `application.properties`.
   - Timeout y reintentos acotados; logs sin datos personales.

2. **Feature `features/asistente/`**
   - `dto/`: request con historial + mensaje; response con texto y, si aplica, ids de sucursal/servicio/empleado/turno para que el front renderice tarjetas accionables.
   - `AsistenteService`: prompt del sistema, orquestación del `ChatClient` y registro de herramientas.
   - Tools Delegadas:
     | Tool | Destino |
     | --- | --- |
     | `buscarSucursales(categoria, zona, lat, lng)` | `SucursalService`, filtrando `activo` y ordenando por distancia Haversine sobre `latitud/longitud` |
     | `listarServicios(sucursalId)` | `ServicioService` |
     | `listarEmpleados(sucursalId)` | endpoint `/sucursales/{id}/elegir-empleado` |
     | `verDisponibilidad(empleadoId, servicioId)` | `TurnoService.verDisponibilidad` |
     | `reservar(fechaHora, empleadoId, servicioId)` | `TurnoService.registrarTurno` |
   - `AsistenteController`: `POST /asistente/mensaje`.

3. **Reglas de negocio del ejemplo**
   - «Soy hombre» → mapear a `ECategoria.BARBERIA` (o a un servicio cuyo nombre indique corte masculino). `Servicio` no tiene género: no inventar un campo nuevo sin historia que lo pida.
   - «Cerca de casa» → pedir dirección o barrio (Places autocomplete ya integrado) y calcular distancia en Java. No usar geolocalización del navegador: la CSP la tiene deshabilitada.
   - Confirmación explícita antes de `reservar`.
   - **Pendiente de producto:** `POST /turnos/registrar` exige `CLIENTE`. Decidir si el asistente solo recomienda y envía a login/registro, o si se habilita reserva de invitado. No asumirlo.

4. **Seguridad (US-GEN-04)**
   - Definir explícitamente en `SecurityConfig` si `/asistente/mensaje` es público o autenticado. Si es público: límite de tasa, longitud máxima del mensaje y sin datos de otros usuarios.
   - El tool `reservar` exige el JWT del usuario; el modelo no puede suplantarlo.
   - Sanitizar la salida antes del DOM en el front; usar `ds-*` del sistema de diseño.

5. **Frontend (historia aparte, rama `docs/` propia si es solo documentación)**
   - Modo "Asistente IA" en el widget existente, con estados de carga/error/offline, `aria-live` y bloqueo de doble envío (`AGENTS.md` §5).
   - Tarjetas de sucursal/turno devueltas por la API, no texto libre con datos incrustados.

6. **Verificación**
   - `./mvnw test` y `./mvnw clean package -DskipTests`.
   - Proveedor mockeado en tests (`@MockitoBean`); cubrir tool invocado, tool no invocado, error del proveedor y rechazo de reserva sin confirmación.
   - No se espera migración SQL (no hay entidad nueva) salvo que se decida persistir conversaciones.

---

## 6. Referencias

- `appointmee-back/SKILLS.md` — §2.3 trazabilidad, §3 flujo de ramas, §4.4 integraciones, §4.7 documentación, §4.9 chat.
- `appointmee-front/AGENTS.md` — §5 convenciones Angular, §8 checklist US-GEN-01/US-GEN-04.
- `features/turno/TurnoService.java`, `features/turno/TurnoController.java`, `features/turno/dto/DisponibilidadDTO.java`.
- `features/sucursal/Sucursal.java`, `features/sucursal/categoria/ECategoria.java`.
- `features/servicio/Servicio.java`.
- `config/SecurityConfig.java`.
- `appointmee-front/docs/google-maps.md` (CSP y Places).
- Documentación de Spring AI: Chat Client, Tool Calling y MCP.
