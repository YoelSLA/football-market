# Implementation Plan: UI de venta inmediata oficial y selector de precio flexible en P2P (SPEC-027)

**Branch**: `feat/027-frontend-instant-buyback-and-p2p-pricing` | **Date**: 2026-11-23 | **Spec**: [`spec.md`](spec.md)  
**Estado**: Plan de implementación técnico propuesto.  
**Referencias normativas**:
- [`spec.md`](spec.md) (Especificación viva clarificada)
- [`data-model.md`](data-model.md) (Modelos, estados y DTOs)
- [`research.md`](research.md) (Decisiones de arquitectura técnica)
- [`contracts/sell-official-api.md`](contracts/sell-official-api.md) (Contrato `POST /api/orders/sell-official`)
- [`contracts/p2p-flexible-pricing-api.md`](contracts/p2p-flexible-pricing-api.md) (Contrato `POST /api/listings`)
- [`quickstart.md`](quickstart.md) (Guía de validación de escenarios interactivos)

---

## 1. Summary

Implementación en el frontend (`frontend/src/features/market/`) de la experiencia integral de venta y fijación de precios en el mercado:
1. **Modal de Venta Dinámico (`SellTokenModal`)**:
   - Integración de selector segmentado accesible (`SellModeSelector`) para alternar entre *"Venta Inmediata Oficial"* (contra el superusuario) y *"Publicación en Comunidad P2P"*.
   - En venta oficial inmediata: desglose en tiempo real con cotización oficial, comisión fija del 10%, valor unitario neto y créditos netos a percibir. Ejecución contra `POST /api/orders/sell-official` mediante `useSellOfficialTokensMutation`.
   - En venta P2P: control de fijación de precio interactivo (`P2PPriceSelector`) con slider e input numérico acotados a $[-20\%, +20\%]$ respecto a la cotización oficial, inicializado por defecto en un descuento promocional del -10% para acelerar transacciones. Ejecución contra `POST /api/listings` transmitiendo `unitPrice`.
   - Feedback post-transacción: pantalla de comprobante detallado dentro del diálogo (`SellModalSuccess`) y emisión en paralelo del toast global (`useMarketToast`).
   - Refresco reactivo de balances (`userCredits`) y tenencias (`userHoldings`, `portfolioSummary`).
2. **Badges de Oportunidad en Catálogo P2P (`MarketP2PPage`)**:
   - Comparación del precio unitario de cada orden frente a la cotización oficial del jugador.
   - Badge visual verde esmeralda `🏷️ -X% Oferta` para precios menores al oficial (redondeo al entero más cercano).
   - Badge visual naranja/rojo `🔥 +X% Premium` para precios superiores al oficial.
   - Ocultamiento de badge cuando el precio coincide con la cotización oficial o no hay cotización disponible.

---

## 2. Technical Context

- **Frontend Stack**: React 19.3.0 + TypeScript 7.0.2, TanStack Query v5, React Hook Form con resolver Zod, SCSS Modules con tokens centralizados `@/styles/abstracts`.
- **Backend Dependencies**: Endpoints de `SPEC-026` (`POST /api/orders/sell-official` y `POST /api/listings` con soporte de `unitPrice`).
- **Almacenamiento y Estado**: TanStack Query cache (`marketQueryKeys`, `portfolioQueryKeys`), `useAuthStore` para sesión y saldo de créditos.
- **Target Platform & Viewport**: Aplicación web desktop adaptativa ($\ge 1024$ px), conforme a Constitución §Viewport.
- **Testing y Verificación**: Escenarios interactivos end-to-end en `quickstart.md`; verificación estática y compilación de frontend (`npm run build`) conforme a la Constitución y AGENTS del área.

---

## 3. Constitution Check

*GATE: Evaluación frente a los principios fundamentales de la Constitución.*

| Principio | Evaluación de Cumplimiento |
| :--- | :--- |
| **I. Simplicidad y calidad** | **PASA.** Cero dependencias externas agregadas. El slider y controles de precio se resuelven con elementos HTML5 nativos (`<input type="range">` y `<input type="number">`). Reutilización del modal y componentes existentes de `MarketSellPage`. |
| **II. Proporcionalidad del alcance** | **PASA.** Se ciñe estrictamente a las 4 User Stories del issue #95 (modal dinámico oficial/P2P, slider $\pm 20\%$, badges de catálogo y feedback accesible). Se excluyen subastas, compras oficiales o backend. |
| **III. Seguridad e integridad** | **PASA.** Toda petición utiliza cliente `http` con `authenticated: true` (Bearer JWT). Manejo de errores seguro que no filtra trazas internas ni credenciales. Prevención estricta de doble submit (`isBusy`). |
| **IV. Coherencia de contratos** | **PASA.** Contratos documentados en `contracts/sell-official-api.md` y `contracts/p2p-flexible-pricing-api.md`. Manejo exacto en centésimas en la API y división por 100 para display en créditos. Sincronización continua. |
| **V. Trazabilidad y fuente de verdad** | **PASA.** Asignación en `specs/assignments.md`. Tríada obligatoria permanente preservada (`spec.md`, `plan.md`, `tasks.md`). Pull Request #113 vinculado al issue #95. |
| **Viewport Desktop** | **PASA.** Soporte exclusivo para resolución Desktop ($\ge 1024$ px). |
| **Verificación y testing** | **PASA.** El agente no ejecuta tests de aplicación. Control permitido: `npm run build` en `frontend/`. |

