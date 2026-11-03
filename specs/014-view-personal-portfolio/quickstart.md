# Guía de validación: Consultar portfolio personal (SPEC-014)

**Feature**: SPEC-014 · **Rama**: `feat/014-consultar-portfolio-personal` · **Fecha**: 2026-10-20
**Plan**: [`../plan.md`](../plan.md) · **Contrato**: [`../contracts/portfolio-api.md`](../contracts/portfolio-api.md) ·
**Modelo**: [`../data-model.md`](../data-model.md)

Guía de validación de la consulta del portfolio. Describe **cómo comprobar** que el comportamiento
acordado se cumple, no cómo se implementa: el detalle de implementación, las tareas y el código
pertenecen a `tasks.md` y a la fase de implementación.

> La ejecución de las pruebas corresponde al usuario. Los comandos que aparecen abajo son los que
> el proyecto ofrece para esa ejecución; ninguno se ejecuta como parte de esta fase.

---

## 1. Prerrequisitos

| Requisito | Detalle |
| --- | --- |
| Java | 21 (toolchain del backend) |
| Docker | Necesario para Testcontainers: las categorías Repository y Service usan PostgreSQL real |
| Base de datos | Se levanta con Testcontainers; **no** se usa la base de desarrollo ni la de producción |
| Dependencia contable | El componente `FinancialPositionService` de SPEC-015 debe estar integrado. Mientras no lo esté, la parte financiera no puede validarse de extremo a extremo |
| Migraciones | Las del proyecto se aplican automáticamente sobre la base de pruebas |

### Advertencia previa sobre la base de desarrollo

La base de desarrollo es **desechable**: el proyecto permite reinicializarla, y los datos de
desarrollo no son un requisito. Aun así, no debe usarse para validar esta feature, porque las
pruebas exigen un entorno aislado y reproducible.

---

## 2. Comandos de validación

Desde `backend/`:

| Comando | Qué ejecuta |
| --- | --- |
| `./gradlew validateTestClassification` | Validación estática de la clasificación por etiquetas de las pruebas |
| `./gradlew fastTest` | Suites `unit` y `lightweight`, sin cobertura |
| `./gradlew integrationTest` | Suite `integration`, sin cobertura: persistencia real y caso de uso |
| `./gradlew test` | Suite completa con cobertura |
| `./gradlew build -x test` | Control de compilación, formato y clasificación, **sin** ejecutar pruebas |

Los escenarios de este documento se validan con `integrationTest` (persistencia y caso de uso) y con
`fastTest` (contrato HTTP), y se corroboran de extremo a extremo con `test`.

---

## 3. Escenarios de validación

### 3.1 Categoría Controller — contrato HTTP

| # | Escenario | Resultado esperado |
| --- | --- | --- |
| 1 | Petición autenticada a `GET /api/portfolio/me` | 200 con `summary`, `positions` y `pagination`; los tres objetos presentes |
| 2 | Petición sin credencial, con credencial inválida o con sesión vencida | 401; el caso de uso no se ejecuta |
| 3 | `page` negativa, `size` cero, `size` mayor que 100 o valor no entero | 400 `INVALID_PARAMETER_TYPE`, sin posiciones ni resumen |
| 4 | `status` distinto de `OPEN` o `CLOSED`, incluida otra capitalización | 400 `INVALID_PARAMETER_TYPE`; no hay conversión de mayúsculas |
| 5 | Forma de la respuesta | Todos los campos de `summary`, `positions` y `pagination` presentes, con los nombres y las unidades del contrato |
| 6 | Petición de un usuario sobre otro | No existe ruta que lo permita: la única ruta opera sobre la identidad de la sesión |

### 3.2 Categoría Service — composición del portfolio

