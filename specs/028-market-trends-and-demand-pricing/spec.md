# Feature Specification: Métricas de tendencias y ajuste de oferta-demanda en cotizaciones

<!-- MUST seguir Constitution (§V) y docs/development/sdd-workflow.md.
Documentación viva: este documento describe exclusivamente el comportamiento funcional
vigente y aprobado del sistema. El historial de cambios, decisiones superadas y requisitos
derogados se delega íntegramente a Git; queda prohibido acumular secciones de antecedentes
históricos o revisiones pasadas en este archivo. Forma parte de la tríada permanente
obligatoria junto a plan.md y tasks.md.
Specify materializa/reanuda identidad mediante create-new-feature.sh; no asignar números
manualmente ni sobrescribir artefactos al usar --reuse. Las US formales nacen aquí,
no de épicas REQ. La existencia de este archivo no completa Specify ni habilita Ready. -->

**Feature Branch**: `feat/028-market-trends-and-demand-pricing` — rama Git de la feature. El directorio de esta spec usa el nombre de spec, `028-market-trends-and-demand-pricing`, sin el prefijo de tipo.

**Created**: 2026-11-23

**Status**: Ready for implementation

<!-- Estados y criterios de transición: ../../specs/README.md. -->

**Input**: SPEC-028 — ofrecer métricas de tendencias y sentimiento del mercado y reflejar oferta y demanda en las cotizaciones; consultar tendencias mediante `GET /api/market/trends`; incorporar el ajuste de oferta-demanda al recálculo habitual que conserva el score deportivo; permitir un recálculo manual separado de todos los jugadores elegibles que aplique únicamente el ajuste de oferta-demanda y no consulte ni aplique scores; respetar límites de variación y precio mínimo en ambos recálculos; distinguir en auditoría el impacto deportivo y el de mercado. Depende de las transacciones y del recálculo de cotizaciones existentes. El frontend queda excluido.

## Clarifications

### Session 2026-11-23

- Q: ¿Qué período de transacciones deben usar los recálculos habitual y exclusivo de mercado para calcular la demanda neta? → A: Ambos recálculos usan la misma ventana configurable de tendencias, predeterminada a 24 horas.
- Q: ¿Cómo se contabiliza una liquidación P2P para las métricas de compra/venta y la demanda neta? → A: Se cuenta solo como compra: suma sus tokens y créditos al volumen de compras y a la demanda del jugador; no se cuenta otra vez como venta por identificar también al vendedor. La demanda neta que ajusta cada cotización se calcula por jugador.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar tendencias del mercado (Priority: P1)

Como usuario autenticado, quiero consultar las métricas de tendencia y sentimiento de mercado para entender cómo evolucionan la oferta, la demanda y la presión de compra o venta.

**Why this priority**: Hace observable la información de mercado que sustenta los ajustes de cotización.

**Independent Test**: Consultar las tendencias con una identidad autenticada y comparar las métricas expuestas con las transacciones y ofertas disponibles del período considerado.

**Acceptance Scenarios**:

1. **Given** datos de mercado disponibles y una identidad autenticada, **When** consulta `GET /api/market/trends`, **Then** recibe los cinco jugadores con mayor volumen de compra, porcentajes de transacciones de compra y venta, sesgo de mercado, volumen total operado y ventana observada.
2. **Given** una solicitud sin autenticación válida, **When** consulta el endpoint, **Then** la solicitud se rechaza sin exponer datos de mercado protegidos.
3. **Given** que todavía no hay actividad suficiente para calcular una tendencia, **When** consulta el endpoint, **Then** la respuesta distingue la ausencia de datos de un sentimiento o una tendencia neutros.

---

### User Story 2 - Incorporar la demanda al recálculo habitual (Priority: P1)

Como responsable del mercado, quiero que el recálculo habitual combine el score deportivo disponible con el ajuste de oferta-demanda para que ambas señales influyan en las cotizaciones sin perder la valoración deportiva existente.

**Why this priority**: Integra la nueva señal en el proceso normal de actualización de precios y preserva el comportamiento deportivo vigente.

**Independent Test**: Ejecutar el recálculo habitual con señales deportivas y de mercado conocidas y verificar que ambas contribuciones se reflejan por separado en el resultado auditable.

**Acceptance Scenarios**:

