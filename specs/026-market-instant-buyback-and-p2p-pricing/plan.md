# Plan de implementación: Venta inmediata y precio flexible P2P (SPEC-026)

**Rama**: `feat/026-market-instant-buyback-and-p2p-pricing` | **Fecha**: 2026-11-22 | **Spec**: [spec.md](spec.md)

<!-- Documentación viva: diseño vigente. El historial de decisiones se delega a Git.
Este plan no genera Tasks ni habilita Implement. -->

## Resumen

Extender el mercado backend para que un usuario pueda vender tokens libres al superusuario con
comisión configurable, y fijar opcionalmente un precio en publicaciones P2P dentro de la banda
80%-120% de la cotización vigente. Se reutilizan las cuentas, tenencias, cotizaciones, publicaciones
y transacciones existentes. La venta oficial será un nuevo caso de uso transaccional expuesto en
`POST /api/orders/sell-official`; la publicación existente conserva `POST /api/orders/sell`.
Las liquidaciones registran en forma inmutable cotización de referencia, porcentaje descontado,
precio neto, importe y estados antes/después. No se agregan dependencias ni un frontend.

## Contexto técnico

| Aspecto | Decisión vigente |
|---|---|
| Lenguaje/plataforma | Java 21; backend REST en Spring Boot 4.1.1. |
| Dependencias principales | Spring Web MVC, Spring Data JPA/Hibernate, Spring Security, Jakarta Validation, Flyway y PostgreSQL. Se reutilizan `QuoteSource`, `MarketSettlementExecutor` y el dominio de mercado; sin dependencias nuevas. |
| Persistencia | PostgreSQL. Se amplían `market_transactions` con los datos de cotización bruta y comisión necesarios para auditar el buyback; no se requiere una tabla de configuración ni una entidad de dominio nueva. |
| Verificación | Tests backend según `docs/backend/testing.md`; los escribe el equipo y los ejecuta el usuario. El agente no ejecuta tests. |
| Tipo y alcance | API REST backend; dos flujos existentes afectados (publicación P2P y transacción financiera) y un endpoint nuevo. |
| Objetivos de rendimiento | No hay SLA específico para estas operaciones. No se introduce optimización ni requisito de rendimiento adicional. |
| Restricciones | Una única transacción para fondos, tokens y registro; importes enteros en centésimas y aritmética exacta; bloqueo de cuentas antes de tenencias y orden ascendente de propietarios; preservar sin cambios la compra oficial existente. El superusuario solo puede pagar buybacks si su cuenta dispone de créditos. |
| Escala | Cantidad por operación entre 1 y 100; se conserva el volumen y los límites del mercado existente. |

## Constitution Check — antes del diseño

| Principio | Resultado | Fundamento |
|---|---|---|
| §I Simplicidad y calidad | PASS | Reutiliza el límite de mercado y la liquidación existentes; no introduce servicios financieros genéricos, tablas de configuración ni infraestructura nueva. |
| §II Proporcionalidad | PASS | Solo cubre FR-001–FR-018 y sus escenarios; la falta de saldo del superusuario se rechaza, sin crear un mecanismo de fondeo. |
| §III Seguridad e integridad | PASS | Identidad/rol desde usuario persistido; transacción completa con bloqueos, validaciones y rollback. No se confía en identidades del body. |
| §IV Coherencia de contratos | PASS condicionado a implementación sincronizada | Contrato de SPEC-026, DTO, OpenAPI, entidad/migración, pruebas de Controller/Service/persistencia y contratos afectados de SPEC-010/011 deberán actualizarse juntos. |
| §V Trazabilidad | PASS | SPEC-026 es el origen; los contratos de esta Feature y las fuentes vigentes de SPEC-010/011 reflejarán las superficies compartidas. |
| §VI Mínimo alcance | PASS | Sin cambios al frontend, compra oficial existente, cotizaciones ni mecanismo de saldo. |
| Arquitectura backend | PASS | Controller → Mapper → Service; el caso de uso usa Model, Repository e Integration autorizados. No se agrega dependencia Service → Service ni Orchestrator. |
| Testing / idioma | PASS | Pruebas backend pertinentes, no ejecutadas por el agente; documentación en español y código nuevo en inglés. |

