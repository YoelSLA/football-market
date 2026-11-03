# Trabajo pendiente y futuro — SPEC-019

**Registro:** 2026-10-25. Fuente de tareas y marcadores: [tasks.md](tasks.md). Evidencia de entrega: [implementation-report.md](implementation-report.md). No crea gates, nuevas Issues ni requisitos fuera de la spec.

## Trabajo necesario antes de cerrar el alcance vigente

| Pendiente | Clasificación / trazabilidad | Qué se necesita para resolverlo |
| --- | --- | --- |
| Integración autenticada de cotizaciones reales | Validación de integración requerida; T-17/Q-04, FR-04/FR-06 | Respuesta real GET current del jugador e inspección del estado de query. 200 válido debe permitir confirmar con cantidad válida sin ejecutar compra durante esta inspección; 404/401/403/red deben distinguirse. Sin quote válida, compra bloqueada. No se presume falta de datos backend |
| Compra y resultado efectivo | Flujo funcional no verificado E2E; T-17, FR-08–FR-12 | Evidencia humana autorizada del contrato POST, resultado ejecutado, errores, prevención de doble envío y actualización de información. No se simula ni ejecuta una compra en esta preparación |
| Modal y accesibilidad | Validación requerida; T-18/V-04 | Comparación 04/05, teclado, ARIA, lector, foco, cierre y conservación de estado con evidencia |
| Scroll/responsive y detalles visuales | Validación requerida; T-18/S-03/Q-04 | Inspección autenticada en 375/768/1440px registrando altura/zoom, panel móvil expandido, paginación visible, iconos y scrollbar, además de rutas ajenas sin regresión |
| Revisión y cierre | Gates humanos pendientes, no tareas nuevas | Revisar reportes/diff, resolver hallazgos autorizados, verificar aplicabilidad de testing y evaluar CONVERGE cuando corresponda; autorización explícita final. Ningún build sustituye estos pasos |

Estos pendientes **no son opcionales ni quedan dispensados por registrarse aquí**. Su estado se mantiene únicamente en tasks.md y las autorizaciones del proyecto. No se registra una causa backend como deuda técnica sin evidencia verificable, conforme a docs/TECHNICAL_DEBT.md §2.

## Capacidades fuera del alcance funcional vigente del rediseño

| Capacidad | Situación actual | Condición para trabajo futuro |
| --- | --- | --- |
| Filtros combinables sobre catálogo completo y orden por precio | Panel únicamente visual/deshabilitado. Propuesta funcional anterior sustituida por decisión humana; no hay carga global ni endpoints nuevos | Nuevo alcance autorizado, estrategia de lectura aprobada, artefactos y ANALYZE vigentes; no reinterpretar controles visuales como funcionales |
| −/+ y cantidades rápidas | Deshabilitados y pendientes; el campo textual editable 1–100 sí está implementado | Autorizar comportamiento y actualizar las tareas aplicables; no activar en esta preparación |
| Selector de tamaño de página | Visual/deshabilitado, catálogo conserva 12 por página y paginación actual | Autorizar modificación del contrato consumidor/composición sin alterar players incidentalmente |
| Favoritos, venta, Mis órdenes e historial | Controles futuros o pestañas sin ruta, señaladas como pendientes | Funcionalidad especificada y autorizada aparte; añadir iconos no la implementa |
| Saldo, órdenes pendientes, estadísticas y variación 24h | Ausencia explícita de datos en la UI, sin cifras o gráficos simulados | Conectar fuentes reales y alcance aprobado; no reutilizar demos como datos financieros |

No se convierten estas capacidades en deuda técnica de implementación incumplida: fueron excluidas o limitadas expresamente por el alcance visual vigente. La revisión futura debe conservar esa distinción y no usar este registro para afirmar completitud de la compra.
