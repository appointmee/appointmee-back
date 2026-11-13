# Flujo de verificación de cuenta

## Acceso y despliegue

Actualizar frontend y backend juntos. No requiere SQL nuevo si ya se aplicaron auth_tokens.sql y email_verificacion.sql. No se modifica automáticamente ninguna cuenta existente ni se desmarca un email ya verificado.

1. Login con contraseña incorrecta, usuario inexistente o desactivado: 401 genérico.
2. Login con contraseña correcta y email pendiente: 403 EMAIL_NOT_VERIFIED con verificationToken y verificationExpiresIn=900.
3. Angular guarda el email y ese permiso limitado en sessionStorage. No guarda contraseña ni código OTP, ni los coloca en URLs.
4. `/configuracion/verificar-email` tiene verificationGuard: exige ese contexto y lo valida con el backend. Escribir la URL o un email en sessionStorage no basta.

El permiso está firmado, vence a los 15 minutos y se vincula a ID, email y versión de credenciales. No sirve como access token ni refresh. Cambiar contraseña o email o desactivar la cuenta invalida su uso.

## Contrato

Todos estos endpoints reciben `X-Verification-Token` con el permiso del login. No requieren una sesión completa porque aún se está verificando la cuenta.

- POST /auth/verification-status, cuerpo {"email":"..."}: verificada, enviado, enviadoAhora, retryAfterSeconds, attemptsRemaining, expiresInSeconds.
- POST /auth/send-verification, cuerpo {"email":"...","reenviar":false}: conserva un código vigente. Con reenviar=true pide otro si el servidor permite el reenvío. Devuelve 202 y el estado real; enviadoAhora=false significa que NO se emitió un código nuevo. Un fallo HTTP de Resend devuelve 503 y revierte la generación, conservando el código anterior.
- POST /auth/verify-email, cuerpo {"email":"...","codigo":"123456"}: confirma y crea sesión en una transacción. Devuelve message, token/accessToken, refreshToken, expiresIn y usuario. Si falla crear la sesión, no quedan confirmación ni consumo de código parcialmente guardados.
- POST /auth/complete-verification, cuerpo {"email":"..."}: recupera la sesión si el servidor ya confirmó el email pero se perdió la respuesta. Exige el permiso previo del login y que la cuenta esté realmente verificada; no reutiliza el OTP.

Los errores de código responden con VERIFICATION_CODE_INVALID, VERIFICATION_CODE_EXPIRED o VERIFICATION_ATTEMPTS_EXCEEDED, attemptsRemaining y retryAfterSeconds. Una cuenta ya confirmada devuelve 409 EMAIL_ALREADY_VERIFIED. No se incluye el código en mensajes ni URLs.

## Correcciones de estado y reenvío

El primer reenvío tiene espera de 15 segundos; los siguientes, 60. La base conserva el historial: volver al login no reinicia el reloj del servidor. Angular muestra los segundos informados por el backend, no un contador independiente. Recargar conserva el código vigente; solo un reenvío efectivo reemplaza el anterior. Después de reenviar usar el último correo.

La confirmación y la emisión de sesión son atómicas. El cliente valida la respuesta completa antes de guardar tokens o marcar autenticación. Ante una respuesta perdida o incompleta consulta el estado real y recupera la sesión si ya se confirmó. Cancelar el flujo evita que respuestas tardías revivan una sesión en el navegador.

El registro sigue intentando enviar el primer código. El envío automático de la pantalla solo hace falta si no hay un código vigente y la espera del servidor lo permite. Un 202 no garantiza entrega final del proveedor, pero enviadoAhora distingue generación efectiva de cooldown. No se ignoran los fallos HTTP del proveedor durante los reenvíos autorizados.

## Pruebas

La colección Postman actualizada captura verificationToken del login pendiente y lo envía en los endpoints de verificación. Las pruebas de integración usan H2 y correo simulado; incluyen acceso sin login, permiso manipulado/vencido, rechazo de otros tipos de token, límites de reenvío, conservación del código al recargar o al fallar el proveedor, confirmación con respuesta perdida, intentos reales y reversión si falla crear sesión.