## Estructura del proyecto

```text
specs/026-market-instant-buyback-and-p2p-pricing/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    ├── official-buyback.md
    └── p2p-listing-price.md

backend/src/main/java/footballmarket/features/market/
├── application/                 # interfaz e implementación del caso de venta oficial
├── application/internal/        # ejecución interna y liquidación reutilizada
├── domain/models/               # MarketTransaction y, solo si es necesario, regla de comisión
├── domain/records/               # request/receipt de la operación
├── infraestructure/              # repositorios de saldo/tenencias/listados existentes
└── presentation/                 # Controller, DTO, Mapper, OpenAPI y traducción de errores

backend/src/main/resources/db/migration/
└── V<timestamp-UTC>__add_buyback_audit_fields.sql
```

La estructura física sigue los roles y paquetes vigentes de `features/market`; no se crean
directorios alternativos ni se mueve código existente.

## Decisiones de diseño

1. **Rutas separadas**: `POST /api/orders/sell` mantiene la publicación P2P; la venta oficial se
   expone mediante `POST /api/orders/sell-official`, sin reinterpretar ni romper el contrato actual.
2. **Liquidación inversa con los bloqueos existentes**: la operación identifica al usuario desde el
   principal autenticado y al superusuario desde persistencia; valida rol, cantidad, visibilidad,
   mercado, cotización, comisión y tenencia libre. Bloquea cuentas por `user_id` ascendente y luego
   tenencias por propietario ascendente. Después de bloquear la tenencia del vendedor, revalida la
   suma de reservas vigentes y su disponibilidad libre. Revalida también el saldo del superusuario.
   El superusuario ocupa el rol de comprador y el usuario autenticado el de vendedor para reutilizar
   `MarketSettlementExecutor.transfer`: debita/crédita cuentas y transfiere tokens del vendedor al
   comprador. Los movimientos y el registro se ejecutan en el mismo límite `@Transactional`; un
   fallo revierte todo.
3. **Comisión entera configurable**: property de aplicación con unidad de porcentaje entero,
   defecto `10`, rango inclusivo `10..99`. Un valor fuera del rango hace fallar la configuración
   antes de aceptar una venta. El precio neto por token se obtiene con aritmética entera exacta y
   redondeo half-up (mitades hacia arriba) antes de multiplicar por la cantidad. Si el redondeo
   produce cero, la venta se rechaza sin cambios. Un saldo insuficiente del superusuario también
   produce rechazo total.
4. **Auditoría compatible con el historial**: `unit_price` continúa representando el precio realmente
   pagado; se agregan columnas nullable `quote_price` y `discount_percentage` a
   `market_transactions`. Aunque el nombre físico `discount_percentage` se conserva, su significado
   en la venta oficial es la comisión aplicada, entre 10 y 99 inclusive. Los registros históricos y
   las operaciones a las que no aplica esta comisión conservan `NULL`; el factory de la venta
   oficial exige ambos datos. No se altera ni se renombra ninguna columna aplicada. La respuesta de
   venta expone las partes y las condiciones financieras exigidas.
5. **Precio P2P opcional y congelado**: añadir `unitPrice` opcional al request actual. Si se omite se
   utiliza la cotización vigente; si se informa, se exige entero JSON positivo y se valida la banda
   inclusiva 80%-120% en comparación exacta, sin convertir la cotización en otro redondeo. La
   publicación persiste su precio e importe calculado existentes, y su compra continúa liquidando
   esos valores congelados. No cambia el DTO de compra ni su flujo.
6. **Contratos**: los contratos HTTP normativos nuevos viven en `contracts/` de SPEC-026. La
   implementación también sincroniza las secciones afectadas de los contratos vigentes
   `specs/010-buy-tokens/contracts/buy-tokens.md` y
   `specs/011-sell-player-tokens/contracts/listings.md`; no se cambia el alcance funcional de esas
   Features.

## Límites de alcance y consecuencias

