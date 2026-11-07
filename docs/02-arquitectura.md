# 02 - Arquitectura

## Arquitectura general

Backend Spring Boot con capas: Controllers → Services → Repositories. DTOs para contratos, modelos de dominio (entidades JPA), seguridad JWT y manejo global de excepciones.

## Comunicación

- Angular (frontend) consume API REST del backend.
- Backend persiste en PostgreSQL.
- Integraciones externas: Mercado Pago (pagos/webhook), Cloudinary y WhatsApp (no verificados en este repositorio).

## Organización de carpetas

```
src/main/java/com/gg/turnlook/Backend/
├── Controllers/
├── DTO/
├── Model/
├── Repository/
├── Service/
├── Security/
├── config/
├── Enum/
└── Excepciones/
```

## Diagramas C4

No se encontraron diagramas C4 en este repositorio. Referenciarlos desde el repositorio/documentación del equipo cuando estén disponibles.
