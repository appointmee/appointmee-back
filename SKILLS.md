# SKILLS.md — AppointMee Back

Guía operativa para agentes de IA que trabajan en **AppointMee Back**.
Es el equivalente backend de `AGENTS.md` (que solo existe en el repo del front): mismas
reglas de rama, commits, trazabilidad con Jira y calidad.

> Última revisión: 2026-11-24, sobre `origin/develop` (`c2c6390`).

---

## 0. Regla de oro

Antes de tocar código:

1. `git status --short` + `git branch --show-current` + `git log --oneline -5`.
2. Si hay cambios ajenos sin commitear (por ejemplo `mvnw`, `pom.xml`,
   `application.properties` o `pom.xml.save`), **no mezclarlos**: usar un `git worktree`
   aparte o preguntar.
3. Nunca trabajar sobre `main` ni `develop`.
4. Crear la rama desde `origin/develop` **antes** de editar.
5. Identificar la historia de Jira (`SCRUM-<id>`). No inventar IDs.

---

## 1. Contexto real del repositorio

| Ítem | Valor |
|---|---|
| Runtime | **Java 21** |
| Framework | **Spring Boot 4.0.5** (Web MVC, Security, Data JPA, Validation) |
| Build | **Maven** con wrapper (`./mvnw`) |
| Base de datos | **PostgreSQL** en runtime; **H2** en tests |
| Esquema | `spring.jpa.hibernate.ddl-auto=validate` → el esquema debe existir y coincidir |
| Auth | JWT access + refresh, filtro propio, sesiones stateless |
| Integraciones | Mercado Pago, Cloudinary, Resend (email), WhatsApp gateway |
| Docs API | Springdoc/OpenAPI + Swagger UI |
| Deploy | Docker multi-stage (JDK 21 → JRE 21), puerto **8080** |
| Tests | JUnit 5 + Mockito + MockMvc + `@DataJpaTest` con H2 |

### Estructura real (no la del README)

```text
src/main/java/com/gg/turnlook/
├── TurnlookApplication.java     # @SpringBootApplication + @EnableScheduling
├── auth/                        # login, refresh, JWT, password reset, handlers
├── common/                      # excepciones globales, utilidades
├── config/                      # SecurityConfig (CORS, filtros, reglas por ruta)
├── features/
│   ├── usuario/                 # usuarios + rol/ERol (ADMINISTRADOR, EMPLEADOR, EMPLEADO, CLIENTE)
│   ├── sucursal/                # sucursales + categoria/ECategoria
│   ├── servicio/
│   ├── turno/                   # EstadoTurno, disponibilidad, cancelación automática
│   ├── solicitud/               # empleado/ y empleador/ (EstadoSolicitud)
│   ├── resenia/
│   ├── pago/                    # Mercado Pago + webhook
│   ├── imagen/                  # Cloudinary
│   ├── verificacion/            # email/OTP
│   ├── estadisticas/            # /graficos
│   └── observabilidad/          # reporte 404 y métricas
└── web/                         # HomeController

src/main/resources/
├── application.properties       # variables de entorno, sin secretos versionados
├── static/                      # front legado servido por el back (*-folder, recursos)
└── db/                          # scripts SQL de migración manual
    ├── auth_tokens.sql
    ├── email_verificacion.sql
    ├── imagenes_cloudinary.sql
    ├── optimizacion_consultas.sql
    ├── sucursal_dias_apertura.sql
    └── turno_fecha_modificacion_estado.sql

src/test/java/com/gg/turnlook/   # tests por feature
src/test/js/                     # test JS auxiliar (formulario de imágenes)
```

> El `README.md` describe una estructura vieja (`com/gg/turnlook/Backend` con
> `Controllers/`, `DTO/`, `Model/`). El código real usa **arquitectura por feature**.
> La fuente de verdad es el árbol de `src/main/java`.

### API agrupada por prefijo

