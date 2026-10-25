# UX y accesibilidad Checklist: Comprar tokens desde el frontend

**Purpose**: Calidad de requisitos de presentación, interacción y correspondencia con referencias 04/05.
**Created**: 2026-10-23
**Feature**: [spec.md](../spec.md) · [quickstart.md](../quickstart.md)

**Review Ownership**: Checklist especializado del revisor; marcar `[x]` solo tras evaluación humana. Las casillas no miden avance de implementación.

## Modal y referencia visual

- [x] CHK001 ¿Están especificados overlay semitransparente y mercado visible sin cambio de layout/scroll/URL ni pérdida de página, filtros existentes o pestaña al abrir/cerrar? [Cobertura, Spec §FR-03, Ref 04/05]
- [x] CHK002 ¿Está definida la cantidad inicial 1 de la ref. 05 y la estimación correspondiente si existe cotización, sin presentar el importe estimado como definitivo? [Consistencia, Spec §FR-04/US2, Ref 05]
- [x] CHK003 ¿Queda claro qué aspectos de las referencias 04/05 aplican y que saldo, órdenes, métricas, variación 24 h y filtros sin fuente no se fabrican? [Claridad, Plan §Contratos/riesgo, Quickstart §US4]
- [x] CHK004 ¿Son verificables los criterios de ausencia de reflow y responsive a 375/768/1440 px sin exigir reproducción de datos ajenos al alcance? [Measurabilidad, Spec §SC-05/FR-13, Quickstart §US4]

## Teclado y estados

- [x] CHK005 ¿Se especifican foco inicial en cantidad, ciclo Tab/Shift+Tab, `role="dialog"`, `aria-modal`, etiquetas y restauración al disparador, con excepción de desmontaje por 401? [Cobertura, Spec §FR-13/Edge Cases]
- [x] CHK006 ¿Se distinguen los cierres sin compra (Cancelar/ESC/overlay) de los controles deshabilitados durante la solicitud? [Consistencia, Spec §FR-07/FR-12, US1/US4]
- [x] CHK007 ¿Se definen el estado sin cotización, el error de cantidad inline, el aviso accesible de éxito/error y el reintento sin duplicar POST? [Cobertura, Spec §FR-05/FR-06/FR-09/FR-10/FR-12]
- [x] CHK008 ¿Está definido el cierre tras éxito a los 2 segundos o por acción humana sin perder la notificación y con foco seguro cuando se cierra? [Claridad, Spec §FR-09/FR-13, Plan §Presentación]

## Notes

- Pendiente evaluación humana de requisitos y posterior cotejo visual con las imágenes 04/05; no se ejecutaron pruebas ni se aprobaron casillas.
