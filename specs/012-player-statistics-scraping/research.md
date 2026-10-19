# Research: Scraping de estadísticas de rendimiento de jugadores

## Catálogo y cobertura observada

**Decision**: Tratar como evidencia validada el catálogo cerrado de 60 métricas: 45 disponibles por scraping, 57 por JSON, 42 compartidas y tres exclusivas del scraping (`shots_on_target_total`, `second_yellow_total`, `starts_total`). Las quince combinaciones JSON recorridas forman parte del contrato externo.

**Rationale**: Las pruebas previas recorrieron el DOM y las respuestas JSON, identificaron columnas técnicas y permitieron enumerar la cobertura completa. Repetir esa investigación durante PLAN no aporta una decisión nueva y aumentaría presión sobre un endpoint no oficial.

**Alternatives considered**: Mantener solo ocho métricas JSON quedó invalidado por la evidencia; descubrir métricas durante ejecución impediría validar estructura y cobertura; configurar el catálogo en base de datos permitiría combinaciones fuera de la spec.

## Canales independientes y vigencia

**Decision**: Permitir ejecuciones independientes `SCRAPING` y `JSON`. Cada canal persiste su fila de campaña por jugador, fecha y canal, con la procedencia real en la propia fila; no existe valor vigente único entre canales. Una ausencia no sobrescribe ni degrada la fila del otro canal.

**Rationale**: Cumple la necesidad de actualizar por cualquiera de las vías, permite que un canal complete al otro y conserva auditoría sin duplicar conceptos canónicos.

**Alternatives considered**: Prioridad fija de scraping o JSON contradice la aclaración; sobrescribir filas elimina histórico; exigir ambos canales en una misma ejecución impide actualización independiente.

## Playwright y navegador

**Decision**: Incorporar `com.microsoft.playwright:playwright:1.63.0` y ejecutar Chromium con `headless=false`. La instalación del navegador es prerrequisito operativo, no acción del arranque de Spring.

**Rationale**: La verificación previa mostró 403 en cinco variantes headless y respuesta estable en headed. Playwright aporta aislamiento de contextos y acceso al DOM desde Java.

**Alternatives considered**: HTTP directo no reproduce el scraping exigido; Selenium agrega gestión de driver; ocultar automatización o falsear User-Agent no resolvió el bloqueo.

## Ciclo de vida del navegador

**Decision**: Crear Playwright y Chromium al comenzar un lote de scraping, compartir el proceso entre un máximo inicial de cuatro workers y usar un `BrowserContext` independiente para la carga inicial y otro para la sección de detalle. Cerrar todos los recursos al finalizar.

**Rationale**: Conserva el aislamiento que evitó el agotamiento de sesión y permite que la aplicación arranque sin entorno gráfico.

**Alternatives considered**: Un contexto por jugador reproduce el fallo observado; un navegador por sección agrega coste; un singleton de arranque viola FR-055.

## Cliente JSON

**Decision**: Reutilizar la configuración segura del cliente dedicado WhoScored de SPEC-008 y ampliar una Integration separada para las quince combinaciones de categoría/subcategoría recorridas y documentadas en el contrato. Validar `playerTableStats`, `statColumns`, contexto y columnas esperadas aunque el MIME sea `text/html`.

**Rationale**: El endpoint observado devuelve JSON válido con MIME HTML y requiere User-Agent de navegador. Separar el adaptador estadístico evita acoplarlo al contrato de identidad y permite validar las 57 métricas conocidas.

**Alternatives considered**: Reutilizar records de identidad mezcla contratos; aceptar campos dinámicos sin catálogo oculta cambios de estructura; requerir MIME JSON rechaza respuestas válidas observadas.

El control de ritmo, timeouts y `Retry-After` no se duplica: se extiende el pacer y la configuración segura del cliente ya existentes para el canal de identidad, con valores por canal. Crear un segundo pacer o un segundo cliente habría producido dos políticas de ritmo que pueden divergir frente al mismo proveedor.

## Paralelismo, ritmo y duración

**Decision**: Máximo inicial de cuatro workers por canal, lote de 30 minutos solo para scraping, intervalo mínimo de 1500 ms por worker y reintentos acotados solo ante transporte o timeout. JSON recorre todos los pendientes del día en una petición sin corte temporal de lote ni máximo de jugadores; mantiene timeouts de consultas y límite de retry. No reintentar 403, 404, estructura inválida ni datos incoherentes.

**Rationale**: La extrapolación histórica de 4–5 horas con tres workers no acredita el diseño final con contexto independiente por sección y pacing de 2500 ms. El objetivo acordado el 2026-10-16 es hasta 10 horas para aproximadamente 2100 jugadores por scraping, con la configuración vigente de cuatro workers y 1500 ms y proveedor disponible, incluyendo el encadenamiento de lotes; falta medición humana. JSON realiza quince consultas por jugador y reporta duración y cobertura por separado.