| Prefijo | Dominio |
|---|---|
| `/auth` | login, register, refresh, forgot/reset password, verificación |
| `/usuarios` | perfil, `/me`, admin, activar, cambio de email, borrar cuenta |
| `/sucursales` | listado, filtros, categorías, detalle, empleados, reseñas, imágenes |
| `/servicios` | listado por sucursal, detalle, alta/edición/eliminación |
| `/turnos` | disponibilidad, crear, propios, de sucursal, finalizar, cancelar |
| `/solicitudes-empleado` | invitaciones de empleo (empleador → empleado) |
| `/solicitudes-empleador` | solicitudes para ser empleador |
| `/pagos` | checkout y webhook de Mercado Pago |
| `/imagenes` | upload y gestión Cloudinary |
| `/graficos` | estadísticas administrador/empleador |
| `/verificaciones` | códigos de verificación |
| `/observabilidad` | reporte de 404 |

---

## 2. Jira: proyecto `SCRUM`

Sitio: `appointmee.atlassian.net` · Proyecto: **SCRUM** · Board: **SCRUM**.

### 2.1 Tipos y estados

| Tipos de issue | Estados |
|---|---|
| `Epic`, `Story`, `Task`, `Fix`, `Subtask` | `Por hacer` → `En curso` → `En revisión` → `Finalizado` |

### 2.2 Formato de las historias

```text
US-<ÁREA>-<NN> · <Módulo> · <Nuevo|Mejora|Fix> · Rol: <Cliente|Empleado|Empleador|Administrador>

### Historia
Como <rol>, quiero <objetivo> para <beneficio>.

### Objetivo / demo
* ...

### Criterios de aceptación
* ...

### Dependencias
* ...
```

En backend, cada criterio suele traducirse en:

- contrato de endpoint (request/response/errores),
- validación y autorización por rol,
- persistencia y migración SQL si cambia el esquema,
- tests de servicio/repositorio y, si aplica, integración HTTP.

### 2.3 Trazabilidad Jira ↔ Git

- Rama: `SCRUM-<id>-<slug-kebab>` (ej. `SCRUM-307-consulta-sucursal-activo`).
- Commit: referenciar `SCRUM-<id>` cuando aporte.
- PR contra **`develop`**; `main` solo por release.
- Al mergear, mover el issue a `Finalizado`.

### 2.4 Contexto de sprint

Al cierre de esta revisión el sprint activo es **SCRUM Sprint 4** (24/11 → 08/12), con foco
en pasarela de pagos propia, mapa/búsqueda por cercanía, notificaciones in-app y chat.
Verificar el sprint real antes de planificar.

---

## 3. Git: flujo obligatorio

```bash
git fetch origin develop
git switch -c SCRUM-<id>-<slug> origin/develop
# ... implementar ...
git fetch origin develop && git rebase origin/develop
git push -u origin HEAD
gh pr create --base develop --head SCRUM-<id>-<slug> \
  --title "tipo(scope): resumen" --body-file /tmp/pr-body.md
gh pr view <n> --json number,title,baseRefName,headRefName,state,url,commits
```

Reglas:

- Base del PR: **`develop`**. Nunca `main`.
- No force-push sobre ramas compartidas sin pedido explícito.
- No commitear `.env`, `target/`, logs, IDE, secretos ni `pom.xml.save`.
- Un commit = una funcionalidad coherente.
- Para documentación con working tree sucio: `git worktree add` + rama `docs/<slug>`.
- `main` solo recibe releases desde `develop`.

### Convención de commits

El historial mezcla estilos; para cambios nuevos usar **Conventional Commits en español**:

```text
feat(turnos): agregar disponibilidad por profesional
fix(sucursales): excluir inactivas en listados operativos
test(turnos): cubrir cancelación automática
refactor(backend): mover el proyecto a la raíz
docs: documentar variables de entorno
chore(deps): actualizar Spring Boot
```

---

## 4. Skills (procedimientos reutilizables)

### 4.1 Agregar un endpoint

1. Identificar el feature en `features/<dominio>/`.
2. Crear/ajustar DTO de request y response (records o clases existentes).
3. Implementar en `Service` la regla de negocio y validaciones.
4. Persistir con el `Repository` (Spring Data JPA) y `@Transactional` cuando corresponda.
5. Exponer en `Controller` con el prefijo del dominio y verbos REST consistentes.
6. Autorizar con `@PreAuthorize` o reglas en `SecurityConfig`; si es público, agregarlo a la
   lista de `permitAll` y justificarlo.
