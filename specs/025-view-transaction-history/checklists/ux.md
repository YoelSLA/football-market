# UX Requirements Checklist: Historial desktop

**Feature**: [spec.md](../spec.md), [plan.md](../plan.md) y [`docs/wireframes/transaction-history.md`](../../../docs/wireframes/transaction-history.md)
**Propósito**: Evaluar la calidad, consistencia y completitud de los requisitos de experiencia de usuario y diseño desktop.

---

## 1. Diseño y composición de pantalla

- [x] CHK001 El alcance se limita expresamente a resolución de escritorio.
- [x] CHK002 La composición define encabezado compacto, cuatro indicadores, barra de filtros, tabla y paginación.
- [x] CHK003 La columna de Jugador agrupa retrato actual, nombre, equipo, liga y número de transacción.
- [x] CHK004 Nombre e imagen del jugador son informativos y carecen de navegación a la ficha.
- [x] CHK005 Compra y venta se presentan de forma neutra sin interpretarse como ganancia o pérdida financiera.
- [x] CHK006 La barra de controles define búsqueda por nombre, selector de tipo, rango de fechas, orden y botón limpiar.
- [x] CHK007 El estado vacío inicial (`Ir al mercado`) y sin resultados (`Limpiar filtros`) poseen mensajes y acciones diferenciadas.
- [x] CHK008 Carga, error, vacío y sin resultados son accesibles y compactos.
- [x] CHK009 No existen contenedores de estado que fuercen una altura vacía de pantalla completa.
- [x] CHK010 La retícula base de hasta 8 px y los espaciados locales están definidos de forma armónica.
- [x] CHK011 Las columnas y su jerarquía garantizan la ausencia de scroll horizontal en 1920×913 px.
- [x] CHK012 Se reutilizan componentes compartidos (`PlayerPortrait`, `Pagination`, `Button`), iconos y tokens de estilo.
- [x] CHK013 Las imágenes nullable disponen de una cadena de fallback predecible con retrato genérico.
- [x] CHK014 La paginación informa el rango de elementos visibles y el total del conjunto filtrado.
- [x] CHK015 El criterio de usabilidad SC-006 define condiciones objetivas de reconocimiento en pantalla.
