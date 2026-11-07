# 03 - Instalación y ejecución

## Requisitos previos

- JDK 21
- PostgreSQL
- Maven (o `./mvnw`)
- Docker (opcional)

## Instalación

```bash
git clone https://github.com/gonnnzaDev/AppointMee.git
cd AppointMee
```

## Variables de entorno

| Variable | Uso |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuario BD |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña BD |
| `MP_ACCESS_TOKEN` | Credencial Mercado Pago |
| `JWT_SECRET` | Clave firma JWT |

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/appointmee
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=<tu-clave>
export MP_ACCESS_TOKEN=<tu-token>
export JWT_SECRET=<clave-segura>
```

## Ejecutar backend

```bash
./mvnw spring-boot:run
```

Puerto por defecto 8080. Hibernate usa `ddl-auto=validate`; el esquema debe existir.

## Ejecutar frontend

No disponible en este repositorio (Angular en repositorio separado). Consultar su README.

## Pruebas

```bash
./mvnw test
```

## Docker

```bash
docker build -t appointmee-backend .
```
