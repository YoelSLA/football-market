# Research: Detalle individual del jugador

**Estado**: Registro consolidado de decisiones arquitectónicas y de diseño vigentes.
**Referencias normativas**: [spec.md](spec.md), [plan.md](plan.md), [data-model.md](data-model.md) y [wireframe canónico](../../docs/wireframes/player-detail.md).

---

## 1. Decisiones de presentación y experiencia de usuario (Frontend)

| Decisión | Justificación técnica y funcional | Alternativas descartadas |
| --- | --- | --- |
| **4 KPI principales + 2 paneles temáticos específicos** | Las barras de progreso relativas carecían de una escala comparativa homogénea y el bloque de estadísticas destacadas duplicaba datos ya expuestos. 4 tarjetas KPI y 2 paneles temáticos estructuran la información con máxima claridad posicional. | Mantener barras de progreso arbitrarias; radar charts; percentiles sin población de referencia. |
| **Filas de panel acotadas (44–58 px) con espaciado armónico** | Establecer `min-height: 44px; max-height: 58px;` con `justify-content: space-evenly` garantiza que los paneles encajen de forma natural tanto en pantallas de baja altura (768 px, 800 px) como en pantallas altas sin estiramiento desmedido. | `flex: 1` sin límites verticales; alturas estáticas fijas que provocan desbordamiento. |
| **Eliminación de unidades redundantes en tarjetas KPI** | El texto de la unidad repetía innecesariamente el concepto ya indicado en el título (ej. mostrar `0,4` y debajo `regates/partido`, en vez de `0,4 regates regates/partido`). Solo el símbolo `%` se mantiene contiguo al valor. | Duplicar unidades en valor y subtítulo; abreviaturas técnicas crípticas. |
| **Jerarquía visual en KPI: iconos de 24 px y valores de 28 px bold** | Refuerza la legibilidad inmediata del dato principal en escritorio, utilizando contenedores de 48×48 px para los iconos temáticos. | Iconos pequeños de 18 px; tipografía sin peso destacado. |
| **Eliminación de `text-overflow: ellipsis` en subtítulos de KPI** | El recorte arbitrario con elipsis cortaba trazos descendentes (`g`, `p`, `y`). Ajustar `line-height: 1.35` y padding interno resuelve el clipping preservando la integridad del texto. | Cortar texto indispensable; truncamiento con puntos suspensivos. |
| **Cabecera compacta de 3 zonas con cotización oficial de 94 px** | Integra en una misma franja la identidad del jugador (avatar 90×90 px), tres métricas clave y la cotización oficial con sus badges de variación semántica (`Cotización inicial`, `Sin variación`, `▲ +X,XX %`, `▼ -X,XX %`) y diferencia absoluta en créditos. | Ocultar cotizaciones en cabecera; inventar variaciones cuando falta precio base. |
| **Paleta semántica con tokens de colores sólidos** | La interfaz utiliza fondos y bordes definidos con variables SCSS semánticas (`$surface-page-deep`, `$surface-card-dark`, `$surface-panel-to`, `$surface-accent-deep`, `$accent-sports-green`, `$border-panel`), eliminando gradientes y sombras decorativas. | Gradientes decorativos; sombras difusas; colores hardcodeados. |
| **Cero scroll vertical y horizontal en escritorio** | El Resumen deportivo encaja completamente en el viewport en resoluciones de escritorio (1920×1080, 1920×913, 1366×768, 1280×800), verificado de forma automatizada con Playwright. | Permitir scrolls internos anidados; recortar contenidos esenciales. |
| **Tres pestañas ordenadas con Cotizaciones por defecto** | Organización en `[ Cotizaciones ]` (por defecto), `[ Resumen deportivo ]` y `[ Todas las estadísticas ]`, manteniendo el foco en el valor del activo y su rendimiento deportivo. | Pestaña de resumen por defecto; pestañas flotantes; acordeones colapsables. |
| **Retención de respuesta compatible en Query Cache** | Si falla un refetch por pérdida de red, se mantiene en pantalla la última respuesta exitosa válida del mismo jugador indicando el fallo y ofreciendo reintento, evitando parpadeos o pantallas vacías. | Destruir la vista ante error transitorio; mostrar pantalla de error global. |

---

## 2. Decisiones de arquitectura y backend

| Decisión | Justificación técnica y funcional | Alternativas descartadas |
| --- | --- | --- |
| **Separación de endpoints de identidad, estadísticas y cotizaciones** | Mantener `GET /api/players/{id}`, `GET /api/players/{id}/statistics` y `GET /api/players/{id}/quotes` independientes evita sobrecargar el catálogo y permite gestionar fallos de forma aislada. | Un único endpoint masivo con payload mixto; acoplar estadísticas al catálogo. |
| **Servicio de lectura dedicado (`PlayerStatisticsService`)** | La consulta de estadísticas se resuelve internamente sin acoplarse al servicio de sincronización ni al de valuaciones, consultando datos ya persistidos en la base local. | Invocar evaluadores en tiempo real; llamadas remotas en caliente a WhoScored. |
| **Prioridad de anclaje JSON sobre SCRAPING** | Si coinciden extracciones de ambas fuentes para la misma fecha, los valores JSON actúan como ancla de contexto por su estabilidad estructurada. SCRAPING solo completa valores nulos si coinciden exactamente fecha, jugador y competición. | Fusionar registros de distintas fechas o temporadas; promediar valores entre fuentes. |
| **Distinción rigurosa entre ausencia (`null`) y cero (`0`)** | Un valor numérico `0` refleja una métrica efectivamente medida sin ocurrencias (ej. 0 goles). `null` indica que la métrica no estuvo disponible en la extracción y se representa con un guion (`-`). | Tratar nulos como ceros; ocultar métricas con valor cero. |
| **Categorización del estado del snapshot** | Estados normativos: `AVAILABLE` (60 métricas con valor), `PARTIAL` (1 a 59 métricas con valor) y `NO_DATA` (0 métricas). Permite a la interfaz reaccionar coherentemente sin ambigüedades. | Códigos de error HTTP para representar datos incompletos. |
| **Validación unificada de identificadores de ruta** | Los IDs deben ser números enteros positivos (`Long`). Formatos inválidos, cadenas no numéricas o valores menores o iguales a cero responden inmediatamente con HTTP 400. Jugadores inexistentes o inactivos responden indistintamente con HTTP 404. | Validaciones permisivas; revelar la existencia de jugadores inactivos. |
