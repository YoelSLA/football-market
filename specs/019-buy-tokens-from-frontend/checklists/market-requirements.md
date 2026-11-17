# Calidad de requisitos de ambos mercados Checklist: SPEC-019

**Purpose**: Evaluar completitud, claridad, consistencia, verificabilidad y cobertura de los requisitos vigentes del Mercado Oficial y Mercado P2P.
**Created**: 2026-11-13
**Feature**: [spec.md](../spec.md) · [plan.md](../plan.md) · [research.md](../research.md) · [data-model.md](../data-model.md) · [quickstart.md](../quickstart.md)

**Note**: Este checklist personalizado fue generado por `/speckit.checklist` a partir de la documentación vigente. Evalúa requisitos escritos, no implementación.
**Review Ownership**: El checklist pertenece al revisor. Marcar `[x]` solo cuando el revisor determine que el criterio de calidad de requisitos está satisfecho.
**Marker Semantics**: `[x]` acredita revisión satisfactoria del requisito; no acredita implementación, pruebas ni aceptación funcional.

## Completitud y trazabilidad

- [x] CHK001 ¿Están definidas como superficies independientes las rutas protegidas `/market/buy` y `/market/p2p`, sin selector interno ni pérdida de las capacidades vigentes de compra? [Completitud, Spec §FR-001]
- [x] CHK002 ¿Cada historia principal tiene escenarios independientes y criterios trazables para catálogo, selección, confirmación, resultado y recuperación de errores? [Cobertura, Spec §US1–US4]
- [x] CHK003 ¿Los requisitos distinguen explícitamente lectura remota, transformación local, presentación y autoridad transaccional del backend para ambos mercados? [Claridad, Spec §FR-002/FR-023–FR-025, Plan §Diseño técnico]
- [x] CHK004 ¿Los criterios de éxito cubren reconstrucción URL, ausencia de datos simulados, prevención de doble envío, lote completo, errores seguros y accesibilidad sin depender de detalles de implementación? [Completitud, Spec §SC-001–SC-011]
- [x] CHK005 ¿Las dependencias sobre SPEC-010, SPEC-011, catálogo deportivo, sesión, saldo y portfolio están declaradas con su autoridad y límites? [Dependencias, Spec §Assumptions, Research §Fuentes]

## Contratos HTTP y modelos

- [x] CHK006 ¿Los requisitos del catálogo oficial corresponden exactamente a `GET /api/market/official`, su paginación base cero, filtros admitidos, criterio único de orden y cotización nullable? [Consistencia, Spec §FR-002–FR-009, Contracts §Mercado Oficial]
- [x] CHK007 ¿La documentación diferencia todos los campos recibidos de cotización de los únicos campos autorizados para presentación, excluyendo score, multiplicador, origen y `calculatedAt`? [Claridad, Spec §FR-011–FR-013, Data Model §OfficialMarketQuote]
- [x] CHK008 ¿`quote: null`, `previousPrice: null`, `variationPercentage: null` y variación cero tienen semánticas distintas, completas y consistentes en spec, contrato, modelo y estados? [Consistencia, Spec §FR-011/FR-013, Contracts §Respuesta 200]
- [x] CHK009 ¿Los requisitos P2P reflejan que `GET /api/listings` no recibe filtros ni paginación y devuelve la colección completa comprable de la sesión? [Consistencia, Spec §FR-023, Contracts §Lectura P2P]
- [x] CHK010 ¿Están definidos los criterios de validez de publicaciones y la exclusión de vendedor, publicaciones inválidas y jugadores sin metadatos resolubles sin inventar datos? [Cobertura, Spec §FR-023–FR-025/FR-030, Data Model §Mercado P2P]
- [x] CHK011 ¿Los dos modos de `POST /api/orders/buy` están separados inequívocamente como `{ playerId, quantity }` y `{ listingId }`, sin precio, usuario ni cantidad P2P? [Claridad, Spec §FR-018/FR-031, Contracts §Compra]
- [x] CHK012 ¿La respuesta común de compra y sus validaciones de identificadores, importes, total, instante y `listingId` están descritas de forma coherente para ambos flujos? [Consistencia, Spec §FR-020/FR-033, Data Model §BuyResult]

