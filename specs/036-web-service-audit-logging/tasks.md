# Tasks: Auditoría de peticiones a servicios web

**Input**: Design documents in `/specs/036-web-service-audit-logging/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/audit-log.md`, `quickstart.md`

**Tests**: Incluir pruebas backend necesarias según las normas del proyecto; su ejecución corresponde al usuario y el agente no las ejecuta. No crear pruebas frontend.

**Workflow local**: Seguir `docs/development/sdd-workflow.md`. Las tareas viven exclusivamente en este archivo; completarlas no acredita cierre ni autoriza fases posteriores.

## Phase 1: Setup

No se requieren tareas de inicialización: el proyecto Spring Boot, Spring MVC, Spring Security y el logging SLF4J/Logback ya existen y se reutilizan.

## Phase 2: Foundational

No se requieren prerrequisitos compartidos adicionales. La auditoría se integra en la frontera MVC existente y utiliza el contexto de seguridad y logging ya configurados.

## Phase 3: User Story 1 - Consultar auditoría de peticiones REST (Priority: P1) 🎯 MVP

**Goal**: Producir un registro de auditoría para cada petición entrante a un endpoint REST Spring, con identidad, operación, parámetros de consulta reconocidos excepto identificadores, y duración, sin exponer los datos excluidos por el contrato.

**Independent Test**: Enviar peticiones autenticadas y anónimas, exitosas y fallidas, con parámetros reconocidos/no reconocidos, identificadores de consulta y credenciales en la solicitud; comprobar que se registran los parámetros reconocidos no sensibles y se excluyen los identificadores y credenciales. La ejecución queda a cargo del usuario.

### Tests para User Story 1

- [x] T001 [US1] Crear pruebas MVC de auditoría en `backend/src/test/java/footballmarket/common/presentation/RequestAuditInterceptorTest.java` que comprueben registro autenticado y `anonymous`, método y patrón de ruta, inclusión de parámetros de consulta reconocidos no sensibles, exclusión de parámetros no reconocidos e identificadores, ausencia de parámetros, petición completada con error y ausencia de credenciales/contraseñas en los registros.

### Implementation for User Story 1

- [x] T002 [US1] Implementar `RequestAuditInterceptor` en `backend/src/main/java/footballmarket/common/presentation/RequestAuditInterceptor.java` para medir cada invocación MVC, resolver el usuario desde el contexto de seguridad o `anonymous`, obtener la operación y los parámetros reconocidos no sensibles por el handler, excluir identificadores de consulta y credenciales/contraseñas, y emitir timestamp, método, ruta, parámetros registrables y duración en milisegundos mediante SLF4J también al finalizar con error.
- [x] T003 [US1] Registrar `RequestAuditInterceptor` en la configuración MVC de `backend/src/main/java/footballmarket/common/presentation/HttpMvcConfiguration.java`, aplicándolo a los endpoints REST de la aplicación y sin extender la auditoría a llamadas salientes.

## Phase 4: Polish & Cross-Cutting Concerns

No se añaden tareas adicionales fuera del alcance de la User Story. La prueba de aceptación independiente anterior cubre los criterios transversales de seguridad, autenticación y errores.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin tareas; la infraestructura base ya existe.
- **Foundational (Phase 2)**: Sin tareas; no hay prerrequisitos nuevos.
- **User Story 1 (Phase 3)**: No depende de otras historias.
- **Polish (Phase 4)**: No tiene tareas independientes; los criterios transversales forman parte de US1.

### User Story Dependencies

- **User Story 1 (P1)**: Única historia; puede comenzar sin completar otras historias.

### Within User Story 1

- T001 define y cubre los comportamientos observables del contrato.
- T002 implementa la observación del ciclo de vida MVC y el registro seguro.
- T003 conecta el interceptor a MVC para cubrir los endpoints REST.
- T003 depende de T002; las pruebas se ejecutan por el usuario tras implementar, nunca por el agente.

### Parallel Opportunities

- No se identifican tareas paralelizables: las tareas de implementación comparten la misma integración MVC y siguen la dependencia T001 → T002 → T003.

## Parallel Example: User Story 1

No aplica: las tareas tienen dependencia secuencial para mantener coherentes la prueba, el interceptor y su registro en MVC.

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Preparar cobertura MVC de los criterios de US1 (T001).
2. Implementar el interceptor con el filtrado de datos sensibles y el registro requerido (T002).
3. Registrar el interceptor para endpoints REST (T003).
4. Solicitar al usuario la ejecución de las pruebas backend indicadas; el agente no ejecuta tests.

### Incremental Delivery

US1 constituye el alcance completo de esta Feature. La finalización de sus tareas no habilita por sí sola ANALYZE, IMPLEMENT, autorizaciones humanas ni cierre.

**Verificación de IMPLEMENT**: la persona usuaria informó que el test de T001 pasa; el agente no lo ejecutó ni compiló, y no se aportó el comando ni la salida. El build backend `./gradlew build -x test` terminó satisfactoriamente.
