# Feature Specification: Programación semanal de datos deportivos y evaluación

**Feature Branch**: `feat/024-schedule-sports-data-sync`

**Created**: 2026-11-05

**Status**: Draft

**Input**: Issue [#41](https://github.com/YoelSLA/football-market/issues/41), `REQ-TP-05`: actualizar datos deportivos y evaluarlos semanalmente, manteniendo independiente el recálculo de cotizaciones.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Actualización y evaluación semanales (Priority: P1)

Como responsable del catálogo, quiero que las métricas deportivas se actualicen y evalúen automáticamente cada semana, para disponer de datos recientes sin intervención manual.

**Why this priority**: Es el propósito central de la funcionalidad.

**Independent Test**: Verificar una ejecución en el horario establecido y observar el orden y resultado de las tres operaciones existentes.

**Acceptance Scenarios**:

1. **Given** que llega el domingo a las 12:00 en `America/Argentina/Buenos_Aires`, **When** se activa la programación, **Then** inicia una ejecución semanal.
2. **Given** una ejecución programada, **When** JSON finaliza sin fallo global y luego scraping finaliza sin fallo global, **Then** se ejecuta la evaluación, en ese orden y sin solapamiento entre etapas.
3. **Given** una ejecución que termina, **When** el responsable consulta el registro operativo existente, **Then** puede identificar su resultado y, ante una falla, la operación y el error comunicado por ella.
4. **Given** una ejecución deportiva, **When** se realiza la evaluación, **Then** no se ejecutan el recálculo de cotizaciones ni operaciones financieras.

### User Story 2 - Fallos y disponibilidad (Priority: P1)

Como administrador, quiero conocer los fallos y conservar las operaciones manuales disponibles, para poder actuar sin que una ejecución automática fallida afecte el trabajo posterior.

**Why this priority**: Evita ejecutar etapas dependientes tras un fallo y conserva el control manual.

**Independent Test**: Provocar fallos globales en cada etapa y comprobar secuencia, registro y disponibilidad manual.

**Acceptance Scenarios**:

1. **Given** que JSON falla globalmente, **When** termina esa operación, **Then** no se ejecutan scraping ni evaluación y el registro identifica JSON y su error.
2. **Given** que JSON termina sin fallo global y scraping falla globalmente, **When** termina scraping, **Then** no se ejecuta la evaluación y el registro identifica scraping y su error.
3. **Given** que falla la evaluación, **When** termina la ejecución, **Then** el registro identifica la evaluación y comunica su error.
4. **Given** una falla de la ejecución automática, **When** el administrador autorizado ejecuta manualmente los procesos existentes, **Then** la falla del scheduler no impide esas operaciones.

### Edge Cases

- **Obligatorio — resultado parcial**: las operaciones de SPEC-012 distinguen fallos globales de resultados parciales por jugadores; un resultado parcial no se convierte por sí solo en fallo global ni en un umbral de cobertura nuevo. Se conservan sus contratos existentes.
- **Obligatorio — datos previos**: un fallo no destruye datos válidos ya persistidos; la identidad de los jugadores y las garantías de persistencia corresponden a las operaciones existentes de SPEC-012 y SPEC-013.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST iniciar la secuencia una vez por semana, los domingos a las 12:00, en la zona `America/Argentina/Buenos_Aires`.
- **FR-002**: La secuencia MUST ejecutar las operaciones existentes en orden: actualización JSON, actualización por scraping y evaluación estadística. Cada operación MUST terminar antes de iniciar la siguiente.
- **FR-003**: Ante fallo global o interrupción de JSON, el sistema MUST detener la secuencia sin ejecutar scraping ni evaluación. Ante fallo global o interrupción de scraping, MUST detenerla sin ejecutar la evaluación.
- **FR-004**: El resultado de cada ejecución automática MUST comunicarse mediante el registro operativo existente. Si falla una operación, MUST identificarla y comunicar el error que esta devuelve, sin exponer secretos ni información sensible. JSON y scraping MUST distinguirse claramente.
- **FR-005**: Retirado: no se requiere contemplar ejecuciones en paralelo.
- **FR-006**: Una falla de la programación MUST NOT impedir al administrador autorizado ejecutar manualmente los procesos existentes, respetando sus contratos y restricciones propios.
- **FR-007**: La secuencia MUST reutilizar las operaciones de SPEC-012 y SPEC-013 y las capacidades de programación existentes, sin incorporar herramientas ni infraestructura nuevas. MUST conservar la asociación correcta de datos con jugadores y no destruir datos válidos por una falla.
- **FR-008**: La sincronización y evaluación deportivas MUST permanecer independientes del recálculo de cotizaciones y MUST NOT ejecutar operaciones financieras.
- **FR-009**: El scheduler MUST NOT incorporar porcentajes de éxito, métricas de cobertura ni mecanismos equivalentes. Los resúmenes y la semántica preexistentes de las operaciones no se modifican por esta restricción.

### Key Entities

- **Ejecución programada**: intento semanal de las tres operaciones ordenadas, con resultado observable y etapa fallida cuando corresponda.
- **Datos deportivos y evaluación**: métricas asociadas a los jugadores y valoraciones obtenidas mediante los procesos existentes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En cada horario semanal definido se inicia una secuencia.
- **SC-002**: En una ejecución sin fallos globales se observan exactamente tres operaciones, en el orden JSON → scraping → evaluación.
- **SC-003**: En cada fallo global de JSON o scraping se observan cero ejecuciones de las etapas posteriores y el registro identifica la operación y su error; ante fallo de evaluación también se comunica su error.
- **SC-004**: Tras una falla automática, el administrador puede iniciar los procesos manuales existentes.
- **SC-005**: La secuencia no ejecuta recálculos de cotizaciones ni operaciones financieras y conserva las garantías existentes sobre identidad y datos válidos.

## Assumptions

- La aplicación está activa cuando llega el horario; no se establece recuperación de disparos perdidos ni reintento automático.
- SPEC-012 distingue resultados `COMPLETED`, `PARTIAL`, `FAILED` e `INTERRUPTED`: `FAILED` e `INTERRUPTED` y las excepciones globales detienen la secuencia; `PARTIAL` conserva la semántica de la operación y no implica por sí solo un fallo global. La evaluación conserva sus propias condiciones para seleccionar datos y sus errores.
- Los errores globales existentes se comunican mediante excepciones o resultados de las operaciones; el registro operativo permite informar el resultado automático sin crear un canal o consulta nuevos.
- Se asume que las operaciones no se ejecutan en paralelo. Los procesos reutilizados conservan sus fechas y comportamiento de persistencia propios.
- No se agregan nuevas métricas deportivas, interfaz, programación configurable, reintentos, historial propio del scheduler ni cambios en el cálculo de cotizaciones.

## Evidencia de SPECIFY

- Identidad: la rama local y remota `feat/024-schedule-sports-data-sync` existía sin carpeta de SPEC; el usuario confirmó que SPEC-024 y esa rama corresponden al issue #41 y que no existe otra asignación concurrente.
- Colisiones: se revisaron directorios locales `specs/`, `specs/assignments.md`, ramas locales y referencias remotas accesibles; la rama 024 corresponde a esta feature según confirmación humana. Creación automática: `SPEC_ACTION=created`, `BRANCH_STATUS=already-current`.
- Contexto: SPEC-012 conserva estados parciales y sus resúmenes; SPEC-013 realiza la evaluación existente; SPEC-023 registra errores de su scheduler en el registro operativo. No se modifica el seguimiento remoto.
- Validación de SPECIFY documentada en [checklists/requirements.md](checklists/requirements.md). CLARIFY permanece pendiente y es obligatorio antes de PLAN; este borrador no autoriza implementación.

## Evidencia de CLARIFY

- 2026-11-05: escaneo de alcance, comportamiento, datos, fallos, dependencias, seguridad y criterios de aceptación; no se identificaron ambigüedades críticas que requieran preguntas. Se conservan los contratos de SPEC-012 y SPEC-013, incluida la selección de la campaña confirmada por la evaluación y las fechas propias de cada operación.
- El análisis no autoriza implementación. La finalización de CLARIFY queda pendiente de la publicación y del Pull Request requeridos por el workflow.