1. **Given** jugadores elegibles con score deportivo y señales de oferta-demanda, **When** se ejecuta el recálculo habitual, **Then** la cotización incorpora ambos impactos y la auditoría los distingue.
2. **Given** un cambio combinado que excedería el límite de variación o dejaría el precio bajo el mínimo, **When** se calcula la cotización, **Then** se aplica el límite vigente y el precio nunca queda por debajo del mínimo.
3. **Given** que el recálculo existente no dispone de score deportivo aplicable para un jugador, **When** se ejecuta el recálculo habitual, **Then** se conserva exactamente el comportamiento existente para ese caso y el operador puede aplicar oferta-demanda mediante el endpoint manual exclusivo de mercado.
4. **Given** una demanda neta de 10 tokens, **When** se calcula el ajuste de mercado, **Then** el factor de mercado es +5% antes de aplicar el límite general de variación.
5. **Given** una demanda neta de −20 tokens, **When** se calcula el ajuste de mercado, **Then** el factor de mercado es −10% antes de aplicar el límite general de variación.

---

### User Story 3 - Recalcular cotizaciones solo por mercado (Priority: P2)

Como operador autorizado, quiero iniciar un recálculo manual para todos los jugadores elegibles basado exclusivamente en oferta-demanda, sin depender de valoraciones deportivas.

**Why this priority**: Permite actualizar el mercado cuando cambia la actividad de compra y venta aunque no haya nuevos scores deportivos.

**Independent Test**: Ejecutar el recálculo manual con datos de mercado disponibles y scores deportivos ausentes o distintos, y verificar que solo la señal de mercado determina el ajuste.

**Acceptance Scenarios**:

1. **Given** jugadores elegibles y actividad de mercado, **When** un operador autorizado invoca `POST /api/quotes/recalculate/market-demand`, **Then** se procesan todos los jugadores elegibles y se aplica únicamente el ajuste de oferta-demanda.
2. **Given** que hay scores deportivos disponibles, **When** se ejecuta el recálculo exclusivamente de mercado, **Then** no se consultan ni aplican dichos scores.
3. **Given** una variación de mercado fuera del límite permitido o un resultado inferior al precio mínimo, **When** se calcula una cotización, **Then** se limita el movimiento y se conserva el precio mínimo.
4. **Given** que la ejecución finaliza, **When** se consulta su resultado de auditoría, **Then** pueden distinguirse el precio anterior, el impacto de mercado y el precio resultante, sin atribuirle impacto deportivo.

### Edge Cases

