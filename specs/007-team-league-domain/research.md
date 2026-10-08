# Investigación: Dominio de equipos y ligas

Fecha: 2026-10-06. Fuente funcional: [spec.md](spec.md), RF-001–RF-039. Estas decisiones reemplazan las propuestas previas invalidadas; no acreditan implementación ni ejecución de controles.

## R1. Proveedor compartido

- **Decisión:** renombrar PlayerProvider a ExternalProvider, manteniendo FOOTBALL_DATA, THE_SPORTS_DB y EnumType.STRING.
- **Razón:** claridad del dominio, no obligación funcional. El renombrado es mecánico en modelos, repositorios, servicios y tests consumidores; no cambia valores persistidos, migraciones, JSON u OpenAPI.
- **Alternativas:** conservar el nombre antiguo es viable, pero se aprobó el nombre compartido; no duplicar enums por entidad.

## R2. Asociación y backfill

- **Decisión:** presente válido se reconoce por referencias de jugador/equipo; ausente aún sin asociación recibe matching estricto; presente inválido conserva estado sin fallback. Asociados previamente no reciben backfill. Tras finalizar la transición no se reutiliza evidencia de casos como fallback.
- **Razón:** RF-017/RF-034 protegen conversión legacy, no la autoridad del nombre histórico frente a identidad actual.
- **Alternativas:** matching textual universal rechazado; puede ocultar transferencias y crear pendientes artificiales. Liga textual no desambigua.

## R3. Atomicidad y presencia

- **Decisión:** foto inmutable con identidades presentes, candidatos procesables, protecciones y anomalías. HTTP antes de escritura. TransactionTemplate con intento completo y un único commit final para dominio, referencias, backfill, casos y actividad.
- **Razón:** RF-032/RF-037/RF-039. Un flush no confirma; fallo técnico revierte todo. Protección incluye jugadores previos de un Team cuyo plantel no puede determinarse por invalidez individual.
- **Alternativas:** commits por jugador/equipo o continuar después de fallo JPA rechazados. Conflicto UNIQUE reconocido por SQLSTATE 23505, tabla y constraint: rollback y reaplicación de la misma foto excluyendo el conflicto, registrándolo en el intento válido. Otros errores se propagan, no se convierten en descartes. Reintentos acotados por nuevas exclusiones.

## R4. TheSportsDB independiente

- **Decisión:** enriquecimiento solo tras commit principal; red sin escritura abierta; transacción corta por Team para intento, referencia y conflicto. Revalidar nombre, elegibilidad y propietario antes de escribir. Fallo por equipo no altera resultado principal.
- **Razón:** RF-022/RF-038 permiten enriquecimiento parcial entre equipos, no escritura parcial del resultado de un equipo.
- **Alternativas:** incluir red o enriquecimiento en commit principal rechazado. Se conserva pacing; una tentativa lógica por equipo/ejecución, sin retries internos nuevos por defecto. 429 respeta Retry-After, los fallos técnicos no consumen evaluación válida.

## R5. Confianza y límites externos