## Filtros, orden y URL

- [x] CHK013 ¿La lista oficial de parámetros URL visibles coincide en spec y plan, excluye `hasQuote` y `size`, y define su limpieza con `replace` sin alterar el contrato backend? [Consistencia, Spec §FR-008, Contracts §Parámetros]
- [x] CHK014 ¿Los requisitos oficiales definen con claridad qué controles son inmediatos, cuál usa debounce, qué cambios restablecen `page=0` y cuándo una query debe quedar pausada por validación? [Claridad, Spec §FR-004/FR-006/FR-008, Plan §URL]
- [x] CHK015 ¿La dependencia liga-equipo y la limpieza del equipo incompatible están especificadas sin derivar opciones desde una página parcial? [Completitud, Spec §FR-005, Contracts §Opciones deportivas]
- [x] CHK016 ¿La semántica de importes del rango oficial diferencia créditos editables de centésimas contractuales e incluye negativos, rango invertido y límites inclusivos? [Cobertura, Spec §FR-006, Contracts §Parámetros]
- [x] CHK017 ¿Los nombres exactos `search`, `league`, `position`, `sort`, `page` y `player` del estado P2P están definidos y son consistentes entre spec, plan, contrato y wireframe? [Consistencia, Spec §FR-027–FR-028, Plan §Flujo P2P]
- [x] CHK018 ¿La búsqueda P2P está especificada con 300 ms de debounce y los filtros de liga, posición y orden con actualización inmediata en todas las fuentes vigentes? [Consistencia, Spec §FR-026, Plan §Flujo P2P]
- [x] CHK019 ¿Los requisitos P2P establecen inequívocamente el orden agrupar → filtrar → ordenar → paginar y los desempates deterministas de catálogo y publicaciones? [Claridad, Spec §FR-024/FR-026/FR-029, Data Model §Derivación]
- [x] CHK020 ¿La reconstrucción mediante compartir, recargar y Back/Forward cubre catálogo, página y detalle en ambos mercados, incluidos parámetros inválidos o fuera de rango? [Cobertura, Spec §FR-008/FR-027–FR-028, Edge Cases]

## Catálogos, detalle y paginación

- [x] CHK021 ¿Los requisitos oficiales diferencian mercado vacío, filtros sin resultados, error, actualización con datos previos y jugador sin cotización sin presentar estados parciales como completos? [Cobertura, Spec §FR-013–FR-014, Quickstart §Mercado Oficial]
- [x] CHK022 ¿La ubicación única del total, el tamaño oficial fijo de 8 y la paginación fuera de la grilla están definidos sin controles duplicados? [Consistencia, Spec §FR-009–FR-010, Wireframe Oficial §Distribución]
- [x] CHK023 ¿El catálogo P2P define una ficha única por jugador con conteo, suma segura y precio mínimo calculados sobre todas las publicaciones válidas antes de paginar de a 12? [Completitud, Spec §FR-024/FR-026, Data Model §Mercado P2P]
- [x] CHK024 ¿El detalle P2P en `/market/p2p?player=<id>` está especificado como derivación de la colección ya leída, conserva los demás parámetros y no exige `GET /api/listings/{id}`? [Claridad, Spec §FR-028–FR-029, Plan §Flujo P2P]
- [x] CHK025 ¿Los requisitos cubren jugador inválido, jugador sin ofertas después de una actualización y publicaciones que vencen mientras el detalle permanece abierto? [Edge Case, Spec §Edge Cases/FR-034]
- [x] CHK026 ¿La distribución aprobada define cuatro cards P2P por fila en escritorio y los breakpoints adaptables sin contradecir los requisitos generales de ausencia de overflow? [Consistencia, Wireframe P2P §Grilla, Spec §FR-036]

## Compra y estados transaccionales