---

## 4. Arquitectura de Componentes del Frontend

### 4.1. Estructura de Archivos a Crear / Modificar

```text
frontend/src/features/market/
├── types/
│   ├── models.ts                               # Actualizar SellMode, SellOfficialResult, P2POpportunity
│   └── dtos.ts                                 # OrderSellOfficialRequestDTO, OrderSellOfficialResponseDTO, unitPrice en CreateListing
├── market.service.ts                           # sellTokensOfficial(), sellTokens(playerId, quantity, unitPrice)
├── market.mapper.ts                            # Mappers para venta oficial y oportunidades P2P
├── hooks/
│   ├── mutations/
│   │   ├── useSellTokensMutation.ts            # Actualizar soporte de unitPrice opcional
│   │   └── useSellOfficialTokensMutation.ts    # Nueva mutación POST /api/orders/sell-official
│   └── pages/
│       └── useMarketSellPage.ts                # Gestión de sellMode, p2pUnitPrice, officialMutation y refresco
└── pages/
    ├── MarketSellPage/
    │   └── components/
    │       └── SellTokenModal/
    │           ├── SellTokenModal.tsx          # Orquestador del diálogo con selector de modalidad
    │           ├── SellTokenModal.module.scss  # Estilos desktop
    │           └── components/
    │               ├── SellModeSelector.tsx    # Conmutador segmentado Venta Oficial vs P2P
    │               ├── SellModalOfficialEstimation.tsx # Desglose con spread del 10%
    │               ├── P2PPriceSelector.tsx    # Slider + input de banda [-20%, +20%] con badges
    │               ├── SellModalFooter.tsx     # Botones dinámicos según sellMode
    │               └── SellModalSuccess.tsx    # Soporte para comprobante oficial y P2P
    └── MarketP2PPage/
        └── components/
            └── P2POpportunityBadge/            # Badge comparativo 🏷️ -X% Oferta / 🔥 +X% Premium
                ├── P2POpportunityBadge.tsx
                └── P2POpportunityBadge.module.scss
```

---

## 5. Flujo de Datos y Estado

```text
                    [Usuario hace clic en "Vender" en MarketSellCard]
                                          │
                                          ▼
                                  [useMarketSellPage]
                     - openSell(player)
                     - quoteQuery.data (cotización oficial de referencia)
                     - sellMode: 'OFFICIAL' (default)
                     - p2pUnitPrice: officialPrice * 0.90 (-10% default)
                                          │
                                          ▼
                                  [SellTokenModal]
                     ┌────────────────────┴────────────────────┐
                     ▼                                         ▼
           [Modo: OFFICIAL]                             [Modo: P2P]
      - SellModalOfficialEstimation                - P2PPriceSelector
        Spread: 10%                                  Banda: [-20%, +20%]
        Neto: officialPrice * 0.90                   Badge: Oferta / Premium
                     │                                         │
                     ▼                                         ▼
      [useSellOfficialTokensMutation]              [useSellTokensMutation]
        POST /api/orders/sell-official               POST /api/listings (unitPrice)
                     │                                         │
                     └────────────────────┬────────────────────┘
                                          ▼
                                    [onSuccess]
                      - queryClient.invalidateQueries(marketQueryKeys.all)
                      - queryClient.invalidateQueries(portfolioQueryKeys.all)
                      - queryClient.invalidateQueries(userCredits)
                      - notifySellSuccess() (Toast global)
                      - SellModalSuccess (Comprobante interno)
```

---

## 6. Plan de Fases de Implementación

- **Fase 1: Capa de Tipos, DTOs y Servicios**:
  - Incorporar interfaces de `SellMode`, `SellOfficialResult`, `OrderSellOfficialRequestDTO`, `OrderSellOfficialResponseDTO` y actualización de `CreateListingRequestDTO`.
  - Implementar métodos de servicio HTTP en `market.service.ts` y mappers correspondientes.
  - Implementar `useSellOfficialTokensMutation.ts` y actualizar `useSellTokensMutation.ts`.
- **Fase 2: Hook Orquestador `useMarketSellPage`**:
  - Incorporar el estado de modalidad (`sellMode`), precio unitario P2P (`p2pUnitPriceCentimes`), cálculo de variaciones y sincronización con cotizaciones vigentes.
  - Integrar la mutación de venta oficial con el refresco reactivo de queries de TanStack Query.
- **Fase 3: Componentes del Modal de Venta (`SellTokenModal`)**:
  - Construir `SellModeSelector` con pestañas accesibles para conmutar entre modalidades.
  - Construir `SellModalOfficialEstimation` para el desglose financiero del spread del 10%.
  - Construir `P2PPriceSelector` con slider interactivo, input numérico y botones de ajuste rápido.
  - Actualizar `SellModalFooter` y `SellModalSuccess` para soportar ambas modalidades.
- **Fase 4: Badges de Oportunidad en Catálogo P2P**:
  - Crear componente `P2POpportunityBadge` con lógica de comparación y redondeo al entero más cercano.
  - Integrar el badge en las tarjetas y listados del mercado secundario (`MarketP2PPage` / `CommunityMarketPanel`).
- **Fase 5: Validación y Build**:
  - Verificar compilación limpia con `npm run build` desde `frontend/`.
  - Recorrer los escenarios de validación definidos en `quickstart.md`.