- No hay transacciones en el período: la consulta devuelve `topHotPlayers: []`, volumen total cero y porcentajes/sesgo nulos; la señal de mercado es factor cero en ambos recálculos. El recálculo solo de mercado no altera precios y registra su ejecución. El recálculo habitual aún puede variar el precio por el componente deportivo si existe score; sin score conserva el comportamiento existente.
- La demanda neta es cero con actividad de compra y venta: no aplicar ajuste de mercado; distinguirlo de la falta total de actividad en la consulta.
- La demanda neta alcanza +100 o −100 tokens: el factor de mercado alcanza +50% o −50%, respectivamente, antes de los límites generales y del piso mínimo.
- El número de transacciones de compra y venta es igual, pero difiere el volumen de tokens: `marketBias` es `NEUTRAL` según el conteo de transacciones, aunque el ajuste de cotización suba o baje según los tokens netos.
- El precio calculado por cualquiera de los recálculos queda por debajo del mínimo: persistir como mínimo el precio mínimo vigente.
- El movimiento calculado supera el límite permitido: limitar la variación conforme a las reglas existentes.
- El jugador no es elegible para cotización: excluirlo de ambos recálculos y reflejarlo en el resultado de la ejecución.
- El jugador no tiene score deportivo aplicable durante el recálculo habitual: se conserva el comportamiento existente sin score; el ajuste de mercado independiente se solicita mediante el endpoint manual solo de mercado.
- Falla el recálculo de un jugador: no producir una cotización que atribuya impactos incompletos y mantener el tratamiento de fallos del recálculo existente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST exponer `GET /api/market/trends` para consultar métricas de tendencia y sentimiento de mercado a usuarios autenticados, usando las reglas de autorización existentes aplicables a la consulta.
- **FR-002**: La respuesta de tendencias MUST identificar el período observado e informar los cinco jugadores con mayor volumen de compra (`topHotPlayers`), incluyendo tokens y créditos; el sentimiento MUST incluir porcentajes de transacciones de compra y venta (`buyTransactionsPercentage`, `sellTransactionsPercentage`) calculados sobre el número de transacciones, y el sesgo del mercado (`marketBias`: compras mayores que ventas = `BULLISH`, ventas mayores que compras = `BEARISH`, volúmenes iguales = `NEUTRAL`); el volumen total MUST incluir créditos y tokens operados. MUST distinguir la ausencia total de transacciones de un sentimiento neutral.
- **FR-003**: Las métricas MUST agregarse desde `market_transactions` sobre una ventana configurable cuyo valor predeterminado es las 24 horas anteriores a la consulta. Esta misma ventana configurable MUST usarse para calcular la demanda neta de ambos recálculos, tomando como referencia el momento de ejecución de cada recálculo. MUST derivar el sentimiento de los porcentajes de compras y ventas. Si no hay transacciones en la ventana, la respuesta MUST devolver `topHotPlayers` vacío, volumen total cero y porcentajes y `marketBias` nulos; MUST NOT presentarse la ausencia de actividad como sentimiento neutral.
- **FR-004**: Cuando el recálculo habitual existente dispone de un score deportivo aplicable, MUST conservar su uso, calcular la demanda neta como `tokensComprados - tokensVendidos` en la ventana configurable definida por FR-003 e incorporar adicionalmente el ajuste de mercado. El factor de mercado MUST calcularse como `clamp((demandaNeta / 100) × 0,50, −0,50, +0,50)`, donde una demanda neta de +100 tokens equivale a +50% y −100 tokens equivale a −50%. El multiplicador combinado MUST ser `sportMultiplier * (1 + marketDemandFactor)`. El límite general de variación existente y el precio mínimo MUST aplicarse al resultado combinado. Cuando no haya score deportivo aplicable, MUST conservarse el comportamiento existente sin score; el ajuste independiente de mercado se realiza mediante FR-006.
- **FR-005**: El recálculo habitual MUST conservar por separado la contribución deportiva y la contribución de mercado para que la auditoría permita distinguirlas y reconstruir su impacto en el precio.
- **FR-006**: El sistema MUST exponer `POST /api/quotes/recalculate/market-demand`, protegido por JWT y la autorización administrativa vigente para operaciones del mercado, para recalcular las cotizaciones de todos los jugadores elegibles usando exclusivamente el ajuste de oferta-demanda sobre el precio vigente y la demanda neta de la ventana configurable definida por FR-003; MUST NOT consultar ni aplicar scores deportivos durante esa operación.
- **FR-007**: Una demanda neta de cero MUST producir un factor de mercado cero. Sin transacciones en la ventana, el recálculo solo de mercado MUST dejar las cotizaciones sin cambios y registrar el resultado de la ejecución; el recálculo habitual MUST aplicar el componente deportivo si dispone de score y MUST conservar el comportamiento existente si no lo dispone. Ambos recálculos MUST respetar el límite de variación por período y el precio mínimo definidos por el sistema de cotizaciones existente.
- **FR-013**: Los porcentajes de transacciones y `marketBias` MUST basarse en el conteo de transacciones de compra y venta. El factor de recotización MUST basarse en el volumen neto de tokens. Por ello, igual cantidad de transacciones puede producir `marketBias: NEUTRAL` y, a la vez, un ajuste no nulo de cotización si los volúmenes de tokens difieren.
- **FR-008**: La auditoría de cada cotización MUST permitir diferenciar el precio anterior, el impacto deportivo cuando aplique, el impacto de mercado, los límites aplicados y el precio resultante. El recálculo solo de mercado MUST identificar que no aplicó score deportivo.
- **FR-009**: El endpoint de recálculo manual MUST requerir JWT válido y la autorización administrativa vigente para operaciones del mercado; MUST NOT introducir un rol nuevo.
- **FR-010**: La operación manual MUST procesar la población completa de jugadores elegibles en una única solicitud y registrar el origen y el resultado de la ejecución para su auditoría, siguiendo el contrato de ejecución de cotizaciones existente.
- **FR-011**: Los recálculos MUST utilizar las transacciones y registros de mercado existentes como fuente de actividad; MUST NOT alterar las transacciones ya confirmadas.
- **FR-012**: La funcionalidad MUST limitarse al backend; vistas, gráficos y controles de frontend quedan fuera de alcance.