7. Mapear errores con las excepciones de `common/exception` (`NotFoundException`,
   `BadRequestException`, `ConflictException`, `ForbiddenException`, `UnauthorizedException`).
8. Documentar en Springdoc si el contrato no es obvio.
9. Tests: servicio + controller/integración según el riesgo.
10. Verificar que el front no rompa: revisar `appointmee-front/src/app/models` y servicios.

### 4.2 Cambiar el esquema de base de datos

`ddl-auto=validate` **no crea ni altera tablas**. Pasos:

1. Escribir un script SQL idempotente en `src/main/resources/db/`.
2. Nombrarlo por tema (`<tema>.sql`), con comentarios de propósito.
3. Ejecutarlo manualmente una vez sobre PostgreSQL (local/staging/prod según el caso).
4. Ajustar entidades JPA para que `validate` pase.
5. Si el cambio es destructivo, documentar plan de rollback en el PR.

### 4.3 Permisos y roles

- Roles: `ADMINISTRADOR`, `EMPLEADOR`, `EMPLEADO`, `CLIENTE` (`features/usuario/rol/ERol`).
- `SecurityConfig` define rutas públicas y `anyRequest().authenticated()`.
- En controladores usar `@PreAuthorize("hasRole('...')")` o `hasAnyRole(...)`.
- Regla: el frontend es UX; **el backend autoriza siempre** por sí mismo.
- Al agregar una ruta pública, revisar que no exponga datos de otro usuario/sucursal.

### 4.4 Integraciones externas

| Integración | Consideraciones |
|---|---|
| **Mercado Pago** | Token por entorno; webhook público (`/pagos/webhook`); validar estado antes de confirmar turno; nunca confiar en el cliente. |
| **Cloudinary** | Requerido al arrancar (sin credenciales la app no inicia); límites de upload 5 MB / 6 MB request. |
| **Resend** | Verificación de email y reset de contraseña; no loggear códigos ni tokens. |
| **WhatsApp gateway** | URL + API key por entorno. |

Checklist de integración nueva (US-GEN-04):

- [ ] Sin secretos en el repo; todo por variables de entorno.
- [ ] Validar entrada y no exponer detalles internos en errores.
- [ ] Autorización explícita y límites de tasa/abuso cuando aplique.
- [ ] Manejo de timeouts/reintentos y logs sin datos sensibles.
- [ ] Tests con el proveedor mockeado (`@MockitoBean`).

### 4.5 Tests (JUnit 5)

Patrones presentes en el repo:

- **`@DataJpaTest` + H2** (`create-drop`) para repositorios y consultas:
  `SucursalBusquedaTest`.
- **`@SpringBootTest(webEnvironment = RANDOM_PORT)` + H2** para flujos de autenticación:
  `AuthIntegrationTest`.
- **MockMvc** para controllers: `ImagenControllerTest`, `ObservabilidadControllerTest`,
  `MercadoPagoControllerTest`.
- **Mockito** (`@MockitoBean` / mocks) para servicios externos.
- Test JS auxiliar: `src/test/js/imagen-formulario.test.cjs`.

Reglas:

- Cubrir reglas de negocio, permisos, errores y casos borde; no solo happy path.
- Usar H2 en tests, pero cuidado con SQL específico de PostgreSQL (funciones, ILIKE,
  constraints): si el test depende de ello, usar una consulta compatible o documentarlo.
- Restaurar mocks y no depender de servicios externos reales.
- No hay JaCoCo configurado actualmente: no afirmar cobertura medida.

### 4.6 Build, ejecución y Docker

```bash
# Ejecutar en local (requiere PostgreSQL y variables)
./mvnw spring-boot:run

# Suite de tests
./mvnw test

# Test puntual
./mvnw -Dtest=SucursalBusquedaTest test

# Empaquetar (salta tests, como el Dockerfile)
./mvnw clean package -DskipTests

# Imagen Docker
docker build -t appointmee-backend .
docker run --rm -p 8080:8080 --env-file .env appointmee-backend
```

Swagger local: `http://localhost:8080/swagger-ui/index.html` y
`http://localhost:8080/v3/api-docs`.

### 4.7 Documentación

- Actualizar `README.md` cuando cambien stack, comandos, variables o estructura.
- Mantener `AUTH.md` y `EMAIL-VERIFICACION.md` al día si cambian los flujos.
- Documentar contratos nuevos y notas de migración en el PR.
- Rama `docs/<slug>`, commit `docs: ...`, PR a `develop`.

