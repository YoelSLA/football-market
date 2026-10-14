# Fuente de datos de demostración: estadísticas y cotizaciones

Define la fuente centralizada de datos simulados del detalle del jugador y cómo se sustituirá
por información real. **No constituye un contrato HTTP**: estos datos no se transportan por la
API y no provienen del backend.

## Alcance

Cubre los modelos de demostración descritos en [../data-model.md](../data-model.md):

- `PlayerSampleSeason`: partidos, goles, asistencias y minutos por jugador en la temporada fija
  2025/2026.
- `TokenQuoteSeries`: treinta cotizaciones diarias en créditos dentro de un período fijo.
- `TokenQuoteSnapshot`: precio actual, fecha y variación derivados de la serie.

## Reglas transversales

1. Determinismo: todo valor se deriva de una función hash estable del `playerId` (y de la
   temporada o el período, que son constantes), sin generators aleatorios.
2. Independencia del reloj: ni la temporada ni las fechas dependen de `Date.now()` ni de la
   configuración regional del navegador.
3. Aislamiento: las estadísticas solo se generan para una identidad real resuelta; nunca se
   generan para un id inválido, inexistente, inactivo o no visible.
4. Procedencia: cada bloque que use estos datos muestra la etiqueta «Datos de demostración».
5. Consistencia: catálogo y detalle consumen la misma función, de modo que los valores comunes
   coinciden por jugador.
6. Sustituibilidad: ninguna Presentation depende de la forma de derivación; reemplazar la fuente
   no requiere cambios en componentes ni en el flujo de estados.

## Dependencia del detalle con la identidad real

El detalle sigue esta secuencia y no muestra datos de demostración antes de tiempo:

```text
identidad real resuelta
  └─ playerId
       ├─ estadísticas de 2025/2026
       └─ serie de treinta cotizaciones (una por día) → cotización actual y variación
```

Ante carga, error, `400`, `401` o `404`, ninguna de las dos fuentes se invoca.

## Sustitución futura

| Fuente de demostración | Fuente real prevista | Impacto en la presentación |
| --- | --- | --- |
| `PlayerSampleSeason` | Estadísticas reales (contrato a acordar) | Ninguno; solo cambia el Service y el Query Hook. |
| `TokenQuoteSeries` / `TokenQuoteSnapshot` | Historial real de cotizaciones (p. ej. `GET /api/players/{id}/quotes`) | Ninguno; se conservan estados de vacío, punto único y variación no disponible. |

El anexo de [player-detail.md](player-detail.md) describe conceptualmente la integración futura de
cotizaciones y advierte que no es una API existente. Al acuerdoarse el contrato real, ambos
documentos se actualizan y la fuente de demostración deja de usarse para el recurso sustituido.

## Restricciones

- No introducir endpoints estadísticos ni financieros en esta feature.
- No presentar estos datos como rendimiento, cotización operativa o valor de transferencia real.
- No persistir datos de demostración ni asociarlos a entidades persistidas.
- No reutilizar estos modelos como DTO de una API futura.

## Uso compartido con el catálogo (2026-10-13)

La fuente de estadísticas de demostración es **compartida** por el detalle y por la tarjeta del
catálogo: ambos consumen el mismo generador determinista a partir del identificador del jugador, de
modo que los partidos, goles y asistencias coinciden exactamente para el mismo jugador. El catálogo
presenta esos mismos valores etiquetados como demostración, conforme a SPEC-005 FR-005.

La serie de cotizaciones es exclusiva del detalle: el catálogo no muestra información financiera.
El contrato del catálogo no se modifica por este compartimiento; solo su valor de presentación.