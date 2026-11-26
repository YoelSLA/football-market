# Plan de implementación: Caché de consultas frecuentes del backend

**Rama**: `feat/034-high-frequency-query-caching` | **Fecha**: 2026-11-25 | **Spec**: [spec.md](spec.md)

La rama indicada se contrastó con `git branch --show-current`; `setup-plan.sh` devolvió el
identificador sin el prefijo `feat/`. Este plan describe exclusivamente el diseño vigente; su
existencia no acredita por sí sola la preparación ni autoriza IMPLEMENT.

## Resumen

Aplicar cachés locales en memoria con Spring Cache y Caffeine en las seis consultas REST incluidas
en la SPEC-034. Las claves se basan en los criterios efectivos de cada consulta y las políticas de
expiración y capacidad son las fijadas por FR-004. La invalidación se integra con la finalización
correcta del recálculo, la sincronización del catálogo y las mutaciones existentes que afectan los
mercados. No cambian rutas, parámetros, respuestas ni autenticación; una entrada ausente se obtiene
de PostgreSQL mediante el flujo productivo actual.

## Contexto técnico

**Lenguaje y versión**: Java 21.
**Dependencias principales**: Spring Boot 4.1.1, Spring Cache (`spring-boot-starter-cache`) y
Caffeine; las dos últimas todavía no están declaradas en el build efectivo.
**Persistencia**: PostgreSQL mediante Spring Data JPA; la caché es local, volátil y no persistente.
**Verificación**: tests backend conforme a `docs/backend/testing.md`, ejecutados por el usuario;
build permitido `./gradlew build -x test` desde `backend/` únicamente si se modifica código.
**Plataforma**: servicio web backend Spring MVC.
**Tipo**: aplicación web/API REST.
**Objetivos de rendimiento**: duración máxima y capacidad por caché según FR-004; estos límites no
constituyen SLA de latencia, tasa de aciertos ni reducción de consultas.
**Restricciones**: conservar íntegramente contratos HTTP y seguridad existentes; no añadir Redis,
caché distribuida, lecturas personales en tiempo real ni endpoints transaccionales al alcance.
**Alcance**: seis consultas GET de ranking, catálogo y mercados; las mutaciones existentes solo se
tocan para comunicar invalidaciones posteriores a su éxito.

## Comprobación de Constitución

### Antes de investigar y diseñar

- **Pasa** — alcance limitado a FR/CA; se reutilizan los Controllers, Services, DTO, Model,
  repositories y operaciones existentes.
- **Pasa** — claves derivadas de entradas efectivas y usuario autenticado; no se confía en una
  identidad enviada por el cliente para separar resultados P2P.
- **Pasa** — los contratos observables y la autorización existentes se conservan; no se cachean
  errores ni respuestas de endpoints transaccionales.
- **Pasa** — sin persistencia nueva ni migraciones: Caffeine se reinicia vacía y PostgreSQL sigue
  siendo la fuente canónica.
- **Pasa** — no se agregan metas de latencia ni otras garantías cuantitativas fuera de la SPEC.

### Gates

No hay una violación constitucional identificada que requiera justificación o escalamiento. La
selección de Caffeine es la alternativa local de menor alcance acorde con FR-009; Redis queda
expresamente excluido. No hay aclaraciones funcionales pendientes en el Technical Context.

## Diseño vigente

### Arquitectura y límites

- Activar Spring Cache en configuración backend y declarar el starter de caché y Caffeine como
  dependencias efectivas. Configurar seis cachés Caffeine independientes con `expireAfterWrite` y
  `maximumSize` exactos de FR-004; usar políticas explícitas por caché, no una política global
  compartida que borre las diferencias.
- Aplicar la caché en los Services de consulta existentes, antes de su acceso a repositories. Los
  valores cacheados serán resultados de aplicación (records/Model de lectura), no DTO HTTP ni
  entidades JPA administradas; Controllers y Mappers conservan su flujo actual.
