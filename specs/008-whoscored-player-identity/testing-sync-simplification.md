# Testing Report — sincronización única de identidad

Feature: SPEC-008, cambio 2026-11-01, tareas T058–T066. Complemento del [reporte de revisión completo](code-review-sync-simplification.md), que contiene rutas e inventario de archivos. Este informe registra el alcance de pruebas revisado y la confirmación del usuario; no acredita ejecución por el agente.

## Tests y cobertura

Rutas relativas a `backend/src/test/java/footballmarket/`:

| Tests | US y criterios | Assertions principales |
| --- | --- | --- |
| Nuevo `features/identity/domain/records/PlayerIdentitySyncSummaryTest.java` | US1/US2, SC-002/007, FR-016/019–021 | Categorías exclusivas, reconciliación, cantidades inválidas/overflow, finalización por pase y nueve combinaciones de prioridad global. |
| Adaptado `features/identity/domain/models/WhoScoredIdentityMatcherTest.java` | US1, SC-001/003, FR-005–009/013/027–029 | Nombre y equipo del mismo contexto, homónimos, normalización exacta, ambigüedad global y conflicto de propietarios; constructores transitorios. |
| Adaptado `features/identity/infraestructure/PlayerIdentityPersistenceTest.java` | US1/US2, SC-003/005/006 | Flyway retira tablas pero conserva índice/revisión; snapshot sin filtros nuevos, recaptura de referencias concurrentes, constraints, carreras, rollback técnico y atributos de catálogo preservados. |
| Adaptado `features/identity/application/PlayerIdentitySynchronizationServiceTest.java` | Ambas US, SC-002/004/006/007 | Una llamada, histórico elegible, cero elegibles, actual FAILED, histórico FAILED/PARTIAL, actual parcial, datos inválidos, ausencia de coincidencia frente a sin resolver, recaptura, interrupción, red sin transacción y relanzamiento. |
| Adaptado `features/identity/presentation/PlayerIdentityControllerTest.java` | US1/US2, FR-021–023, SC-007/008 | Cinco campos exactos en los tres estados y vacío; 500 ErrorResponseDTO, JWT ausente/expirado/firma inválida y ausencia de endpoint histórico. |
| Adaptado `features/identity/e2e/PlayerIdentityE2ETest.java` | Ambas US, SC-002/003/004/008 | Autenticación/catálogo y sincronización por HTTP real con DTO; histórico automático, cinco jugadores una vez, idempotencia y catálogo no modificado. |
| Seis consumidores de `features/statistics/application/` y `features/statistics/e2e/` enumerados en Code Review Report | Regresión de preparación de SPEC-012 | Preparan referencias con API única y verifican el campo vigente, sin ID de auditoría. No cambian las reglas estadísticas. |

Se retiró `PlayerIdentityAuditModelTest` porque sus entidades/estados persistidos desaparecen. Reglas aún vigentes se verifican en resumen, Service y Repository. Los tests existentes de Integration/pacer conservan protocolo, parsing, paginación, límites, ritmo y ausencia de retries; no se crean tests artificiales de auditoría retirada ni tests directos de Mapper.

## Dobles y fronteras

- Model: unitario puro, sin Spring/DB.
- Repository: PostgreSQL/Testcontainers, JPA y Flyway reales; acceso a persistencia permitido por la categoría, incluyendo errores de DB controlados y carreras.
- Service: SpringBootTest, Service/repositorios/DB reales e Integration mock; assertions funcionales solo por la API productiva. Properties spy ajusta bindings de fixtures; no se mockea el componente probado.
- Controller: slice MVC y cadena de seguridad reales, Service y autenticación auxiliar sustituidos para aislar el contrato; rechazo previo verifica ausencia de invocación del Service.
- E2E: servidor y componentes productivos reales, proveedor HTTP controlado mediante `IdentityControlledProvider`, RestClient y DTO productivos; sin Internet ni credenciales reales.

## Riesgos y revisión recomendada

Revisar primero categorías y prioridad de estados, después Service/Repository (constraints, aislamiento, carreras y migración) y finalmente Controller/E2E y consumidores de estadísticas. El mock de Integration en Service no demuestra protocolo; se complementa con Integration/E2E. El build no compila ni ejecuta estos tests y no demuestra por sí solo la migración. El proveedor simulado no garantiza disponibilidad ni cobertura de WhoScored real.

Comandos propuestos para el usuario desde `backend/`, con Docker disponible para las categorías que lo necesitan:

```powershell
.\gradlew.bat test --tests "footballmarket.features.identity.*"
.\gradlew.bat test --tests "footballmarket.integrations.whoscored.*"
.\gradlew.bat test --tests "footballmarket.features.statistics.*"
```

## Resultado y autorización registrados

El usuario informó «corren todos los tests» y confirmó posteriormente «La 1 y 2 ya estan», al referirse explícitamente a revisión de código y validación de testing. **TESTING aprobado por el usuario para el diff local revisado**, sin cambios funcionales posteriores. No se exige salida completa de una suite satisfactoria ni se afirma qué comando exacto ejecutó el usuario, porque no lo aportó. El agente no ejecutó tests ni aplicó migraciones.
