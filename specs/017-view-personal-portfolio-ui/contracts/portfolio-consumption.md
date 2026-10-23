# Contrato de consumo: `GET /api/portfolio/me` desde el frontend

**Feature**: SPEC-017 · **Rama**: `feat/017-view-personal-portfolio-ui` · **Fecha**: 2026-10-21
**Plan**: [`../plan.md`](../plan.md) · **Spec**: [`../spec.md`](../spec.md) ·
**Modelo**: [`../data-model.md`](../data-model.md)

Este documento describe **cómo consume** el frontend la consulta del portfolio propio. **No redefine el
contrato HTTP**: el contrato autoritativo es
`specs/014-view-personal-portfolio/contracts/portfolio-api.md`, que SPEC-014 define y que el backend
implementa. Aquí se documenta el lado del consumidor: qué se pide, cómo se tipa, cómo se transforman
las respuestas y cómo se comunican los fallos a la interfaz.

---

## 1. Frontera

| Aspecto | Valor |
| --- | --- |
| Endpoint | `GET /api/portfolio/me` |
| Ubicación en el código | `frontend/src/features/portfolio/portfolio.service.ts` — único dueño del endpoint |
| Autenticación | `authenticated: true`; la identidad la resuelve el backend desde la sesión |
| Identificador de usuario | **No se envía ninguno.** No existe parámetro ni ruta que lo permita |
| Método | `GET`, sin cuerpo |
| Cancelación | La señal de aborto de TanStack Query se propaga a la solicitud |

## 2. Parámetros que envía el frontend

| Parámetro | Valores que envía | Vista |
| --- | --- | --- |
| `page` | `0` en el resumen; la página seleccionada en tenencias | Ambas |
| `size` | `1` en el resumen; `6` en tenencias | Ambas |
| `status` | Ausente, `OPEN` o `CLOSED` | Solo tenencias |

Correspondencia del filtro de la interfaz con el parámetro:

| Filtro visible | `status` enviado |
| --- | --- |
| *Todas* | ausente |
| *Abiertas* | `OPEN` |
| *Cerradas* | `CLOSED` |

El frontend solo envía valores que el contrato admite. No envía otros estados, novariantes de
mayúsculas y no valores fuera de rango: una petición inválida sería un error del cliente, no un caso de
uso.

## 3. Respuesta 200

La respuesta tiene tres objetos de nivel superior. El Service la recibe como `PortfolioResponseDTO` y la transforma
en Model dentro de su frontera; el DTO no sale de ella.

```
Backend → infrastructure/http → Service → (DTO → Mapper → Model) → Hook → Page / Component
```

La forma completa de los objetos está en [`../data-model.md`](../data-model.md) §3.

### 3.1 Frontera de transformación tipada

`playerName`, `teamName` y `position` son `string | null`, conforme al DTO y al contrato de
SPEC-014. Esa nulabilidad se conserva en Model y en la presentación: el dato ausente se comunica
sin sustituirlo por información ficticia ni omitir la posición o sus resultados.

El Service utiliza `http.get<PortfolioResponseDTO>` y entrega el DTO al Mapper, que devuelve Model.
Se preservan valores, nulos y orden; no hay type guards ni validaciones runtime ad hoc. Las garantías
de coherencia y las reglas financieras siguen perteneciendo a SPEC-014 y SPEC-015.
El genérico del cliente HTTP proporciona tipado estático, no validación de datos externos en runtime.
Por tanto no se garantiza detectar ni rechazar preventivamente una respuesta malformada. Una ruptura
de contrato se trata en su origen, sin introducir una estrategia de validación local en esta feature.

## 4. Errores

| Situación | HTTP | Qué hace el frontend |
| --- | --- | --- |
| Sesión ausente, inválida o vencida | 401 | La cadena de seguridad resuelve la sesión; la aplicación vuelve al acceso público. No se muestra información financiera |
| Parámetro fuera de rango | 400 | No se alcanza a producir: el frontend solo envía valores admitidos. Si ocurre, se muestra como error genérico |
| Inconsistencia de la persistencia | 500 | Estado de error con el mensaje del backend y acción de reintento. No se muestran datos parciales |
| Falla técnica del servicio de cotizaciones | 503 | Estado de error con el mensaje del backend y acción de reintento. **No** se muestra ningún dato financiero, ni siquiera el resultado realizado |
| Cotización ausente | 200 | **No es un error.** La posición muestra *Sin cotización* y los indicadores afectados muestran el guion |
| Red caída o error de transporte | — | Error genérico con reintento |

El mensaje presented procede de `getErrorMessage(error, fallback)` de `infrastructure/http`: devuelve el
mensaje del backend cuando el error es un `ApiError` válido y el texto de respaldo en cualquier otro
caso. La feature **no interpreta códigos de error propios**: el backend no define códigos funcionales
para esta consulta, y la distinción entre error y ausencia ya la expresa el estado HTTP.

## 5. Reglas de presentación que el contrato impone

Estas reglas no son un añadido de la interfaz: son la traducción de las garantías del contrato a texto
visible. Son las que sostienen FR-004, FR-029, FR-033 y AC05.

| Situación en el contrato | Qué muestra la interfaz |
| --- | --- |
| Indicador `null` | Guion `—`, nunca `0,00` |
| Indicador `0` informado | `0,00`, con su color neutro |
| Resultado negativo | El importe con su signo y el rol de estado negativo |
| Resultado positivo | El importe y el rol de estado positivo |
| `averageUnitPrice` nulo en una posición cerrada | Guion |
| `acquisitionCost` en una posición liquidada | `0,00`: es un cero real, no una ausencia |
| `currentUnitPrice` nulo en una posición **abierta** | *Sin cotización* |
| `currentUnitPrice` nulo en una posición **cerrada** | Guion: no es una ausencia de cotización, es que no hay posición abierta |
| `valuationComplete: false` | El resumen lo comunica; las posiciones sin cotización lo indican individualmente |

## 6. Lo que este contrato no cubre

- **El historial de transacciones**: no forma parte de la respuesta y no se amplía con paginación
  adicional. Corresponde a otro requerimiento.
- **Las cotizaciones**: la consulta expone la cotización vigente como dato, no su historial ni su
  cálculo.
- **Cualquier operación de mercado**: la consulta es de solo lectura y la interfaz no habilita compras,
  ventas ni publicaciones.
- **La imagen alternativa del jugador**: el contrato expone `imageUrl` y no `fallbackImageUrl`; la
  interfaz no puede ofrecer un retrato que el backend no informa.

## 7. Referencias

- Contrato HTTP autoritativo: `specs/014-view-personal-portfolio/contracts/portfolio-api.md`
- Modelo de datos de origen: `specs/014-view-personal-portfolio/data-model.md` §3
- Reglas contables: `specs/015-define-average-cost-and-financial-results-rules/`
- Errores de la aplicación: `docs/backend/architecture.md` §1.7
- Contrato de error del frontend: `frontend/src/infrastructure/http/`
