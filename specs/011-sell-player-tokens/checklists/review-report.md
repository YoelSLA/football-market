# Informe de revisión de checklists — SPEC-011

**Revisión**: calidad de requisitos y diseño documentado de `011-sell-player-tokens`
**Fecha**: 2026-10-17 (revisión inicial) · **Actualizado**: 2026-10-17 (cierre documental de pendientes) · **Cierre final**: 2026-10-17 (CHK009)
**Alcance**: los tres checklists contra `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/` y `quickstart.md`, con lectura de contraste de SPEC-010 y del código pertinente.
**Naturaleza**: revisión documental. No se evaluó implementación ejecutada; no existen `tasks.md`, migraciones, pruebas ni anotaciones OpenAPI para esta feature.

Este informe registra estados y evidencias. **No alteró el texto de los criterios** de los checklists.

---

## 1. Veredicto

**APTO PARA TASKS**

Los once pendientes quedaron cerrados con evidencia documental. **CHK009** se resolvió tras la
confirmación explícita del usuario del fundamento de negocio del rango 1–100, incorporada en
`spec.md` (FR-004 y CL-18) y verificada como compatible con el enunciado, SPEC-010 y la restricción de
base de datos. No quedan pendientes.

No se reabrieron las decisiones D-01, D-02, D-03 ni las decisiones de Clarify asentadas en `spec.md`.
La decisión funcional sobre la frontera de expiración tomada por el usuario quedó incorporada y
armónica en `spec.md`, `data-model.md` y `quickstart.md`.

---

## 2. Resumen por checklist

| Checklist | Total real | CUMPLE | NO CUMPLE | NO APLICA | NO VERIFICABLE | `[x]` marcados |
|---|---|---|---|---|---|---|
| `requirements-quality.md` | 44 | 44 | 0 | 0 | 0 | 44 |
| `api-contract.md` | 38 | 38 | 0 | 0 | 0 | 38 |
| `concurrency.md` | 41 | 41 | 0 | 0 | 0 | 41 |
| **Global** | **123** | **123** | **0** | **0** | **0** | **123** |

### Antes y después por ítem

| Checklist | ID | Antes | Después | Evidencia aportada |
|---|---|---|---|---|
| rq | CHK009 | NO CUMPLE | CUMPLE | Decisión funcional confirmada por el usuario e incorporada en `spec.md` FR-004 (fundamento del rango) y CL-18. Verificada su compatibilidad con `tp/enunciado.md` §3.3, SPEC-010 FR-016 y la restricción `initial_quantity = 100` de `V10__create_market_domain.sql` |
| rq | CHK030 | NO CUMPLE | CUMPLE | `spec.md:154` (FR-010) declara la frontera `now >= expiresAt`; `spec.md:130` la repite en casos límite; `data-model.md:238-243` la deriva de `expires_at > :now` |
| rq | CHK039 | NO CUMPLE | CUMPLE | `spec.md` §Supuestos: supuesto de cuenta de créditos por usuario y comportamiento ya decidido `409 LISTING_NOT_AVAILABLE` sin mutación (remite a FR-018) |
| rq | CHK042 | NO CUMPLE | CUMPLE | `plan.md:186` corregido a FR-032; FR-031 verificado como la prohibición de `POST /api/listings`; resto de referencias comprobadas |
| rq | CHK043 | NO CUMPLE | CUMPLE | `spec.md:3` reescrito; estado verificado con comandos de solo lectura: rama activa y existente. No se afirma upstream en el texto |
| ac | CHK011 | NO CUMPLE | CUMPLE | `contracts/listings.md:50-55`: `201 Created` fundamentado en la convención de SPEC-001 FR-010, con la divergencia del cuerpo declarada y acotada |
| ac | CHK012 | NO CUMPLE | CUMPLE | `contracts/listings.md:213-223`: tabla estado efectivo / estado almacenado / `closedAt` para los cuatro estados, con `EXPIRED` sin cierre persistido |
| co | CHK006 | NO CUMPLE | CUMPLE | `spec.md` FR-020 remite a `research.md` R-01 y al flujo de `plan.md` sin duplicar detalle de implementación |
| co | CHK010 | NO CUMPLE | CUMPLE | `spec.md` §Supuestos: nivel de aislamiento declarado como supuesto explícito; verificado que no hay configuración explícita en `application.yml`, `.properties` ni `@Transactional(isolation=…)` |
| co | CHK011 | NO CUMPLE | CUMPLE | `plan.md:285-290`: definición precisa de «serializa», con alcance por registro y negación de serialización global |
| co | CHK031 | NO CUMPLE | CUMPLE | `spec.md:131` caso límite OBLIGATORIO con la regla de no revalidación final; `quickstart.md:249-268` subescenario 9b |