- Construir claves con valores normalizados que efectivamente gobiernan la consulta. Incluir todos
  los filtros, orden, paginación, opciones que alteran el cuerpo y valores por defecto resueltos.
  Donde la operación ya recibe un record de criterios inmutable, usar el record completo como
  identidad de la consulta, sin incorporar datos ajenos al resultado.
- Para P2P, resolver el `viewerId` desde la identidad autenticada existente y combinarlo con el
  record completo de criterios. El acceso a caché debe ocurrir después de resolver ese ID; no usar
  `viewerEmail`, cabeceras o parámetros controlados por el cliente como sustituto de la identidad.
  Usar la abstracción `org.springframework.cache.Cache`/`CacheManager` para este punto si el
  interceptor declarativo no permite formar la clave tras resolver el ID. No introducir una nueva
  dependencia Service → Service.
- Las consultas cacheables deben devolver resultados no nulos, incluidos catálogos/páginas vacíos,
  que son respuestas válidas ya representadas por las APIs actuales. Las excepciones no se
  almacenan.
- Comunicar invalidaciones con eventos internos de aplicación y un listener de infraestructura que
  limpie las cachés nombradas. Las operaciones publican el evento solo después de completar su
  trabajo exitosamente. En operaciones transaccionales de mercado, limpiar después del commit para
  no publicar una invalidación como si hubiera terminado una transacción revertida. El evento no
  modifica el resultado ni el contrato HTTP de quien invoca.
- Recálculo correcto: invalidar ranking y mercado oficial al concluir su publicación/resumen.
  Sincronización correcta del catálogo: invalidar filtros, listado y detalle después del commit
  confirmado. Compras/ventas oficiales exitosas invalidan únicamente el mercado oficial;
  publicar o cancelar una oferta P2P, y completar su compra (venta para el vendedor), invalidan
  únicamente el mercado P2P. La invalidación de mutaciones ocurre tras el commit cuando haya
  transacción. No invalidar ante una operación fallida o revertida.
- El proceso reiniciado parte de cachés vacías; una lectura fallida de carga conserva el error
  HTTP existente y no almacena un valor incompleto. No añadir fallback alternativo: el comportamiento
  ya existente consulta PostgreSQL al faltar la entrada.

### Fuentes inspeccionadas y dependencias del cambio

- Consultas existentes: `ScoreRankingQueryService`, `PlayerCatalogService`,
  `OfficialMarketCatalogService` y `P2PMarketCatalogService`, con Controllers y DTO actuales.
- Finalización del recálculo: `QuoteRecalculationServiceImpl`; sus publicaciones y cierre de
  ejecución son el punto de emisión de invalidación.
- Finalización de sincronización de catálogo: `PlayerSynchronizationOrchestrator` /
  `PlayerCatalogServiceImpl`; la foto se confirma mediante `TransactionTemplate`.
- Mutaciones del mercado: implementaciones actuales de compra, publicación, cancelación y
  recompra; cada una conserva su límite transaccional.
- La configuración efectiva del build no incluye aún Spring Cache ni Caffeine. Las versiones de
  Spring permanecen gestionadas por Spring Boot; no fijar versiones administradas manualmente.

### Estructura de fuente prevista

```text
backend/src/main/java/footballmarket/
├── common/
│   └── ... configuración de cachés y listener común de invalidación
├── features/
│   ├── ranking/application/                 # lectura cacheable del ranking
│   ├── catalog/application/                 # filtros, catálogo y detalle
│   ├── market/application/                  # mercados y publicación de invalidaciones
│   └── quote/application/                   # invalidación al finalizar recálculo
└── ...
backend/src/test/java/footballmarket/
└── features/<feature>/...                   # cobertura por responsabilidad y categoría
```

Los nombres concretos de clases y paquetes de soporte se decidirán durante IMPLEMENT al contrastar
las fuentes completas afectadas; no se crea un nuevo rol arquitectónico ni una capa de negocio
genérica.

## Fase 0 — investigación

### Decisión: Spring Cache con Caffeine local

- **Decisión**: usar la abstracción Spring Cache y Caffeine, con cachés explícitamente nombradas y
  configuradas por separado.