- No se permite al superusuario iniciar venta oficial; su cuenta sí actúa como comprador en el
  buyback y debe disponer de saldo suficiente.
- La venta usa tokens libres, descontando reservas P2P según las publicaciones activas y vigentes.
- No se agregan operaciones para financiar al superusuario, cambiar la comisión en runtime,
  cancelar una venta ya liquidada, calcular cotizaciones, idempotencia, UI ni rutas alternativas.
- La lectura de historial y la reconstrucción financiera ya interpretan al usuario como vendedor
  cuando su ID coincide con `seller_id`; su comportamiento se verifica para buyback y se modifica
  solo si esas pruebas revelan una incompatibilidad.
- El precio solicitado P2P es dato HTTP; su validación estructural pertenece al DTO y la banda
  funcional al Service/Model según las responsabilidades existentes.
- La respuesta de error de autenticación sigue siendo la de la cadena de seguridad vigente; los
  errores controlados de la aplicación conservan `ErrorResponseDTO`.
- **Alineación de implementación**: la configuración de comisión y las invariantes de la transacción
  aplican el rango inclusivo 10–99; el comprobante documenta el mismo rango. Las tareas T028–T033,
  que cubren estos límites y el rechazo por precio neto redondeado a cero, figuran completadas en
  `tasks.md`. La ejecución de tests y las aprobaciones humanas se rigen por sus gates correspondientes
  y no se infieren de este estado.

## Constitution Check — reevaluación tras diseño

| Principio | Resultado | Fundamento del diseño |
|---|---|---|
| §I Simplicidad y calidad | PASS | No hay nueva dependencia ni entidad/tipo de persistencia independiente; se extiende el registro financiero y se reutiliza la infraestructura de mercado. |
| §II Proporcionalidad | PASS | Los campos de auditoría, el endpoint diferenciado y la banda de precio son necesarios para requisitos expresos y para no colisionar con la publicación vigente. |
| §III Seguridad e integridad | PASS | Orden de bloqueos coherente; comprobaciones bajo bloqueo; transacción cubre transferencias y auditoría. Los rechazos no producen efectos parciales. |
| §IV Contratos | PASS condicionado a implementación | Migración, entidad, contrato de recibo, respuesta HTTP, OpenAPI, pruebas pertinentes y contratos 010/011 sincronizados en el mismo cambio. |
| §V Trazabilidad | PASS | Cambios funcionales y financieros quedan trazables en contratos, recibo y registro persistido. |
| §VI Mínimo alcance | PASS | Sin abstracciones ni capacidad de fondeo adicionales. |
| Reglas backend y testing | PASS | Se respetan roles, DTO record, excepciones propias, documentación OpenAPI y categorías de pruebas; no se ejecutan tests en esta fase. |

## Complejidad

| Excepción o componente considerado | Decisión |
|---|---|
| Dependencia nueva | Ninguna; las existentes bastan. |
| Nueva tabla o servicio de contabilidad | No: la transacción financiera existente cubre la auditoría y la liquidación. |
| Servicio de configuración administrable por usuarios | No: el porcentaje es configuración de sistema, sin UI en el alcance. |
| Mecanismo para fondear al superusuario | No: está fuera de SPEC-026; el saldo insuficiente se rechaza de forma atómica. |

## Dependencias

| Dependencia | Uso |
|---|---|
| SPEC-001/003 | JWT, usuario persistido y rol. |
| SPEC-010 | Mercado oficial, representación monetaria, saldos, tenencias, transacciones y compra existente que debe permanecer intacta. |
| SPEC-011 | Publicaciones P2P, reservas derivadas, precio congelado y compra secundaria. |
| SPEC-015/016 | Fuente canónica de cotización vigente, consumida a través de `QuoteSource`. |
| Flyway / PostgreSQL | Migración aditiva para la auditoría de buyback. |

Las dependencias técnicas no son blockers: son existentes y su orden se determina en TASKS. La fase
PLAN termina aquí; no crea ni actualiza `tasks.md`, no evalúa checklists ni inicia fases posteriores.