**Verificación de recuento**: el total real es **123**, coincide con lo informado (44 + 38 + 41).
Numeración continua y sin duplicados en los tres archivos: `CHK001`–`CHK044`, `CHK001`–`CHK038`,
`CHK001`–`CHK041`. Los subtotales suman exactamente el total de cada archivo.

---

## 3. Matriz de pendientes

**No quedan pendientes.** Los once hallazgos constan en la tabla de antes/después de la sección 2 con
su evidencia y su cierre. Ninguno fue BLOQUEANTE ni IMPORTANTE.

### Decisión funcional confirmada para CHK009

El usuario confirmó el fundamento del rango 1–100 tokens por publicación:

- **Límite superior**: en el modelo actual del mercado cada jugador tiene como máximo 100 tokens en
  circulación, por lo que una publicación no puede ofrecer más de 100 tokens de un mismo jugador.
- **Límite inferior**: una publicación debe contener una cantidad positiva de tokens.

El hallazgo se cerró **documentando el fundamento de una regla ya existente**, sin introducir reglas
de emisión, sin modificar el suministro de tokens y sin establecer restricciones adicionales.

**Evidencia de compatibilidad verificada**:

| Fuente | Comprobación |
|---|---|
| `tp/enunciado.md:123` | «Cada jugador tendrá un total de 100 tokens emitidos» |
| SPEC-010 `FR-016` | Emisión inicial única de 100 tokens por jugador, sin reposición del inventario vendido |
| `V10__create_market_domain.sql:57` | `CHECK (initial_quantity = 100)`: la base de datos impide cualquier emisión distinta |

**Contradicciones verificadas: ninguna**. La explicación no altera la cantidad máxima en circulación,
ni la disponibilidad del vendedor (FR-006), ni las reservas de publicaciones activas, ni la
possibilidad de múltiples publicaciones simultáneas del mismo jugador, que sigue limitada por la
disponibilidad real y no por el rango.


## 4. Auditoría transversal focalizada

### 4.1 Concurrencia

Verificado que `research.md:14-24`, `plan.md:51,280,303` y `data-model.md:202` enuncian **el mismo**
orden global `user_credits → token_listings → player_tokens`, y que los cuatro flujos lo respetan como
subconjunto ordenado. La contradicción detectada en la auditoría de Plan (H-01), donde la compra
adquiría la publicación antes que las cuentas, está **resuelta**: `plan.md:303-330` documenta la
lectura inicial sin bloqueo (paso 3), el bloqueo de cuentas (paso 4), el bloqueo de la publicación
(paso 5) y la **revalidación de identidad del vendedor bajo bloqueo** (paso 6).

Compra contra compra y compra contra cancelación se serializan en el paso 5, que retiene la fila hasta
el commit; el perdedor revalida y recibe `409`. Verificado.

El alcance de la garantía está acotado y declarado: `research.md:48-53` afirma que no constituye una
demostración general de ausencia de interbloqueos y enumera qué no cubre. Los bloqueos fuera del
orden global —`FOR KEY SHARE` sobre `users` y `players` al crear la primera tenencia— están
identificados y justificados en `plan.md:331-337`. El límite de lo que la verificación concurrente
puede demostrar está declarado en `plan.md` §Estrategia de pruebas, en coherencia con el precedente de
SPEC-010.

### 4.2 DTO compartido

`OrderBuyRequestDTO` declara hoy `@NotNull` en `playerId` y `quantity`
(`backend/src/main/java/footballmarket/controllers/dtos/requests/OrderBuyRequestDTO.java:14,21`) y
aplica `StrictIntegerDeserializer` a `quantity`. Verificado que ese deserializador es
`ValueDeserializer<Integer>` (`:24`), por lo que **no** puede aplicarse a un campo `Long`.

`plan.md:242-266` documenta esto con precisión: el criterio estricto se reutiliza, pero requiere una
variante tipada para `Long` como componente nuevo. No afirma una integración existente.

