# Research: Current Authenticated User

## Protección selectiva de `/api/auth/me`

**Decision**: Reemplazar el permiso amplio de `/api/auth/**` por permisos exactos para registro y login; `/me` queda cubierto por la autenticación obligatoria general.

**Rationale**: La configuración actual haría público cualquier endpoint bajo `/api/auth`. Los matchers exactos preservan el comportamiento existente y protegen la nueva operación sin agregar filtros propios.

**Alternatives considered**:

- Agregar un matcher autenticado antes de `/api/auth/**`: funciona por orden, pero conserva una regla pública demasiado amplia para futuras rutas.
- Protección por anotación únicamente: duplica política y deja una configuración global engañosa.

## Fuente de identidad y consulta persistida

**Decision**: Obtener el `sub` del principal JWT ya validado, pasarlo a AuthenticationService y consultar UserRepository por email normalizado.

**Rationale**: El Resource Server ya verifica firma y expiración. AuthenticationService ya posee la responsabilidad de identidad y acceso al Repository; la consulta prueba que el usuario continúa existiendo y evita usar claims como datos de respuesta.

**Alternatives considered**:

- Devolver claims del JWT: incumple la fuente de verdad persistida y no proporciona el id.
- Decodificar manualmente el token: duplicaría validación criptográfica.
- Usar UserService: su contrato actual es administrativo y mezclaría responsabilidades.
- Crear un Service nuevo: subdivisión innecesaria para una única operación cohesiva con autenticación.

## Usuario inexistente con JWT válido

**Decision**: AuthenticationService producirá un error específico de identidad de sesión y un converter de autenticación JWT lo transformará en un fallo OAuth2 antes de crear la autenticación; el entry point de Security devolverá `401 Unauthorized` sin cuerpo.

**Rationale**: No debe reutilizarse UserNotFoundException porque su contrato actual es `404` con ErrorResponseDTO, ni InvalidCredentialsException porque cambiaría el login. Al resolver el caso dentro de Security, el `401` vacío no pasa por `GlobalExceptionHandler` y se mantiene la obligación constitucional de usar ErrorResponseDTO para errores gestionados por la aplicación.

**Alternatives considered**:

- Retornar Optional al Controller: trasladaría una decisión de aplicación a la frontera HTTP y debilitaría el flujo Model → Mapper.
- Traducir la excepción en GlobalExceptionHandler: produciría un error gestionado por la aplicación sin ErrorResponseDTO y violaría la Constitución.
- Cambiar el handler de UserNotFoundException: podría alterar consumidores existentes.

## Contrato de respuesta

**Decision**: Crear un Response DTO `record` con `Long id` y `String email`, producido por UserMapper desde el User persistido.

**Rationale**: Mantiene separado el contrato HTTP, impide exponer password y cumple el flujo arquitectónico Controller → Mapper.

**Alternatives considered**:

- Serializar User directamente: expondría el Model y potencialmente la contraseña.
- Construir el DTO en el Controller: rompería la responsabilidad del Mapper.

## Persistencia y modelo

**Decision**: Reutilizar User, la tabla `users` y `UserRepository.findByEmail`; no crear migración ni query nueva.

**Rationale**: El modelo ya contiene `id`, `email` y `password`, y el email posee unicidad. La operación es una lectura por identidad existente.

**Alternatives considered**:

- Agregar claims o columnas: no aporta valor y está fuera de alcance.
- Proyección específica de Repository: complejidad innecesaria y acoplamiento del contrato HTTP a persistencia.

## Estrategia de pruebas y documentación

**Decision**: Cubrir responsabilidades en Service, Controller, Security y el journey E2E; documentar con OpenAPI, REST Docs, Asciidoctor y la colección Postman existente.

**Rationale**: La feature combina consulta persistida, contrato HTTP y seguridad. Cada nivel verifica una responsabilidad diferente conforme a la Constitución.

**Alternatives considered**:

- Solo Controller test: no demostraría persistencia real ni integración JWT.
- Solo E2E: dificultaría localizar fallos y no generaría toda la documentación contractual requerida.
- Nueva colección Postman: fragmentaría la documentación ya existente.

## Dependencias y rendimiento

**Decision**: No agregar dependencias, caché, reintentos ni métricas específicas. Realizar una lectura por email en cada llamada exitosa.

**Rationale**: Consultar siempre persistencia es un requisito funcional. El email ya es único y la escala de esta feature no justifica optimización anticipada.

**Alternatives considered**:

- Caché de identidad: podría devolver usuarios eliminados o datos obsoletos.
- Incluir id/email en el JWT: incumple el alcance y seguiría sin demostrar que el usuario existe actualmente.