- **Fundamento**: responde directamente a FR-001/004/009, evita persistencia adicional y permite
  que cada resultado expire o se retire sin alterar la fuente de datos.
- **Alternativas consideradas**: Redis (distribuido y excluido por FR-009); caché propia no integrada
  (duplicaría infraestructura existente de Spring y sería de mayor alcance); caché simple sin
  proveedor local (no permite políticas diferenciadas de expiración/capacidad requeridas).
- **Evidencia**: `backend/build.gradle.kts` y `gradle/libs.versions.toml` no declaran aún las
  dependencias; la documentación oficial de Spring Boot consultada describe autodetección del
  proveedor Caffeine y configuración individual mediante `CaffeineSpec`/`Caffeine`/propiedades.
  La implementación seguirá la configuración explícita compatible con Boot 4.1.1.

### Decisión: claves compuestas y datos de consulta

- **Decisión**: usar criterios normalizados completos y, para P2P, `viewerId` autenticado en la
  clave.
- **Fundamento**: protege la correspondencia filtro/página/respuesta y separa resultados
  personalizados por usuario.
- **Alternativas consideradas**: URL cruda (puede distinguir representaciones equivalentes o dejar
  parámetros fuera); valores parciales (colisiones funcionales); identidad aportada por cliente
  (no confiable).

### Decisión: invalidación posterior a éxito

- **Decisión**: invalidación explícita de conjuntos nombrados al finalizar correctamente las
  operaciones existentes; respetar el commit cuando exista transacción.
- **Fundamento**: el borrado es acotado, hace efectivas las escrituras antes de la siguiente
  consulta y reutiliza los límites de aplicación existentes sin ampliar endpoints.
- **Alternativas consideradas**: esperar al TTL (puede servir datos antiguos tras una mutación);
  invalidar antes de ejecutar (operaciones fallidas provocan trabajo innecesario y no prueban
  consistencia); eviction indiscriminado en toda lectura o mutación (altera el propósito de caché).

## Fase 1 — diseño y contratos

### Modelo de datos

La funcionalidad no agrega entidades persistidas. `Consulta cacheable`, `Resultado de catálogo`,
`Resultado de mercado` y `Evento de actualización` son conceptos en memoria descritos en
[`data-model.md`](data-model.md); las reglas de clave, separación P2P, expiración, límites e
invalidación se detallan allí.

### Contratos

Los seis endpoints son interfaces externas existentes. El contrato de esta feature se define en
[`contracts/http.md`](contracts/http.md), conservando métodos, rutas, autenticación, parámetros,
respuestas y errores. No se documentan ni incorporan endpoints de mutación.

### Guía de validación

[`quickstart.md`](quickstart.md) describe escenarios reproducibles para caché fría/caliente,
variación de parámetros, aislamiento P2P, expiración, invalidaciones y reinicio. Sus comandos son
para ejecución humana. El agente no ejecuta tests.

## Comprobación de Constitución posterior al diseño

- **Pasa** — las seis consultas y eventos de invalidación cubren solo los requisitos y consumidores
  existentes; no se diseñan endpoints ni datos persistidos nuevos.
- **Pasa** — los contratos se mantienen sin cambios observables; filtros, orden, página,
  autenticación y errores siguen resolviéndose por el flujo actual.
- **Pasa** — el usuario P2P deriva del principal autenticado; no se confía en identificación externa.
- **Pasa** — Caffeine está confinada a memoria local, con ausencia/expiración/reinicio atendidos por
  la fuente PostgreSQL ya existente.
- **Pasa** — diseño reutiliza responsabilidades existentes; la infraestructura de invalidación es
  solo la necesaria para no acoplar Services entre áreas ni borrar antes de commit.
- **Pendiente de implementación** — concretar el punto exacto de emisión de eventos por cada mutador
  existente y confirmar los tests necesarios por responsabilidad. Esto es trabajo técnico resoluble
  y no una aclaración funcional bloqueante.

## Complejidad

No se identifica una violación de Constitución ni se requiere justificar una capa o dependencia
arquitectónica adicional. Caffeine es la implementación local exigida por el alcance.
