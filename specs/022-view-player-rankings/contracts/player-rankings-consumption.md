# Contrato de consumo: Ranking de jugadores (Frontend)

**Feature**: `022-view-player-rankings` · **Spec**: [`../spec.md`](../spec.md)

Este artefacto delimita la integración frontend; las especificaciones backend son la fuente normativa del esquema y errores completos:

- Rendimiento: [`SPEC-018 score-ranking-http.md`](../../018-consultar-rankings-de-jugadores/contracts/score-ranking-http.md), `GET /api/rankings/score`.
- Cotización: [`SPEC-016 quotes-http.md`](../../016-ejecutar-recalculo-manual-de-cotizaciones/contracts/quotes-http.md), `GET /api/rankings/quote`.

Ambos endpoints requieren JWT y aceptan los filtros/paginación descritos en sus contratos. Los parámetros efectivos se envían a la tabla sin descargar ni paginar el conjunto en el cliente. La respuesta paginada aporta identidad `player`, metadatos, opciones globales y posiciones globales; el consumidor conserva esos valores y no enriquece desde catálogo.

## Consultas

1. **Tabla**: endpoint del modo activo con `page`, `size` y filtros activos (`name`, `leagueId`, `position`).
2. **Podio**: mismo endpoint del modo activo, sin filtros, `page=0&size=3`. Se solicita separadamente y no se deriva de la página de tabla.

Los parámetros y valores por defecto siguen cada contrato backend; la presentación ofrece 10 filas por página y convierte su numeración de UI a la base cero del servicio. El cambio de modo, filtro o tamaño comienza en la página inicial.

## Interpretación en UI

- La tabla muestra únicamente `content`, sin alterar su orden ni las posiciones oficiales.
- El total de la tabla es `totalElements` filtrado; las opciones de filtro vienen de `filterOptions` y son globales.
- El encabezado de score muestra `strategy` y `quoteDate` cuando existan. Quote usa fechas y origen de cotización conforme a SPEC-016.
- `player.name` es requerido; otros atributos opcionales y datos de club/liga ausentes se presentan con fallback visual, sin inventar valores.
- Página sin filas: distinguir falta global de datos de filtro sin coincidencias usando encabezado y totales. Errores y sesión expirada siguen el manejo común HTTP/autenticación existente; no duplicar ni exponer mensajes técnicos.

No se documentan aquí esquemas duplicados ni se declara que los contratos objetivo estén ya implementados en producción. La coordinación backend/frontend y sus estados de implementación se rigen por cada SPEC.