| # | Escenario | Resultado esperado |
| --- | --- | --- |
| 7 | Usuario con posiciones en varios jugadores, con y sin cotización | Las posiciones aparecen con sus cifras; las que no tienen cotización conservan cantidad, reserva, disponibilidad y resultado realizado, y sus campos de cotización son nulos |
| 8 | Usuario sin tenencias ni operaciones | 200 con `positions` vacío, indicadores en cero y `valuationComplete` en `true` |
| 9 | Posición abierta y posición cerrada | La abierta informa `status` `OPEN`; la cerrada informa `CLOSED`, `valuationStatus` `NOT_APPLICABLE`, valor actual y resultado no realizado nulos, y su resultado realizado acumulado |
| 10 | Filtro `status=OPEN` y filtro `status=CLOSED` | Cada filtro devuelve solo las posiciones de su estado, y los metadatos de paginación cuentan ese conjunto |
| 11 | Consulta sin filtro | Devuelve todas las posiciones |
| 12 | Paginación por defecto y con `page`/`size` explícitos | La página contiene como máximo `size` posiciones; los metadatos informan el total y la cantidad de páginas del conjunto filtrado |
| 13 | Página superior a la última | 200 con `positions` vacío, metadatos correctos y resumen del portfolio completo |
| 14 | Orden de las posiciones | Primero las abiertas con cotización por valor actual descendente, después las abiertas sin cotización, después las cerradas por nombre; dos consultas sobre el mismo estado devuelven la misma secuencia |
| 15 | Resumen con filtro y con paginación distintos | El resumen es idéntico en todas las consultas: representa el portfolio completo |
| 16 | Tokens reservados por publicaciones vigentes, canceladas o vencidas | Solo las activas y vigentes reservan; `quantity = reservedQuantity + availableQuantity` en cada posición y en el resumen |
| 17 | Cotización disponible | La posición informa cotización, instante de cotización, valor actual y resultado no realizado |
| 18 | Cotización inexistente | 200 con la valoración incompleta: `valuationComplete` en `false` y los totales dependientes de cotización nulos; el resultado realizado se conserva |
| 19 | Falla técnica del servicio de cotizaciones | 503, sin datos financieros parciales, ni siquiera el resultado realizado |
| 20 | Posición con tokens emitidos y con tokens adquiridos comercialmente | Un único costo de adquisición, un único valor y un único resultado; la emisión no cuenta como compra |
| 21 | Posición cerrada y posteriormente reabierta | `closedAt` en nulo, `openedAt` con el inicio del ciclo vigente y resultado realizado acumulado, sin reiniciarse |
| 22 | Porcentajes por posición | El rendimiento individual usa el costo de adquisición de esa posición y el peso usa el valor total de todas las posiciones abiertas; ambos nulos cuando su denominador es cero o falta una cotización |
| 23 | Porcentaje global | Se calcula sobre el costo histórico total pagado, no sobre el costo de las posiciones abiertas; es nulo si el denominador es cero o falta una cotización |
| 24 | Divergencia entre la tenencia persistida y la reconstrucción contable | Error de integridad 500, sin devolver cifras inconsistentes |
| 25 | Jugador inactivo | Permanece en el portfolio con `active` en `false` y sus datos descriptivos |
| 26 | Operaciones simultáneas con la consulta | La respuesta es internamente coherente: las cantidades, las reservas, las cifras, el resumen y las posiciones de esa misma respuesta pertenecen a un mismo estado |
| 27 | Dos consultas consecutivas sobre el mismo estado | Resultados financieros idénticos, incluidos el costo de adquisición y los porcentajes |
| 28 | Una venta confirmada deja una tenencia en cero y una compra reabre otra tenencia, entre dos consultas paginadas distintas | La segunda respuesta refleja el estado vigente: la posición cerrada aparece como `CLOSED`, la reabierta como `OPEN` con `closedAt` nulo, y los metadatos de paginación se recalculan. No se mantiene un snapshot entre peticiones |
| 29 | La fuente de cotizaciones entrega un precio distinto del valor por defecto | El valor actual de las posiciones refleja ese precio, y no un valor constante: el portfolio no introduce precios simulados ni depende de valores fijos, y funciona igual con una fuente de cotizaciones reales |

### 3.3 Categoría Repository — consultas de conjunto

| # | Escenario | Resultado esperado |
| --- | --- | --- |
| 30 | Tenencias de un propietario con posiciones en varios jugadores | Devuelve todas sus tenencias con su cantidad, en una sola consulta |
| 31 | Reservas del propietario agrupadas por jugador | Suma las cantidades de sus publicaciones activas y vigentes por jugador; las vencidas, canceladas o vendidas no reservan |
| 32 | Jugadores de una página leídos por lote | Una sola consulta devuelve los jugadores con su equipo, sin una consulta por posición |
| 33 | Índice por propietario sobre las tenencias | El índice existe y el listado por propietario lo usa |

### 3.4 Regresión

| # | Escenario | Resultado esperado |
| --- | --- | --- |
| 34 | Compra y venta de tokens, y publicación de una venta | El comportamiento del mercado no cambia: la consulta no altera operaciones, tenencias, reservas, publicaciones, cotizaciones ni saldos |
| 35 | Posición con doble conteo | Cada operación confirmada cuenta una vez por rol: quien compra y quien vende tienen cada uno su recuento |

---

## 4. Comprobaciones manuales de contraste

| Comprobación | Resultado esperado |
| --- | --- |
| Los importes de la respuesta son enteros | Un valor como `12550` representa 125,50 créditos; no aparece ningún decimal en los importes |
| Los porcentajes tienen dos decimales | `12.50` representa un 12,50 %; un denominador cero produce `null`, no `0.00` |
| La emisión inicial aparece en el portfolio del superusuario | Posición abierta con 100 tokens, costo de adquisición cero y recuento de compras en cero |
| El resumen ignora el filtro y la página | Comparar la misma consulta con y sin `status` devuelve el mismo `summary` |
| Una consulta no altera el mercado | El estado de operaciones, tenencias, reservas y cotizaciones es idéntico antes y después |

---

## 5. Resultado esperado

- Todas las categorías de prueba de la feature pasan: Controller, Service y Repository.
- La clasificación por etiquetas es válida (`validateTestClassification`).
- El contrato HTTP de `contracts/portfolio-api.md` se cumple campo por campo, incluidos los nombres,
  las unidades, la nulabilidad y los códigos de error.
- La suite completa no presenta regresiones en el comportamiento del mercado.

La **ejecución** y la **lectura de sus resultados** corresponden al usuario: la fase de Plan no ejecuta
pruebas ni afirma resultados.

---

## 6. Referencias

- Especificación funcional: [`../spec.md`](../spec.md)
- Contrato HTTP: [`../contracts/portfolio-api.md`](../contracts/portfolio-api.md)
- Modelo de datos: [`../data-model.md`](../data-model.md)
- Decisiones de diseño: [`../research.md`](../research.md)
- Plan: [`../plan.md`](../plan.md)
- Reglas de testing del backend: `docs/backend/testing.md`