El pacing se aplica a inicios de navegación de secciones por worker en scraping y a inicios de consultas por worker en JSON, incluidos retries, no a cada recurso del navegador. Solo scraping acota además por deadline de lote: su media de ritmo se reconstruye de las últimas 20 confirmaciones de campaña usando duración persistida por item; sin muestras usa 60 s conservadores, no medidos. `maxPlayers` limita el total de scraping entre workers. JSON reutiliza pacing/auditoría pero no ese corte y cierra controladamente ante indisponibilidad global/interrupción, sin insistir indefinidamente.

**Alternatives considered**: Más workers elevan riesgo de bloqueo; múltiples retries contradicen FR-018. La decisión anterior de lotes acotados para ambos canales queda reemplazada por la aclaración humana: JSON recorre toda la población pendiente del día; solo scraping mantiene lotes visibles al cliente.

## Reanudación e idempotencia

**Decision**: Identificar la campaña por `extractionDate` UTC y canal. Un jugador queda completado para esa campaña cuando existe un resultado `COMPLETED`; los siguientes lotes seleccionan por ID local ascendente los pendientes o fallidos.

El usuario confirmó que JSON tampoco debe repetir confirmados del mismo día. Su POST sin entrada resuelve fecha UTC al inicio y conserva esa fecha aunque cruce medianoche. Otra llamada ese día reanuda pendientes; si ya está completo no consulta al proveedor. Una llamada otro día inicia nueva campaña; no hay parámetro de fecha histórica ni modo de forzar actualización diaria repetida. En scraping la fecha puede mantenerse explícita entre lotes, incluso después de medianoche.

La unicidad entre runs se garantiza mediante fecha/canal no nulos en el item, FK compuesta `(run_id, extraction_date, source)` al run e índice único parcial por `(extraction_date, source, player_id)` solo para `COMPLETED`. Los intentos fallidos se conservan; una colisión revierte la confirmación atómica entera. Los detalles están en `data-model.md`.

**Rationale**: Evita que completar JSON impida ejecutar scraping o viceversa, permite reanudar cada vía y mantiene cortes históricos comparables.

**Alternatives considered**: Una campaña única para ambos canales confunde coberturas; cursor del cliente puede omitir jugadores; deduplicar solo por timestamp no identifica el corte lógico.

## Persistencia y transacciones

**Decision**: Persistir inicio y cierre del run en transacciones breves; realizar llamadas externas fuera de transacción; confirmar atómicamente por jugador sus contextos, observaciones e incidencias. No mantener una tabla mutable de valores actuales: una consulta ordenada por `observedAt` y desempate estable obtiene la observación vigente.

**Rationale**: Cumple FR-032/FR-035/FR-063, conserva todo el histórico y evita inconsistencias entre una tabla actual y sus observaciones.

**Alternatives considered**: Transacción por lote retiene recursos durante minutos; actualización destructiva pierde histórico; tabla current duplicada requiere sincronización innecesaria.

## Catálogo, alias y derivaciones

**Decision**: Implementar 60 definiciones inmutables con clave, unidad, canales admitidos, columnas externas y regla de derivación. `foulsPerGame` y `foulCommitted / apps` alimentan `fouls_committed_per_match`. Los 29 acumulados se descartan después de derivar y no son métricas persistidas.

**Rationale**: Mantiene el contrato acordado, permite validar cobertura por canal y evita 29 claves adicionales y una entidad de equivalencias.

**Alternatives considered**: Persistir acumulados amplía el catálogo a 89; una `MetricEquivalenceDefinition` es innecesaria para alias que convergen en una sola clave; inferir columnas desconocidas viola el catálogo cerrado.

## Valores ausentes y decimales

**Decision**: Guardar valores en PostgreSQL `NUMERIC` nullable sin precisión ni escala fijas, con disponibilidad `PRESENT`, `ABSENT` o `NOT_DERIVABLE`; utilizar `BigDecimal` desde el token decimal original hasta la persistencia y conservar precisión observada. Solo `PRESENT` participa en la selección del valor vigente. Tolerancias y diferencias también usan `NUMERIC` sin escala fija y operaciones decimales exactas.

**Rationale**: Distingue ausencia, denominador inválido y cero; permite aplicar tolerancias sin error binario y evita que una ausencia posterior borre un valor real.

