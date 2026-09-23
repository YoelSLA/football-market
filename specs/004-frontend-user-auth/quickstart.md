# Validación rápida

## Prerrequisitos

- Node.js `>=24` y dependencias instaladas en `frontend/`.
- Backend de autenticación disponible con los endpoints descritos en [contracts/auth.http.md](./contracts/auth.http.md).
- Configuración de la URL base del cliente HTTP existente.

## Ejecución

1. Iniciar el backend según su configuración local.
2. Ejecutar `npm run dev` desde `frontend/`.
3. Abrir `/register` y enviar email válido, contraseña de al menos 8 caracteres y confirmación coincidente. Esperar `/login` con confirmación y sin acceso a `/home`.
4. Repetir con email duplicado y verificar que se conserva el formulario y se muestra el mensaje del servicio.
5. Abrir `/login`, iniciar sesión con credenciales válidas y verificar llegada a `/home`.
6. Recargar `/home`; verificar que la aplicación consulta `/me` antes de mostrar contenido privado.
7. Intentar `/home` sin token, con JWT expirado y con `/me` en `401`; verificar redirección a `/login`.
8. Con `/me` temporalmente inaccesible, verificar que no se muestra `/home`, se informa el problema y se puede reintentar.
9. Cerrar sesión desde `/home`; verificar redirección a `/login` y bloqueo de `/home` tras recargar.
10. Revisar `/login` y `/register` en escritorio y pantalla estrecha, incluidos loading, errores y estado deshabilitado durante el envío.

## Controles del proyecto

El build frontend permitido es `npm run build` desde `frontend/`. No se crean ni ejecutan tests de frontend.