- **Decisión:** TeamNameNormalizer puro en Model: trim, diacríticos, mayúsculas independientes de locale, espacios colapsados. Comparar nombre principal de equipos Soccer, contar identidades externas distintas. No aliases, sufijos ni fuzzy; duplicados contradictorios invalidan la evaluación, no se elige el primero.
- **Razón:** RF-021/RF-034; PlayerIdentityMatcher tiene tolerancia distinta que se conserva para imágenes, cambiando únicamente su acceso al nombre de Team.
- **Fuente:** [documentación TheSportsDB](https://www.thesportsdb.com/documentation): searchteams.php Free Limit 1, Premium Limit 100 y Free limitado a Arsenal. No documenta aquí prueba suficiente de completitud relevante. Premium tampoco implica resultados ilimitados.
- **Consecuencia:** el adaptador representa completitud como acreditada solo con evidencia verificable del contrato/respuesta, nunca por cantidad recibida o nivel supuesto. Para searchteams.php, mientras no exista tal evidencia, retorna COMPLETENESS_UNPROVEN y no asigna referencia, aunque reciba un match. Eso es una evaluación técnicamente válida y consume oportunidad para el nombre, no un fallo técnico. Fixtures pueden demostrar la regla con completitud acreditada; no prueban que el servicio real la ofrezca.
- **Alternativas:** asumir Premium, tomar primer resultado o lookup como prueba de unicidad rechazados. Lookup confirma candidato, no ausencia de otros.

## R6. Casos de revisión

- **Decisión:** registro deduplicado sin status; sujeto interno o externo, clave funcional estable, código de causa, fechas y evidencia; no historial completo. Consulta humana SQL documentada, operador de solo lectura separado de credenciales generales de aplicación.
- **Razón:** RF-031/RF-036. Cero y múltiples son motivos de LEGACY_TEAM_ASSOCIATION, misma clave por Player. Cambiar motivo/candidatos actualiza caso y preserva texto original. Referencia externa sin entidad identifica al sujeto; sin identidad suficiente solo observación por ejecución/contexto cuando no comprometa completitud.
- **Alternativas:** fila por ejecución, status PENDING constante, endpoint, frontend o comando rechazados. Repository queda acceso interno, no canal de revisión. Registro no certifica que el problema siga presente.

## R7. Dos releases y compatibilidad

- **Resolución B-V5-01 (2026-10-07):** conservar todas las referencias legacy válidas de Player, incluidas varias del mismo proveedor. Mantener unicidad por `(provider,external_id)`, no agregar `(player_id,provider)`. Esta última solo se exige a Team/League. No selección/eliminación/sobrescritura de referencias ni precondición de regularización externa. Se preserva la semántica de SPEC-002.

- **Decisión:** A aprobada para reducir complejidad: Release 1 con V5 y código intermedio; usuario sincroniza y verifica; Release 2 con V6 y código final. No incluir V6 en Release 1.
- **Razón:** decisión técnica, no exigencia de RF-011. Flyway aplica migraciones al arranque; no puede esperarse una sincronización HTTP entre V5 y V6 del mismo despliegue poblado.
- **Convivencia:** legacy NOT NULL sigue válido. Nuevas altas reciben textos derivados de Team/League; asociados escriben espejo temporal, nunca criterio de identidad. No asociados preservan textos originales. Antes de sobrescribir texto para un caso se conserva evidencia original. GET nuevo usa solo asociaciones vigentes incluso en Release 1; antes del gate pueden faltar jugadores aún sin asociación.
- **Alternativas:** tabla temporal en un release es viable pero más compleja; limpiar en otra feature prolonga convivencia. No usar HTTP desde Flyway ni DDL en caliente tras sincronización.

## R8. Recuperación y presentación

- **Decisión:** reintentar y reparar hacia adelante antes de V6; V6 transaccional con guarda y backup previo; forward-only después. Downgrade directo no garantizado. Detalles en quickstart.
- **Razón:** nombres actuales no recuperan necesariamente legacy original; no se promete historial.
- **Presentación aprobada (alternativa C):** clasificar por leagueName y conservar el fallback neutral existente para nombres desconocidos. Un rename puede cambiar temporalmente el estilo; no se exige continuidad visual histórica. leagueId sigue siendo identidad interna estable, no clave de presentación.
- **Alternativas descartadas:** mapa por leagueId necesitaría correspondencias iniciales por entorno; campo de clasificación agregaría contrato y asignación de códigos fuera del alcance aprobado. No se agregan mapas, almacenamiento de clasificación ni leagueCode/leagueSlug/visualKey. La abstención visual de un cliente nuevo ante nombre desconocido es el fallback acordado, no un bloqueo funcional.

## Contexto efectivo

Se reutilizan Java 21, Spring Boot 4.1.1, JPA, PostgreSQL/Flyway, RestClient y cliente TheSportsDB existentes; frontend TypeScript/React/Vite. No dependencias nuevas. FootballDataProperties pasa de cinco códigos fijos a lista no vacía y sin duplicados, conservando configuración inicial. No se genera tasks ni se ejecutan controles de código en esta revisión documental.