### Key Entities *(include if feature involves data)*

- **Métrica de tendencia de mercado**: medida de actividad de oferta, demanda o presión de compra/venta asociada a un período.
- **Ajuste de mercado**: contribución derivada de oferta-demanda que participa en la cotización independientemente del score deportivo.
- **Cotización recalculada**: precio resultante de un recálculo habitual con impactos deportivo y de mercado distinguibles, o de un recálculo manual con solo impacto de mercado.
- **Ejecución de recálculo**: operación identificable que procesa jugadores elegibles y registra el tipo de recálculo, sus parámetros y resultado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las consultas autenticadas a `GET /api/market/trends` informa los cinco jugadores con mayor volumen de compra, porcentajes de transacciones de compra/venta, sesgo de mercado, volumen total de tokens y créditos y período observado; las solicitudes no autenticadas no reciben esos datos.
- **SC-002**: El 100% de las cotizaciones del recálculo habitual permite distinguir el impacto deportivo del impacto de mercado.
- **SC-003**: El 100% de las cotizaciones del recálculo manual solo de mercado se calcula sin consultar ni aplicar scores deportivos y permite identificarlo en auditoría.
- **SC-004**: Ninguna cotización producida por cualquiera de los dos recálculos excede el límite de variación vigente ni queda por debajo del precio mínimo.
- **SC-005**: Cada recálculo manual finalizado informa el resultado del procesamiento de todos los jugadores elegibles conforme al contrato de ejecución existente.

## Assumptions

- Se reutilizan autenticación, autorización, transacciones, cotizaciones, límites de variación, precio mínimo y mecanismo de recálculo existentes; sus contratos vigentes conservan precedencia.
- La operación de recálculo manual de mercado será síncrona y poblacional, siguiendo el modo de ejecución manual de cotizaciones existente.
- El score deportivo continúa siendo un insumo del recálculo habitual y no forma parte del recálculo exclusivo de mercado.
- Las transacciones y el recálculo de cotizaciones existentes son dependencias funcionales; su existencia no implica que la nueva señal de mercado ya esté implementada.
- La ventana para tendencias usa las transacciones confirmadas de las 24 horas anteriores a cada consulta; el volumen se expresa en tokens y el sentimiento se deriva del balance entre compras y ventas.
- La agregación se calcula desde `market_transactions`; la ventana temporal es configurable y su valor predeterminado es 24 horas.
- La ruta del recálculo manual solo de mercado es `POST /api/quotes/recalculate/market-demand`, según los requisitos y criterios de aceptación del issue #96.
- El límite del factor de mercado quedó confirmado en ±50% respecto del precio de referencia; además siguen aplicándose el límite de variación general y el precio mínimo existentes.
- El factor escala linealmente desde demanda neta −100..+100 tokens hasta −50%..+50%; demanda neta cero no añade ajuste de mercado.
- Si el volumen de transacciones produce una demanda neta fuera del rango −100..+100, el factor queda igualmente acotado por `clamp` a −50%..+50%.
- SPEC-016 — recálculo manual de cotizaciones — es dependencia del comportamiento de cotización existente. Las operaciones y transacciones existentes del mercado son fuente de datos y dependencia para las métricas.
- Toda UI para mostrar tendencias o iniciar la operación manual queda fuera de esta SPEC.

## Out of Scope

- Componentes, páginas, gráficos u otros cambios del frontend.
- Cambios a la ejecución o auditoría de transacciones ya confirmadas.
- Cambios a la fórmula o persistencia del score deportivo.
- Automatización o programación periódica nueva del recálculo solo de mercado.

## Trazabilidad

- **Issue**: [#96 — Métricas de tendencias de mercado y factor de demanda en cotizaciones](https://github.com/YoelSLA/football-market/issues/96)
- **Dependencias funcionales**: SPEC-020 / REQ-S2-10 (transacciones persistidas en `market_transactions`) y SPEC-016 / REQ-S2-11-A (recálculo existente de cotizaciones).
- **Fuera de alcance relacionado**: SPEC-029 cubre la experiencia frontend; no forma parte de esta SPEC.