La corrección autorizada el 2026-10-16 sustituye el diseño anterior `NUMERIC(18,6)`:
la captura local `RecorridoHTML/PaginaPrincipal.txt:1219` publica el rating
`6.9033333333333333`, incompatible con seis decimales y con FR-029 si se redondea.
No se impone un límite de seis decimales al normalizador ni al parser. Solo las derivaciones
por `apps` conservan los seis decimales `HALF_UP` de FR-027; el rating directo no se deriva.
El mapping JPA debe declarar `numeric` sin precisión/escala fijas y coincidir con Flyway;
los tests previstos comprobarán ida/vuelta exacta y metadatos de precisión con PostgreSQL.

**Alternatives considered**: `NUMERIC(18,6)` redondea valores reales del proveedor;
fijar otra escala solo desplaza el límite sin responder al requisito de conservar el decimal
publicado; `double` pierde precisión; centinelas contaminan cálculos; omitir toda fila ausente
impide auditar cobertura. La precisión de comparaciones tampoco se limita artificialmente:
con 16 decimales publicados, la mitad de la unidad mínima es `0.00000000000000005`.

## Contrato HTTP

**Decision**: Dos POST independientes: `/api/players/statistics/whoscored/sync/scraping` recibe DTO con fecha/máximo opcionales; `/api/players/statistics/whoscored/sync/json` no recibe parámetros ni body y procesa todos los pendientes de su fecha UTC inicial. Se reemplazan las decisiones previas de POST único y de body común, aún no implementadas. La respuesta sigue compartida y conserva canal/fecha auditados. `GET /coverage` mantiene `source` obligatorio y fecha opcional para consultar ambos canales.

**Rationale**: Las rutas y entradas distintas expresan las operaciones requeridas por el usuario. Ambas son síncronas, con idempotencia diaria por canal y auditoría compartida; JSON no requiere conocer población, IDs ni tamaño de lote. Compartir salida e infraestructura interna no obliga a recibir el mismo request ni a limitar JSON por tiempo de scraping.

**Alternatives considered**: POST único con `source` descartado por la aclaración humana; ejecutar ambos canales juntos contradice su independencia; ejecución asíncrona contradice FR-050.

## Cliente operativo del desarrollador

**Decision**: Cliente CLI Java externo al proceso del backend y ejecutado manualmente por el desarrollador, exclusivamente para scraping. No es bean Spring ni scheduler. Reutiliza Java/RestClient y DTO de la API propia, con modos `batch` y `campaign`, fecha fija y máximo opcional por lote; no necesita opción de canal. JSON se invoca directamente sin body ni parámetros, sin encadenamiento del cliente. JWT desde entorno, sin registro ni propagación al proveedor. Componentes/opciones: `plan.md`.

**Rationale**: El usuario confirmó que el cliente es el desarrollador. Un modo campaña permite encadenar lotes síncronos sin intervención entre peticiones, sin agregar un proceso autónomo residente en el backend. Con máximo de 1, `batch` procesa como máximo un pendiente y `campaign` encadena lotes de un pendiente.

**Parada**: consultar cobertura inicial; terminar con éxito solo con cobertura completa; continuar si los pendientes disminuyen; detenerse como campaña incompleta sin progreso, ante error global/HTTP/transporte o interrupción. No reintentar automáticamente POST de resultado incierto. La posterior reanudación conserva fecha/canal y lo ya confirmado.

## Ubicación de las reglas de dominio

Normalización y derivación/alias se ubican en `models/` (`MetricValueNormalizer`, `MetricDerivationApplier`). Service obtiene datos y persiste resultados; la regla numérica no depende de Repository ni Integration. La tolerancia y clasificación de contraste se retiraron con el contraste entre canales el 2026-10-18, por lo que `MetricComparisonRule` no forma parte del modelo vigente.

## Historial de aclaraciones superadas

Esta sección conserva, sin aplicarlas, las decisiones de aclaración que quedaron
reemplazadas dentro de sus propias sesiones. Se mantienen como evidencia de proceso y
están resumidas en la tabla de decisiones vigentes de `spec.md`.

- 2026-10-15, conflicto de valor entre canales: la decisión inicial era resolver la
  precedencia por canal; quedó reemplazada por la regla temporal de FR-063 (observación
  más reciente, desempate por identificador mayor).
- 2026-10-15, persistencia de acumulados: la decisión inicial era conservar el valor
  acumulado junto a la derivada; quedó reemplazada por FR-027, que persiste solo la
  métrica por partido derivada.
- 2026-10-15, duración de la población: la extrapolación de 4 a 5 horas con tres workers
  a partir del recorrido completo de cinco secciones; quedó reemplazada como objetivo del
  diseño final por la aclaración del 2026-10-16, sin borrar la medición previa. El
  recorrido vigente es el reducido y la estimación es informativa (SC-012).
- 2026-10-15, alcance del lote: la decisión inicial era aplicar el lote acotado a ambos
  canales; quedó precisada el 2026-10-16, con lotes solo en scraping y recorrido completo
  de los pendientes del día en JSON (FR-012, FR-043).

