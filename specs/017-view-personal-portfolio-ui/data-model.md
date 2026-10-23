# Modelo de datos — SPEC-017: visualización del portfolio personal

**Feature**: SPEC-017 · **Rama**: `feat/017-view-personal-portfolio-ui` · **Fecha**: 2026-10-21
**Plan**: [`../plan.md`](../plan.md) · **Spec**: [`../spec.md`](../spec.md) ·
**Contrato**: [`../contracts/portfolio-consumption.md`](../contracts/portfolio-consumption.md)

Este documento describe los datos que la interfaz consume y los que presenta. **No define reglas
financieras**: el contrato de la consulta es de SPEC-014 y las reglas contables son de SPEC-015. Aquí
solo se describe la forma de los datos tal como llegan y tal como se presentan.

---

## 1. Persistencia

Ninguna. La feature no persiste información: no escribe en `localStorage` ni en `sessionStorage`, y no
mantiene estado remoto propio. La sesión vive en `infrastructure/storage` y el estado remoto en la
caché de TanStack Query, que se limpia al cambiar de identidad.

## 2. Fuente y frontera

| Fuente | Artefacto | Papel |
| --- | --- | --- |
| Contrato HTTP | `specs/014-view-personal-portfolio/contracts/portfolio-api.md` | Contrato autoritativo de `GET /api/portfolio/me`. Esta feature lo consume y no lo reescribe. |
| Reglas contables | `specs/015-define-average-cost-and-financial-results-rules/` | Origen de los valores de costo, resultado y rentabilidad. La interfaz los presenta, no los calcula. |
| Modelo de datos del backend | `specs/014-view-personal-portfolio/data-model.md` §3 | Forma completa de la respuesta. |

## 3. DTO — contrato HTTP

`frontend/src/features/portfolio/types/dtos.ts` refleja la respuesta de `GET /api/portfolio/me`. El
DTO no sale de la frontera del Service.

### 3.1 `PortfolioResponseDTO`

```ts
interface PortfolioResponseDTO {
	summary: PortfolioSummaryResponseDTO;
	positions: PortfolioPositionResponseDTO[];
	pagination: PortfolioPaginationResponseDTO;
}
```

### 3.2 `PortfolioSummaryResponseDTO`

Los importes son enteros en **centésimas de crédito**.

| Campo | Tipo | Nulable | Nota |
| --- | --- | --- | --- |
| `totalPositions` | `number` | No | Posiciones **abiertas**. No depende del filtro ni de la página. |
| `totalTokens` | `number` | No | Total de tokens poseídos. |
| `totalReservedTokens` | `number` | No | Tokens reservados por publicaciones activas y vigentes. |
| `totalAvailableTokens` | `number` | No | `totalTokens − totalReservedTokens`. |
| `totalInvested` | `number` | No | Costo de adquisición de las posiciones **abiertas**. |
| `totalCurrentValue` | `number` | **Sí** | Nulo si alguna posición abierta carece de cotización. |
| `totalRealizedProfitLoss` | `number` | No | Resultado realizado acumulado, incluidos ciclos cerrados. |
| `totalUnrealizedProfitLoss` | `number` | **Sí** | Nulo si alguna posición abierta carece de cotización. |
| `totalProfitLoss` | `number` | **Sí** | Nulo si alguna posición abierta carece de quotation. |
| `totalProfitLossPercentage` | `number` | **Sí** | Puntos porcentuales con dos decimales. Nulo si el denominador contable es cero o falta una cotización. |
| `totalBuyTransactions` | `number` | No | Operaciones confirmadas de compra. |
| `totalSellTransactions` | `number` | No | Operaciones confirmadas de venta. |
| `valuationComplete` | `boolean` | No | `false` si alguna posición abierta carece de cotización. |

### 3.3 `PortfolioPositionResponseDTO`

| Campo | Tipo | Nulable | Nota |
| --- | --- | --- | --- |
| `playerId` | `number` | No | |
| `playerName` | `string \| null` | **Sí** | Nulo cuando el nombre no es recuperable del catálogo. |
| `teamName` | `string \| null` | **Sí** | Nulo cuando el equipo no es recuperable del catálogo. |
| `position` | `string \| null` | **Sí** | Posición deportiva; nula cuando no es recuperable del catálogo. |
| `imageUrl` | `string \| null` | **Sí** | El contrato del portfolio **no** expone `fallbackImageUrl`. |
| `active` | `boolean` | No | El jugador puede estar inactivo y conservar su posición. |
| `status` | `"OPEN" \| "CLOSED"` | No | |
| `valuationStatus` | `"VALUED" \| "UNVALUED" \| "NOT_APPLICABLE"` | No | `NOT_APPLICABLE` si y solo si `status` es `CLOSED`. |
| `quantity` | `number` | No | Tokens poseídos. |
| `reservedQuantity` | `number` | No | Tokens reservados. |
| `availableQuantity` | `number` | No | `quantity − reservedQuantity`. |
| `acquisitionCost` | `number` | No | Costo de adquisición de la posición abierta. **Cero real** en una posición liquidada, no ausencia. |
| `averageUnitPrice` | `number \| null` | **Sí** | Nulo en una posición cerrada: no hay cantidad abierta que lo defina. |
| `currentUnitPrice` | `number \| null` | **Sí** | Nulo sin cotización vigente y en posiciones cerradas. |
| `priceUpdatedAt` | `string \| null` | **Sí** | Instante de la cotización, ISO 8601. |
| `currentValue` | `number \| null` | **Sí** | Nulo sin cotización y en posiciones cerradas. |
| `realizedProfitLoss` | `number` | No | Se conserva aunque la posición no tenga cotización. |
| `unrealizedProfitLoss` | `number \| null` | **Sí** | Nulo sin cotización y en posiciones cerradas. |
| `totalProfitLoss` | `number \| null` | **Sí** | Nulo sin cotización. En una posición cerrada es el resultado realizado acumulado. |
| `unrealizedProfitLossPercentage` | `number \| null` | **Sí** | Nulo sin cotización, con costo de adquisición cero o en posiciones cerradas. |
| `portfolioWeightPercentage` | `number \| null` | **Sí** | Nulo sin cotización, con valor total nulo o cero, o en posiciones cerradas. |
| `openedAt` | `string` | No | Inicio del ciclo de tenencia vigente, ISO 8601; conforme al DTO HTTP de SPEC-014. |
| `closedAt` | `string \| null` | **Sí** | Nulo en una posición abierta. |
| `lastTransactionAt` | `string \| null` | **Sí** | Nulo si la posición no tiene ninguna operación confirmada. |
| `totalBuyTransactions` | `number` | No | |
| `totalSellTransactions` | `number` | No | |

