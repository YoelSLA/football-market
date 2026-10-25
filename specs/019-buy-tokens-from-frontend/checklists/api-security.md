# API y seguridad Checklist: Comprar tokens desde el frontend

**Purpose**: Calidad y coherencia de contratos HTTP, cotización real, validación, sesión y errores.
**Created**: 2026-10-23
**Feature**: [spec.md](../spec.md) · [contrato de consumo](../contracts/buy-tokens-frontend.md)

**Review Ownership**: Checklist especializado del revisor; marcar `[x]` solo tras evaluación humana. Generación no equivale a aprobación ni a ejecución de pruebas.

## Lectura y compra

- [x] CHK001 ¿Está definido que el único precio previo a la compra proviene de `GET /api/players/{id}/quotes/current`, con `calculatedAt` del cálculo y `effectiveDate` de vigencia, sin fechas simuladas o de lectura? [Consistencia, Spec §FR-04, Contracts §Lectura]
- [x] CHK002 ¿Se diferencia `404 QUOTE_NOT_FOUND` del GET de los 404 funcionales del POST y se especifica el estado sin cotización sin inventar un precio? [Cobertura, Spec §US2/FR-06, Contracts §Lectura/§Compra]
- [x] CHK003 ¿Está especificado que el POST autenticado envía solo `{playerId, quantity}` numéricos, sin `listingId` ni usuario, y que el backend es autoridad sobre stock y precio? [Claridad, Spec §FR-02/FR-08/FR-15, Contracts §Compra]
- [x] CHK004 ¿Se separan la estimación de la cotización GET y el resultado efectivo del POST (`unitPrice`, `totalAmount`, `executedAt`) en todos los textos de usuario? [Consistencia, Spec §FR-04/FR-09, Data-model §Contratos]

## Entrada, errores y caché

- [x] CHK005 ¿Está definido cómo se interpreta el texto inicial `"1"` y el texto válido `"10"` como enteros 1–100, rechazando vacío/fracciones/no numéricos antes del DTO? [Claridad, Spec §FR-05/US2, Contracts §Compra]
- [x] CHK006 ¿Está acotada SC-03 a los diez códigos POST documentados, al fallback seguro para desconocidos/red/500 y a la distinción 401/GET 404? [Measurabilidad, Spec §SC-03/FR-10, Contracts §Cobertura]
- [x] CHK007 ¿Se definen los efectos de un 401 de solicitud autenticada sin persistir ni restaurar el modal ni revelar datos sensibles? [Seguridad, Spec §Edge Cases, Contracts §Errores]
- [x] CHK008 ¿Está delimitada la invalidación tras éxito a la quote real de market y las keys públicas summary/holdings de portfolio coordinadas por app, sin tocar la serie demo? [Consistencia, Spec §FR-11, Plan §Fronteras]

## Notes

- El frontend no ejecuta tests automatizados; los escenarios manuales están en `quickstart.md` y su evidencia queda pendiente del usuario.
