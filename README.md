<div align="center">

# AppointMee · Backend

**API para gestionar sucursales, servicios y reservas de turnos.**

Construido con Java 21 y Spring Boot. Incluye autenticación JWT, persistencia con PostgreSQL e integración de pagos con Mercado Pago.

</div>

---

## Contenido

- [Qué incluye](#qué-incluye)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Requisitos](#requisitos)
- [Ejecución local](#ejecución-local)
- [Docker](#docker)
- [API y documentación](#api-y-documentación)
- [Configuración](#configuración)
- [Pruebas](#pruebas)
- [Seguridad](#seguridad)
- [Equipo](#equipo)

## Qué incluye

La API concentra la lógica de negocio de AppointMee y organiza sus recursos por dominios: usuarios y roles, sucursales, servicios, turnos, solicitudes, reseñas, imágenes y pagos.

| Dominio | Capacidades principales |
|---|---|
| Usuarios | Registro, inicio de sesión, perfil y administración de cuentas |
| Sucursales | Búsqueda, filtros, gestión, empleados, imágenes y reseñas |
| Servicios | Consulta y administración de servicios por sucursal |
| Turnos | Disponibilidad, creación, consulta, cancelación y finalización |
| Solicitudes | Solicitudes de empleador y empleado, aprobación y rechazo |
| Pagos | Integración con Mercado Pago y recepción de webhooks |

> El acceso y los permisos efectivos dependen de la configuración de seguridad y del rol del usuario.

## Tecnologías

- **Java 21** · **Spring Boot 4.0.5**
- Spring Web MVC, Spring Security y Spring Data JPA
- **PostgreSQL** como base de datos principal; H2 también figura como dependencia
- JWT con JJWT
- Mercado Pago Java SDK
- Springdoc OpenAPI / Swagger UI
- Maven y Docker

## Arquitectura

El código sigue una organización por responsabilidades: **Controllers → Services → Repositories**, con DTOs para las entradas y respuestas, modelos de dominio, configuración de seguridad y manejo global de excepciones.

```text
src/main/java/com/gg/turnlook/Backend/
├── Controllers/   # Endpoints HTTP
├── DTO/           # Contratos de entrada y salida
├── Model/         # Entidades de dominio
├── Repository/    # Persistencia con Spring Data JPA
├── Service/       # Lógica de negocio
├── Security/      # JWT y carga de usuario
├── config/        # Configuración de Spring Security
├── Enum/          # Roles, categorías y estados
└── Excepciones/   # Errores y respuesta global
```

## Requisitos

- JDK **21**
- PostgreSQL accesible desde el entorno
- Docker (opcional)
- Credenciales de Mercado Pago para habilitar operaciones de pago

## Ejecución local

1. Cloná el repositorio y entrá al proyecto:

   ```bash
   git clone https://github.com/gonnnzaDev/AppointMee.git
   cd AppointMee
   ```

2. Configurá las variables de entorno requeridas (ver [Configuración](#configuración)). La aplicación espera que PostgreSQL ya esté creado y que el esquema sea compatible: Hibernate está configurado con `ddl-auto=validate`.

3. Iniciá el servidor:

   ```bash
   ./mvnw spring-boot:run
   ```

La aplicación escucha en el puerto **8080** por defecto, salvo configuración externa.

## Docker

El `Dockerfile` compila el proyecto con Maven sobre JDK 21 y ejecuta el JAR sobre un runtime Java 21. Construí la imagen desde la raíz:

```bash
docker build -t appointmee-backend .
```

Al ejecutar el contenedor, proporcioná las variables de entorno descritas abajo y publicá el puerto 8080 según tu entorno. La base PostgreSQL debe ser alcanzable desde el contenedor.

## API y documentación

Springdoc está incluido en el proyecto. Con la aplicación en marcha, revisá:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Especificación OpenAPI: `http://localhost:8080/v3/api-docs`

Los controladores se encuentran en `src/main/java/com/gg/turnlook/Backend/Controllers`. La API implementa rutas agrupadas bajo `/usuarios`, `/sucursales`, `/servicios`, `/turnos`, `/solicitudes-empleado`, `/solicitudes-empleador` y `/pagos`.

## Configuración

`src/main/resources/application.properties` referencia estas variables; no guardes valores reales en el repositorio:

| Variable | Uso |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuario de base de datos |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de base de datos |
| `MP_ACCESS_TOKEN` | Credencial privada de Mercado Pago |
| `JWT_SECRET` | Clave para firmar tokens JWT |
| `CLOUDINARY_CLOUD_NAME` | Nombre del cloud de Cloudinary |
| `CLOUDINARY_API_KEY` | API key de Cloudinary |
| `CLOUDINARY_API_SECRET` | API secret de Cloudinary |
| `RESEND_API_KEY` | Credencial del proveedor de correo |
| `RESEND_FROM` | Remitente del correo |

Ejemplo de formato local (reemplazá los valores por credenciales propias):

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/appointmee
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=<tu-clave-local>
export MP_ACCESS_TOKEN=<tu-token-de-prueba>
export JWT_SECRET=<clave-segura-local>
```

No reutilices credenciales de producción para desarrollo. Las cuentas de prueba deben crearse en un entorno controlado; no se publican contraseñas de cuentas en este README.


### Base de datos e imagenes

Hibernate esta configurado con ddl-auto=validate, por lo que el esquema debe ser compatible antes de iniciar. Ejecuta una sola vez los scripts de src/main/resources/db/ (incluyendo imagenes_cloudinary.sql) sobre PostgreSQL. Cloudinary es el unico proveedor de imagenes: define CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET antes de arrancar; sin ellas la aplicacion no inicia.

## Pruebas

Ejecutá la suite Maven con:

```bash
./mvnw test
```

El Dockerfile omite las pruebas durante el empaquetado de imagen (`-DskipTests`); en CI conviene ejecutar `./mvnw test` como paso separado antes de construir/publicar.

## Seguridad

- Protegé las credenciales de base de datos, Mercado Pago y JWT mediante el gestor de secretos del entorno de despliegue.
- Usá una clave JWT fuerte y exclusiva por ambiente.
- Habilitá HTTPS en cualquier entorno accesible públicamente.
- Restringí los orígenes CORS y permisos según los dominios reales de la aplicación.
- Rotá de inmediato cualquier credencial que haya sido compartida o comprometida.

## Equipo

- **Backend:** Giovanni Morro Bai
- **Frontend:** Gonzalo Leonel Lopez

---

**AppointMee** · Proyecto final de Programación III · Universidad Tecnológica Nacional