### 3.4 `PortfolioPaginationResponseDTO`

| Campo | Tipo | Nulable |
| --- | --- | --- |
| `page` | `number` | No |
| `size` | `number` | No |
| `totalElements` | `number` | No |
| `totalPages` | `number` | No |

## 4. Model — conceptos de la interfaz

`frontend/src/features/portfolio/types/models.ts` define lo que la interfaz presenta. Mantiene la
separación con el DTO aunque los campos coincidan (§1.8 de la arquitectura).

### 4.1 `PortfolioSummary`

Contiene los trece indicadores del resumen, con la misma nulidad del contrato. Los importes se
mantienen **en centésimas**: la conversión a créditos ocurre en la capa de presentación, no en el
Mapper.

`totalPositions` representa posiciones **abiertas** y se presenta con un rótulo explícito que lo
distingue del total de posiciones listadas en la vista de tenencias (DC-09).

### 4.2 `PortfolioPosition`

`playerName`, `teamName` y `position` conservan el tipo `string | null` del DTO.
La presentación señala explícitamente el dato ausente sin inventar una identidad ni alterar los
datos financieros; el retrato conserva el avatar genérico y un texto alternativo genérico cuando
el nombre no está disponible. Esta ausencia no elimina la posición de la lista ni de su detalle.

Contiene la identidad, la tenencia, la inversión, la actividad y los resultados de una posición. Se
modelan tres uniones literales:

| Unión | Valores | Origen |
| --- | --- | --- |
| `PositionStatus` | `OPEN` · `CLOSED` | `status` del contrato |
| `ValuationStatus` | `VALUED` · `UNVALUED` · `NOT_APPLICABLE` | `valuationStatus` del contrato |
| `PositionFilter` | `ALL` · `OPEN` · `CLOSED` | Filtro de la interfaz; `ALL` no se envía al backend |

`PositionFilter` es un concepto de la interfaz, no del contrato: `ALL` no tiene equivalente en la API
y se traduce a la ausencia del parámetro `status`.

### 4.3 `Portfolio`

```ts
interface Portfolio {
	summary: PortfolioSummary;
	positions: PortfolioPosition[];
	pagination: PortfolioPagination;
}
```

`PortfolioPagination` replica los metadatos de la página solicitada.

## 5. Transformación tipada del Mapper

El Service recibe `PortfolioResponseDTO` y el Mapper transforma DTO → Model mediante asignaciones
explícitas. Conserva los trece indicadores, los campos de cada posición, la paginación, los valores
nulos y el orden. No valida estructuras en runtime ni recompone cantidades, porcentajes o estados.
La coherencia de las cifras y de la paginación es responsabilidad del backend y de su contrato.
DTO y Model permanecen tipos separados; la transformación no introduce normalizaciones financieras.
El tipado no detecta respuestas malformadas en runtime: no se promete su rechazo preventivo.

## 6. Estados de la posición y sus efectos en la presentación

| Estado | `valuationStatus` | Precio actual | Valor actual | Resultado no realizado | Resultado total | Precio medio |
| --- | --- | --- | --- | --- | --- | --- |
| Abierta con cotización | `VALUED` | Importe | Importe | Importe | Importe | Importe |
| Abierta sin cotización | `UNVALUED` | *Sin cotización* | — | — | — | Importe |
| Cerrada | `NOT_APPLICABLE` | — | — | — | Resultado realizado acumulado | — |

La distinción entre «abierta sin cotización» y «cerrada» no es solo de los datos: cambia el texto que
se presenta en el precio actual. *Sin cotización* solo corresponde a una posición abierta.

## 7. Transformación de presentación

Las funciones puras de `utils/` translate los Model a texto. No modifican el valor: solo lo representan.

| Función | Entrada | Salida | Ausencia |
| --- | --- | --- | --- |
| `formatCredits` | centésimas `number \| null` | `10,00` · `-1,80` | `—` |
| `formatPercentage` | puntos porcentuales `number \| null` | `12,50 %` | `—` |
| `formatDateTime` | ISO 8601 `string \| null` | `20/10/2026 16:22` | `—` |

El guion representa la ausencia de dato y es visualmente distinto de `0,00`. El signo se conserva en
el valor.

## 8. Referencias

- Especificación funcional: [`../spec.md`](../spec.md)
- Contrato de consumo: [`../contracts/portfolio-consumption.md`](../contracts/portfolio-consumption.md)
- Plan: [`../plan.md`](../plan.md)
- Contrato HTTP de origen: `specs/014-view-personal-portfolio/contracts/portfolio-api.md`
- Modelo de datos de origen: `specs/014-view-personal-portfolio/data-model.md` §3
- Reglas contables: `specs/015-define-average-cost-and-financial-results-rules/`
