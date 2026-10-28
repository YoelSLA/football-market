# Registro local de gates SDD — SPEC-015

Este archivo aplica la contingencia local de `docs/development/sdd-workflow.md` §12 mientras GitHub
Projects no está configurado. Registra evidencia histórica sin acreditar por sí mismo revisión,
testing ni cierre.

## ANALYZE — 2026-10-20 (primera pasada)

- **Feature**: `015-define-average-cost-and-financial-results-rules`
- **Artefactos evaluados**: `spec.md`, `plan.md`, `tasks.md`, `research.md`, `data-model.md`,
  `contracts/financial-position-service.md`, `quickstart.md` y checklists aplicables.
- **Resultado**: preparación completa; un hallazgo HIGH y tres LOW.
- **Hallazgo HIGH**: la garantía de consultas constantes no se cumplía; R-17 y T026 prevían una
  adquisición de cotizaciones por jugador, incompatible con FR-027 y con AC35 de SPEC-014.
- **LOW**: rótulo de Javadoc inexacto en el contrato del puerto; desajuste entre §2.1 y §5.2 del
  contrato; redacción divergente de «consultas agrupadas».
- **Cobertura**: `FR-001`–`FR-032` y `SC-001`–`SC-012` cubiertos por `T001`–`T033`; sin Tasks ajenas.
- **Tests y builds**: no ejecutados.

## Decisiones funcionales de la revisión con SPEC-014 — 2026-10-20

- **I1 porcentaje global**: la agregación vive en `FinancialPositionCalculator` y se expone como
  operación reutilizable sobre el conjunto completo; conserva `historicalCost` e `investedCost` como
  importes distintos.
- **I2 ciclos de tenencia**: `PlayerFinancialPosition` expone `openedAt`, `closedAt`,
  `lastTransactionAt` y los recuentos por rol, obtenidos en el mismo recorrido del historial.
- **I3 cotización**: `PlayerFinancialPosition` expone `quoteCalculatedAt` junto a
  `currentUnitPrice`, de la misma instantánea.
- **Acceso masivo**: extensión aditiva de `QuoteSource` con `currentQuotes(Set<Long>)`, conservando
  `currentQuote(Long)`; default que rechaza el soporte ausente sin delegar N lecturas.
- **B-01 y B-02** (detectados al preparar la implementación): el porcentaje multiplicaba por 100
  después de redondear, y el precio medio se declaraba indefinido con un tipo que no admite ausencia.
  Corrección: `HALF_UP_2(100 × total / histórico)` y precio medio `Long` nullable.

## IMPLEMENT AUTHORIZATION — 2026-10-20

- **Autorizante**: usuario de la sesión de OpenCode.
- **Evidencia textual**: «Autorizo la implementación de las 33 tareas definidas en tasks.md».
- **Alcance autorizado**: `T001`–`T033` de SPEC-015, respetando el orden de dependencias y el
  contrato de integración con SPEC-014; escribir pruebas sin ejecutarlas.
- **Límites**: no autoriza ampliar alcance, modificar SPEC-014, aprobar gates, hacer commit, push ni
  merge.
- **Inicio de IMPLEMENT**: 2026-10-20, tras reevaluar el checklist de calidad de requisitos.

## IMPLEMENT — 2026-10-20

- **Alcance implementado**: `T001`–`T035`, con `T034` y `T035` añadidos por la extensión masiva
  autorizada y `T031` en curso.
- **Resultado técnico**: `PlayerFinancialPosition` y `FinancialPositionCalculator` como Model puros;
  `FinancialPositionService` con lectura `REPEATABLE READ`, dos consultas contables de conjunto y un
  único lote de cotizaciones; extensión de `QuoteSource`; consultas de conjunto en ambos repositorios;
  índice `player_token_emissions (owner_user_id)`; unificación del reloj de confirmación en la compra
  directa; `docs/backend/architecture.md` con la relación Service → Service autorizada.
- **Build permitido**: `./gradlew build -x test` desde `backend/`, satisfactorio.
- **Tests**: escritos; ejecución del agente limitada por orden expresa a las dos clases afectadas.
- **CODE REVIEW**: informe emitido; hallazgos F1–F8 y tres LOW posteriores corregidos por orden
  expresa. No consta una frase propia de aprobación de CODE REVIEW; la secuencia del flujo y la
  aprobación posterior de TESTING, concedida sobre el código ya corregido, cubren el tránsito del gate.
- **TESTING**: ver sección siguiente.
- **Seguimiento operativo**: el tablero de GitHub Projects refleja el estado vigente de la feature.

## TESTING — 2026-10-20

- **Autorizante**: usuario de la sesión de OpenCode.
- **Evidencia textual**: «La validación de TESTING de SPEC-015 queda aprobada».
- **Primera ejecución completa** (persona responsable): 1170 pruebas y 2 fallos, ambos de esta feature:
  `FinancialPositionServiceTest` (emisión valorada a costo cero) y `FinancialPositionPersistenceTest`
  (consultas con N+1).
- **Causas raíz corregidas**: el fixture registraba auditorías sin la emisión inicial que el mercado
  exige, y las asociaciones `EAGER` se resolvían con una sentencia por fila. No se alteraron reglas
  contables ni aserciones.
- **Ejecución acotada intermedia** (por orden expresa del usuario, dos clases): 14 pruebas, 0 fallos,
  0 errores.
- **Ejecución completa final** (persona responsable, `./gradlew test` sobre el árbol actual): **1172
  pruebas en 216 clases, 0 fallos, 0 errores, 0 omitidos, 631 s**, incluidas las 27 pruebas de
  SPEC-015 en Model, Repository y Service. La ampliación del fixture no alteró el estado compartido
  de la base de pruebas de las demás clases.
- **Alcance aprobado**: la evidencia técnica de la suite completa, sin regresiones.

## Estado de autorizaciones

- **IMPLEMENT AUTHORIZATION**: consumida; el alcance autorizado está implementado y verificado.
- **CODE REVIEW**: satisfecha sobre el código corregido; hallazgos F1–F8 y tres LOW posteriores
  resueltos.
- **TESTING**: aprobada el 2026-10-20 con la evidencia de la suite completa.
- **FEATURE COMPLETION AUTHORIZATION**: pendiente. Requiere CONVERGE final y autorización humana
  explícita; no se concede por la aprobación de TESTING.

## Estado del proceso y del seguimiento

- **Status de `spec.md`**: `Ready for implementation`. El estado `Approved` exige CONVERGE final
  Circle y autorización de cierre, que no se han producido.
- **Seguimiento operativo**: la feature pasa a revisión, con implementación y pruebas cerradas y el
  cierre pendiente.
- **Trabajo sin publicar**: la implementación, las pruebas, la documentación arquitectónica y los
  artefactos SDD permanecen en el árbol de trabajo, sin commit, push ni merge.

## Divergencia documentada fuera de esta feature

- SPEC-014 precisa el precio medio como «no modificado por las ventas». Con el residuo de redondeo
  retenido, esa afirmación es exacta en aritmética fraccionaria y puede desviarse en menos de una
  centésima por token. La corrección corresponde en el origen, en la especificación de SPEC-014, que
  pertenece a otra rama y a otra feature.