Exclusividad de modalidades: `contracts/buy-listing.md:24-31` define la tabla de las cuatro
combinaciones —solo directa, solo secundaria, ambas, ninguna— y `plan.md:226-240` la replica como
regla de validación por modalidad, preservando el `400 INVALID_REQUEST` de la compra directa cuando
falte `playerId` o `quantity`. Verificado que `specs/010-buy-tokens/contracts/buy-tokens.md:76` sigue
estableciendo ese rechazo; la lista de archivos a actualizar está en `plan.md:209-224` con rutas
reales verificadas.

### 4.3 OpenAPI

`plan.md:186-207` define anotaciones para seis operaciones: las cinco rutas nuevas más la variante
`listingId` de la ruta existente. El inventario real coincide con `contracts/listings.md:5-11` y con
`AVAILABLE_DOCS`. El requisito es **FR-032** (`spec.md:175`), no FR-031 como dice el título de la
sección: ese es el hallazgo CHK042.

### 4.4 Integridad referencial

`data-model.md:43-44` declara `ON DELETE NO ACTION` en ambas claves foráneas, y la tabla de
políticas (`:138-156`) y `research.md` R-08 son coherentes. Verificado que no queda ninguna
`ON DELETE CASCADE` en `data-model.md`. Las consecuencias —eliminación física de un usuario con
actividad de mercado rechazada por PostgreSQL— están documentadas, incluida la interacción con
`market_transactions.buyer_id`/`seller_id`, que en SPEC-010 ya carecían de cascada.

### 4.5 Escenarios de carrera

Cubiertos por requisito o por clasificación explícita: dos compradores simultáneos (FR-020, AC23);
compra contra cancelación (§Edge Cases, OBLIGATORIO); dos publicaciones simultáneas que exceden la
tenencia (AC13); vendedor sin respaldo (FR-021); vendedor sin cuenta (FR-018); publicación frente a
pérdida de visibilidad del jugador (§Edge Cases, clasificado RECOMENDADO no bloqueante).

Único hueco: la expiración de una publicación mientras su compra está en curso, que es CHK031.

### 4.6 `quickstart.md` y trazabilidad

14 escenarios más el subescenario 7b para CL-14, con la precondición declarada explícitamente como
fixture futura y sin SQL sobre datos reales. Verificado que los códigos citados en los escenarios
coinciden con los documentados en los contratos. Trazabilidad de `spec.md` íntegra: AC-001–AC-048,
FR-001–FR-033, SC-001–SC-014, CL-01–CL-17, sin huecos.

---

## 5. Cambios realizados

| Archivo | Cambio |
|---|---|
| `checklists/requirements-quality.md` | 39 marcadores `[x]` |
| `checklists/api-contract.md` | 36 marcadores `[x]` |
| `checklists/concurrency.md` | 37 marcadores `[x]` |
| `checklists/review-report.md` | Creado con este informe |

**Ningún texto de criterio fue reescrito, eliminado ni renumerado.** Los once criterios no cumplidos
permanecen sin marcar.

```
 M specs/011-sell-player-tokens/spec.md        (cambio de sesiones previas, no de esta revisión)
?? specs/011-sell-player-tokens/checklists/    (nuevo)
?? specs/011-sell-player-tokens/contracts/     (previo)
?? specs/011-sell-player-tokens/data-model.md  (previo)
?? specs/011-sell-player-tokens/plan.md        (previo)
?? specs/011-sell-player-tokens/quickstart.md  (previo)
?? specs/011-sell-player-tokens/research.md    (previo)
```

No se editaron documentos fuente de SPEC-011 ni de SPEC-010, ni código, pruebas, SQL, migraciones o
configuración. No se ejecutó Gradle, Flyway ni servicios. No se creó `tasks.md`. No se realizó ninguna
operación de Git que altere el estado; solo consultas de lectura.

---

## 6. Próximo paso

El dictamen es **APTO PARA TASKS**. Los once puntos MENOR de la revisión inicial quedaron cerrados con
evidencia documental, incluido el fundamento del rango de publicación. No quedan pendientes.

El siguiente comando posible es `/speckit.tasks`, que **no se ejecutó**.

### Preguntas pendientes

**Ninguna.** Todas las decisiones funcionales de esta feature están cerradas y documentadas en
`spec.md`: publicación, vigencia y frontera de expiración, códigos de error, filtro de estado,
visibilidad de jugadores, integridad referencial, orden de bloqueos y fundamento del rango de
publicación.
