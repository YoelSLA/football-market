# Quickstart: Validation Guide

## Prerrequisitos

- Java 21 instalado.
- PostgreSQL en ejecución.
- Configuración existente del proyecto disponible para el perfil correspondiente.
- Colección de Postman existente ubicada en `postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json`.

## Validación

La funcionalidad se validará utilizando la colección de Postman existente, incorporando las solicitudes correspondientes a registro y autenticación.

### Pasos

1. Ejecutar el backend utilizando el perfil de desarrollo:

   ```bash
   cd backend
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

2. Abrir la colección existente de Postman:

   `postman/collections/34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json`

3. Configurar la variable `baseUrl` con la dirección del backend, por ejemplo:

   ```text
   http://localhost:8080
   ```

4. Ejecutar la solicitud `register` ubicada dentro de la carpeta `auth`.

5. Verificar que el registro responda de acuerdo con el contrato definido para el endpoint `POST /api/auth/register`.

6. Ejecutar la solicitud `login` ubicada dentro de la carpeta `auth`.

7. Verificar que la autenticación responda de acuerdo con el contrato definido para el endpoint `POST /api/auth/login`.

8. Obtener el JWT devuelto por el login para su posterior utilización mediante autenticación Bearer cuando corresponda.

## Validación de Pruebas

La implementación deberá validarse también mediante las pruebas automatizadas definidas en el plan, incluyendo las pruebas correspondientes a Controller, Service e integración cuando sean aplicables.

Toda nueva clase de test deberá utilizar explícitamente:

```java
@ActiveProfiles("test")
```

## Restricciones

- No modificar los archivos YAML existentes del proyecto.
- No crear una colección nueva de Postman.
- No incorporar endpoints adicionales a los definidos en la especificación.
- La validación deberá limitarse a los casos de uso definidos en `spec.md`.