# Implementation Plan: Current Authenticated User

**Branch**: `003-current-user` | **Date**: 2026-09-20 | **Spec**: `specs/003-current-user/spec.md`

**Input**: Especificación de la funcionalidad ubicada en `specs/003-current-user/spec.md`.

## Summary

Incorporar `GET /api/auth/me` como operación protegida de autenticación. Spring Security continuará validando firma y vigencia del JWT y, durante la conversión del JWT en una autenticación, comprobará mediante AuthenticationService que el `sub` corresponde a un usuario persistido. El Controller obtendrá el `sub` del principal ya autenticado y delegará en el mismo Service la recuperación del usuario actual. El resultado se mapeará a un Response DTO con exactamente `id` y `email`. La ausencia del usuario se convertirá en un fallo de autenticación atendido por el entry point de seguridad con `401 Unauthorized` sin cuerpo.

El cambio estrechará la regla pública actual de `/api/auth/**` para dejar públicos únicamente registro y login, sin modificar sus contratos. No requiere migraciones, nuevos claims, cambios en la duración del JWT ni dependencias adicionales.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.1.1, Spring Web, Spring Security, OAuth2 Resource Server, Spring Data JPA y SpringDoc OpenAPI.

**Storage**: PostgreSQL 18.x mediante la tabla `users` y el Repository existentes; no se requieren cambios de esquema.

**Testing**: JUnit Jupiter, Spring Boot Test, MockMvc, Spring REST Docs, AssertJ, Mockito y PostgreSQL con Testcontainers.

**Target Platform**: Servicio web backend sobre JVM.

**Project Type**: Backend de aplicación web.

**Performance Goals**: Validación persistida de la identidad durante la autenticación y recuperación del usuario para construir la respuesta; sin objetivos adicionales de latencia o throughput definidos por la spec. No se incorpora caché porque debe observarse el estado persistido actual.

**Constraints**: Respuesta exitosa limitada a `id` y `email`; todos los `401` de `/me` sin cuerpo; fuente de verdad persistida; registro y login sin cambios; autenticación stateless; sin ampliar el JWT ni agregar logout, revocación o refresh.

**Scale/Scope**: Un endpoint de lectura, un usuario persistido por solicitud y actualización acotada de seguridad, contrato, pruebas y documentación.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Arquitectura backend**: PASS — Controller maneja HTTP y delega; AuthenticationService resuelve el caso de uso; UserRepository realiza la lectura; UserMapper transforma Model a DTO.
- **Dependencias**: PASS — El flujo es Controller → Service → Repository y Controller → Mapper; Service y Repository no dependen de DTO.
- **Service**: PASS — Se amplía la interfaz y su implementación existentes porque la consulta pertenece a la sesión autenticada; no se crea una capa u Orchestrator innecesarios.
- **DTO y Mapper**: PASS — La respuesta será un `record`; el Mapper será estático, sin estado y limitado a `User` → Response DTO.
- **Seguridad**: PASS — Se reutiliza OAuth2 Resource Server y el JWT vigente. Un converter de autenticación valida que el sujeto corresponde a un usuario persistido antes de crear la autenticación; `/me` queda autenticado y register/login permanecen públicos mediante matchers exactos.
- **Errores**: PASS — La identidad ausente o sin usuario se convierte dentro de Security en un fallo de autenticación y llega al entry point del Resource Server, que responde `401` vacío. No pasa por `GlobalExceptionHandler`, por lo que no constituye una respuesta de error gestionada por la aplicación y no contradice el contrato obligatorio de `ErrorResponseDTO`.
- **Persistencia**: PASS — Se reutiliza `findByEmail`; no hay query propia compleja ni migración que justifique tests de Repository.
- **Testing**: PASS — Habrá tests de Service con persistencia real, Controller con seguridad real y Service mock, Security para JWT, y un E2E del journey crítico.
- **Contratos y documentación**: PASS — Se actualizarán OpenAPI, Spring REST Docs, Asciidoctor y la colección Postman existente.
- **Idioma y convenciones**: PASS — Documentación en español e identificadores de código en inglés; Javadoc aplicable y uso de `this` conforme a la Constitución.
- **Alcance**: PASS — No se altera el contenido ni duración del JWT, no se agregan dependencias y no se refactorizan áreas ajenas.

**Resultado previo al diseño**: PASS — No existen violaciones ni aclaraciones técnicas pendientes.

**Reevaluación posterior al diseño**: PASS — El contrato, modelo de datos y guía de validación mantienen las mismas fronteras; no surgieron dependencias, migraciones ni excepciones arquitectónicas adicionales.

## Project Structure