- [x] CHK027 ¿La compra oficial define cantidad inicial, edición 1–100, controles unitarios, accesos rápidos absolutos, estimación y todas las condiciones que bloquean confirmación? [Completitud, Spec §FR-016–FR-019]
- [x] CHK028 ¿La documentación distingue la estimación oficial de los importes ejecutados y deja al servidor como autoridad de precio, stock y disponibilidad? [Consistencia, Spec §FR-020, Contracts §Compra oficial]
- [x] CHK029 ¿La compra P2P especifica lote completo de solo lectura, ausencia de cantidad editable, payload exclusivo y guarda duplicada en botón y manejador? [Claridad, Spec §FR-030–FR-031]
- [x] CHK030 ¿Los errores oficiales y P2P están cubiertos por mensajes seguros, acciones de actualización y tratamiento diferenciado de sesión expirada, conflicto y red incierta? [Cobertura, Spec §FR-022/FR-032–FR-034, Contracts §Errores]
- [x] CHK031 ¿La política de retry distingue lecturas recuperables de un POST transaccional incierto que no debe repetirse automáticamente? [Seguridad, Spec §US4/FR-034, Contracts §Compra P2P]
- [x] CHK032 ¿Las invalidaciones posteriores al éxito están completas y separadas por modalidad, incluyendo catálogo correspondiente, usuario, resumen y todas las tenencias? [Consistencia, Spec §FR-021/FR-033, Plan §Compra]
- [x] CHK033 ¿Los ciclos de éxito están diferenciados como autocierre oficial opcional después de dos segundos y éxito P2P persistente hasta cierre explícito? [Claridad, Spec §FR-021/FR-033]

## Interfaz, accesibilidad y diseño

- [x] CHK034 ¿Los wireframes aprobados distinguen medidas reales, calculadas y propuestas, y los requisitos remiten a ellos sin convertir medidas inexistentes en comportamiento ya implementado? [Consistencia, Spec §Assumptions, Wireframes §Especificaciones dimensionales]
- [x] CHK035 ¿La jerarquía de header, filtros horizontales, barra de resultados, grilla, detalle, paginación y modal está definida para ambos mercados sin duplicar información? [Completitud, Spec §FR-010/FR-036, Wireframes §Distribución]
- [x] CHK036 ¿Los requisitos de foco contenido, foco inicial, restitución, Escape, overlay, bloqueo durante submit, anuncios y scroll interno cubren ambos diálogos y sus estados? [Cobertura, Spec §FR-035/US5]
- [x] CHK037 ¿Las exigencias responsive y de ausencia de overflow son objetivamente verificables en 375, 768, 1440 y 1920 × 913 px según la superficie aplicable? [Measurabilidad, Spec §FR-036/SC-011]
- [x] CHK038 ¿Los requisitos incluyen carga, error, vacío, sin resultados, dato ausente, procesamiento, éxito y error transaccional con semántica accesible para cada superficie aplicable? [Cobertura, Spec §FR-014/FR-034–FR-035]

## Preservación y regresiones

- [x] CHK039 ¿El diseño conserva rutas, compra directa, compra por publicación, navegación, notificaciones e invalidaciones existentes sin fusionar contratos o Page Hooks incompatibles? [Consistencia, Plan §Structure Decision/Preservación de flujos]
- [x] CHK040 ¿Las responsabilidades de Page, Components, Page Hooks, Query Hooks, Service y Mapper están delimitadas sin acceso de Components a estado remoto ni DTOs fuera de Service/Mapper? [Arquitectura, Plan §Responsabilidades]
- [x] CHK041 ¿La reutilización de Modal, Pagination, controles y recursos compartidos está justificada por uso real y no mueve conceptos específicos de mercado a `shared`? [Dependencia, Plan §Responsabilidades UI]
- [x] CHK042 ¿El quickstart cubre regresión cruzada de ambos mercados sin presentar build, inspección visual o escenarios manuales como aceptación automática? [Cobertura, Quickstart §Gate/Registro de evidencia]

## Notes

- Los checklists anteriores se conservan como evidencia histórica y no se reinterpretan como evaluación vigente.
- CHK017 y CHK018 quedaron cerrados con los nombres exactos de parámetros, debounce P2P de 300 ms y actualización inmediata de los demás controles.
- Los marcadores acreditan únicamente calidad documental revisada; no sustituyen implementación, pruebas ni aceptación funcional.
- `/speckit.implement` lee los marcadores como gate y no debe modificarlos.