### 4.8 Cierre y PR

1. `git diff` y `git status` sin archivos ajenos.
2. `./mvnw test` en verde.
3. `./mvnw clean package -DskipTests` compila.
4. Rebase sobre `origin/develop`.
5. Push + PR a `develop` con: qué cambió, endpoints/migraciones, cómo se probó, riesgos.
6. Verificar PR (`baseRefName=develop`) y dejar issue en `En revisión`.

---

## 5. Seguridad

- JWT stateless con access + refresh; `SecurityConfig` deshabilita CSRF y sesiones.
- CORS restringido a orígenes de frontend conocidos (localhost 4200/3000, Railway, Render).
- `Authorization: Bearer` validado por `JwtFilter`.
- No loggear tokens, códigos OTP, contraseñas ni datos personales sensibles.
- No exponer stacktraces: `GlobalExceptionHandler` devuelve mensajes controlados.
- Credenciales solo por variables de entorno; rotar cualquier secreto filtrado.
- Revisar que los endpoints públicos de lectura no permitan enumerar datos privados.

Variables principales (nombres exactos en `application.properties`):

```text
SPRING_DATASOURCE_URL / USERNAME / PASSWORD
JWT_SECRET / JWT_ACCESS_TTL_SECONDS
AUTH_REFRESH_TTL_SECONDS / AUTH_PASSWORD_RESET_TTL_SECONDS / AUTH_PASSWORD_RESET_URL
MP_ACCESS_TOKEN
CLOUDINARY_CLOUD_NAME / API_KEY / API_SECRET
RESEND_API_KEY / RESEND_FROM
WHATSAPP_GATEWAY_URL / WHATSAPP_GATEWAY_API_KEY
```

> `.env` existe en local y está ignorado por git. Nunca copiar su contenido a issues,
> PRs, logs ni documentación.

---

## 6. Definition of Done

- [ ] Rama desde `origin/develop`, sin historias ajenas.
- [ ] Criterios de aceptación cumplidos.
- [ ] Contrato de API claro (request/response/errores) y autorización por rol.
- [ ] Migración SQL en `src/main/resources/db/` si cambió el esquema.
- [ ] Tests nuevos/actualizados y `./mvnw test` en verde.
- [ ] `./mvnw clean package -DskipTests` compila.
- [ ] Commits separados por funcionalidad.
- [ ] PR contra `develop` con qué cambió, cómo se probó y riesgos.
- [ ] Issue de Jira actualizado.
- [ ] Working tree limpio o pendientes informados.

---

## 7. Trampas conocidas

| Tema | Detalle |
|---|---|
| README desfasado | Menciona `com.gg.turnlook.Backend` y carpetas `Controllers/`, `DTO/`, `Model/`. El código real está en `com.gg.turnlook` por feature. |
| Front legado | `src/main/resources/static/*-folder` y `recursos/` son un front viejo servido por el backend. No confundirlo ni modificarlo sin pedido; el front vigente es el repo Angular. |
| `ddl-auto=validate` | La app **no** crea tablas. Sin los scripts de `db/` ejecutados, no arranca. |
| Cloudinary obligatorio | Sin `CLOUDINARY_*` la aplicación no inicia (según README). |
| `pom.xml.save` | Archivo local sin trackear; no commitearlo. |
| `mvnw`, `pom.xml`, `application.properties` modificados | Suelen ser ajustes locales de entorno; revisar el diff antes de incluir nada. |
| H2 vs PostgreSQL | Los tests corren en H2; consultas con funciones propias de PostgreSQL pueden pasar en prod y fallar en tests (o al revés). |
| Webhook Mercado Pago | Es público; validar firma/estado y no confiar en parámetros del cliente. |
| CORS | Si cambia el dominio del front, actualizar `SecurityConfig` y `vercel.json` (CSP/connect-src) en conjunto. |
| Versiones milestone | Hay dependencias en milestone (p. ej. `spring-boot-starter-mail 4.2.0-M2`); revisar compatibilidad al actualizar. |
| Cobertura | No hay JaCoCo configurado; no inventar porcentajes. |