### Documentation (this feature)

```text
specs/003-current-user/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── api.md
└── tasks.md              # Se generará con $speckit-tasks
```

### Source Code (repository root)

```text
backend/
├── src/main/java/footballmarket/
│   ├── config/SecurityConfig.java
│   ├── controllers/
│   │   ├── AuthenticationController.java
│   │   ├── dtos/responses/CurrentUserResponseDTO.java
│   │   └── mappers/UserMapper.java
│   ├── models/User.java
│   ├── repositories/UserRepository.java
│   ├── security/PersistedUserJwtAuthenticationConverter.java
│   ├── services/AuthenticationService.java
│   ├── services/impl/AuthenticationServiceImpl.java
│   └── services/exceptions/CurrentUserNotFoundException.java
├── src/test/java/footballmarket/
│   ├── controllers/AuthenticationControllerTest.java
│   ├── services/AuthenticationServiceTest.java
│   ├── security/JwtAuthenticationIntegrationTest.java
│   └── e2e/AuthenticationJourneyE2ETest.java
└── src/docs/asciidoc/index.adoc

postman/collections/
└── 34427701-ccc98ca8-26b8-4486-b6b4-d25e2ca21d44.json
```

**Structure Decision**: La operación pertenece al Controller y Service de autenticación existentes. Se agrega únicamente el contrato de salida y el error específico necesarios. No se crea un Service, Repository, Model, Orchestrator ni migración nuevos.

## Design Decisions

### Flujo exitoso

1. Spring Security valida criptográficamente el Bearer JWT.
2. PersistedUserJwtAuthenticationConverter usa AuthenticationService para comprobar que el `sub` identifica a un User persistido antes de crear la autenticación.
3. El Controller recibe el principal validado y usa únicamente su `sub` como clave de identidad.
4. AuthenticationService recupera el `User` persistido actual.
5. UserMapper genera un CurrentUserResponseDTO con `id` y `email`.
6. El Controller responde `200 OK`.

### Flujo no autenticado

- Token ausente, malformado, con firma inválida o expirado: rechazo del Resource Server antes del Controller con `401` vacío.
- Token válido sin `sub` utilizable o sin usuario persistido: AuthenticationService produce el error específico de identidad de sesión; PersistedUserJwtAuthenticationConverter lo traduce a un fallo OAuth2 de autenticación y el entry point de Security responde `401` vacío.
- No se devuelve `404`, `ErrorResponseDTO` ni información que permita distinguir por qué falló la autenticación.

### Autorización de rutas

La regla pública amplia `/api/auth/**` se reemplaza por permisos exactos para `POST /api/auth/register` y `POST /api/auth/login`. Swagger/OpenAPI conserva acceso público y el resto continúa bajo `.anyRequest().authenticated()`, incluido `GET /api/auth/me`. El converter persistido se conecta al OAuth2 Resource Server para que toda autenticación Bearer aceptada corresponda a un usuario actual; no cambia el token, sus claims ni su duración.

## Testing Strategy

- **Service**: recuperar el usuario persistido por el email sujeto normalizado y rechazar sujeto nulo, vacío o inexistente usando PostgreSQL de Testcontainers sin acceder al Repository desde el test.
- **Controller**: verificar contrato exacto de `200`, mapeo desde el Model persistido simulado, `401` vacío por identidad inexistente, ausencia/invalidación/expiración del JWT y ausencia de interacción con Service cuando Security rechaza primero. Generar snippets REST Docs para cada respuesta contractual distinta.
- **Security**: demostrar que `/me` está protegido y que JWT ausente, inválido, expirado, sin sujeto o sin usuario persistido produce `401` vacío a través del flujo real del Resource Server; mantener la cobertura de acceso con JWT válido.
- **E2E**: extender registro → login → `/me`, deserializar el Response DTO productivo y comprobar email e id persistidos. Mantener la comprobación de los contratos existentes.
- **Regresión**: ejecutar la suite completa y el formateo obligatorio.

## Documentation Strategy

- Documentar la operación con OpenAPI y seguridad Bearer en alcance de método, sin marcar register/login como protegidos.
- Generar e incluir snippets de Spring REST Docs en `backend/src/docs/asciidoc/index.adoc`.
- Agregar `Current user` a la carpeta `Auth` de la colección Postman existente usando `{{JWT_TOKEN}}`.
- No versionar artefactos generados bajo `backend/build/`; actualizar `backend/src/docs/asciidoc/index.html` solo si el flujo existente confirma que es una fuente versionada y no un generado obsoleto.

## Complexity Tracking

No se identifican violaciones a la Constitución que requieran justificar complejidad adicional.
