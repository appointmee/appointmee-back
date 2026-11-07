# 01 - Introducción

## Qué es AppointMee

AppointMee es una aplicación para gestionar sucursales, servicios y reservas de turnos. Permite a clientes buscar sucursales, reservar turnos y dejar reseñas; a empleadores administrar sucursales, empleados y servicios; y a administradores gestionar usuarios y solicitudes.

## Problema que resuelve

Centraliza la reserva y administración de turnos entre clientes, empleados y empleadores, evitando coordinación manual y mejorando la disponibilidad.

## Tipos de usuarios

- **Administrador**: gestiona usuarios, roles y solicitudes.
- **Empleador**: administra sucursales, servicios y empleados.
- **Empleado**: atiende turnos asignados.
- **Cliente**: busca sucursales, reserva y cancela turnos, deja reseñas.

## Funcionalidades principales implementadas

- Registro, inicio de sesión y autenticación JWT.
- Gestión de usuarios y roles.
- Administración de sucursales, empleados y servicios.
- Búsqueda y filtrado de sucursales.
- Reserva, cancelación y finalización de turnos.
- Solicitudes de empleados y empleadores.
- Pagos con Mercado Pago.
- Reseñas, imágenes y perfiles.

## Tecnologías

- Frontend: Angular (repositorio separado, no verificado aquí).
- Backend: Java 21, Spring Boot 4.0.5, Spring Security, JPA, JWT (JJWT).
- Base de datos: PostgreSQL (H2 como dependencia).
- Integraciones: Mercado Pago, Cloudinary (no verificado en código backend), WhatsApp (no verificado).

## Estado

Funcionalidades listadas corresponden al código backend actual. Funcionalidades pendientes o en desarrollo no se documentan como terminadas; no se identificaron módulos claramente incompletos en el backend revisado.
