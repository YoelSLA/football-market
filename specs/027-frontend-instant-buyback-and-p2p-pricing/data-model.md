# Data Model: UI de venta inmediata oficial y selector de precio flexible en P2P (SPEC-027)

Este documento define las entidades de datos, interfaces TypeScript, estados de UI y contratos de transferencia (DTO) para la feature **SPEC-027**.

---

## 1. Tipos y Enumeraciones de Dominio

### 1.1. Modalidad de Venta (`SellMode`)
```typescript
export type SellMode = "OFFICIAL" | "P2P";
```
- `"OFFICIAL"`: Venta inmediata al superusuario (buyback) con spread fijo del 10%.
- `"P2P"`: Publicación en el mercado comunitario fijando precio unitario flexible ($\pm 20\%$).

### 1.2. Tipo de Oportunidad P2P (`P2POpportunityType`)
```typescript
export type P2POpportunityType = "DISCOUNT" | "PREMIUM" | "NEUTRAL";

export interface P2POpportunity {
  type: P2POpportunityType;
  percentageDelta: number; // Redondeado al entero más cercano
  label: string; // ej: "🏷️ -10% Oferta" o "🔥 +15% Premium"
}
```

---

## 2. Modelos de Dominio de Frontend

### 2.1. Resultado de Venta Inmediata Oficial (`SellOfficialResult`)
```typescript
export interface SellOfficialResult {
  transactionId: number;
  playerId: number;
  playerName: string;
  quantity: number;
  officialPrice: number;        // en centésimas
  discountPercentage: number;   // 10
  unitPriceNet: number;         // en centésimas
  totalCredits: number;         // en centésimas
  executedAt: string;
}
```

### 2.2. Estimación de Venta Oficial en Tiempo Real (`SellOfficialEstimation`)
```typescript
export interface SellOfficialEstimation {
  quantity: number;
  officialPriceCentimes: number;
  discountPercentage: number;   // 10
  spreadCentimes: number;       // descuento unitario en centésimas
  unitPriceNetCentimes: number; // precio neto por token
  totalNetCentimes: number;     // créditos netos totales a percibir
}
```

### 2.3. Estimación de Publicación P2P Flexible (`SellP2PEstimation`)
```typescript
export interface SellP2PEstimation {
  quantity: number;
  officialPriceCentimes: number;
  unitPriceCentimes: number;    // precio elegido por el usuario
  percentageDelta: number;      // variación porcentual (-20 a +20)
  totalCentimes: number;        // total resultante: quantity * unitPriceCentimes
  opportunityType: P2POpportunityType;
}
```

---

## 3. DTOs de Comunicación HTTP (`features/market/types/dtos.ts`)

### 3.1. Venta Oficial Inmediata

#### Request DTO: `OrderSellOfficialRequestDTO`
```typescript
export interface OrderSellOfficialRequestDTO {
  playerId: number;
  quantity: number;
}
```

#### Response DTO: `OrderSellOfficialResponseDTO`
```typescript
export interface OrderSellOfficialResponseDTO {
  transactionId: number;
  playerId: number;
  quantity: number;
  officialPrice: number;
  discountPercentage: number;
  unitPrice: number;
  totalAmount: number;
  executedAt: string;
}
```

### 3.2. Publicación P2P con Precio Flexible

#### Request DTO: `CreateListingRequestDTO` (actualizado)
```typescript
export interface CreateListingRequestDTO {
  playerId: number;
  quantity: number;
  unitPrice?: number; // centésimas de crédito; opcional (si se omite, usa cotización oficial)
}
```

---

## 4. Estado de UI y Formularios

### 4.1. Formulario del Modal de Venta (`SellTokenFormValues`)
```typescript
export interface SellTokenFormValues {
  quantity: string;
  sellMode: SellMode;
  p2pUnitPriceCentimes: number; // sincronizado con el slider/input
}
```

### 4.2. Estado del Diálogo `SellTokenModal`
```typescript
export interface SellTokenModalState {
  isOpen: boolean;
  selectedPlayer: MarketPlayer | null;
  sellMode: SellMode;
  quantity: number;
  p2pUnitPrice: number;
  isBusy: boolean;
  error: string | null;
  officialResult: SellOfficialResult | null;
  p2pResult: SellResult | null;
}
```

---

## 5. Mapeo de Transición de Estados

```text
[Apertura del Modal]
         │
         ▼
[Modalidad: OFFICIAL (por defecto)] ◄────────► [Modalidad: P2P]
  - Cantidad: 1..availableQuantity               - Cantidad: 1..availableQuantity
  - Spread: 10% fijo                             - Precio: [P_oficial * 0.80 .. P_oficial * 1.20]
  - Desglose: Créditos netos                     - Badge: Oferta / Premium
         │                                              │
         ▼                                              ▼
[Confirmar Venta Oficial]                       [Confirmar Publicación P2P]
  - POST /api/orders/sell-official                 - POST /api/listings (con unitPrice)
  - isBusy = true                                  - isBusy = true
         │                                              │
         ▼                                              ▼
[Éxito Oficial]                                 [Éxito P2P]
  - Pantalla Comprobante                          - Pantalla Comprobante
  - Toast global de éxito                         - Toast global de éxito
  - Invalida queries                              - Invalida queries
```
