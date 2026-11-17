# Checklist de requisitos — mercado P2P frontend

**Spec:** SPEC-019 · **Fecha:** 2026-11-04 · **Estado:** revisión documental

## Alcance y contrato

- [x] Se conserva la compra oficial y se sustituye explícitamente la exclusión histórica de `listingId`.
- [x] La estrategia aprobada usa `GET /api/listings` y `POST /api/orders/buy` sin cambios backend.
- [x] «Todas» se delimita a publicaciones comprables expuestas al usuario; las propias quedan excluidas.
- [x] DTO, unidades monetarias, estados, fechas y payload P2P están definidos sin coerciones.
- [x] Nombre público del vendedor, compra parcial y paginación backend quedan fuera de alcance.

## UX y navegación

- [x] Modalidades, catálogo agrupado, detalle y confirmación tienen escenarios independientes.
- [x] La selección es reconstruible con `mode` y `player` en la URL.
- [x] Se definen carga, error, vacío, filtro vacío, expiración, concurrencia, éxito e incertidumbre de red.
- [x] Se distingue el orden de publicaciones del orden/paginación de jugadores.
- [x] La confirmación P2P no reutiliza accidentalmente el formulario de cantidad de compra oficial.

## Arquitectura y seguridad

- [x] Se conserva `Page → Hook → Service` y DTO restringido a Service/Mapper.
- [x] Los metadatos se consumen mediante API pública de players, sin imports privados ni HTTP duplicado.
- [x] El vendedor no se identifica ni se filtran detalles sensibles en errores.
- [x] Backend conserva autoridad sobre visibilidad, vigencia, saldo, stock y liquidación.
- [x] El POST incierto no se reintenta automáticamente.

## Gates

- [x] Las tareas separan lógica/estructura y estética.
- [x] Existe pausa humana obligatoria P-09 antes de SCSS fino.
- [x] Implementación autorizada sobre el artefacto consolidado: «Autorizo implementar SPEC-019 P2P,
  Fase P1, tareas P-01 a P-08.» (usuario, 2026-11-04).
- [x] Revisión humana de Fase P1 y autorización P-10/P-11 recibidas el 2026-11-04.
- [ ] Confirmación visual y funcional humana.

## Observaciones no bloqueantes

- La Alternativa A puede requerir varias lecturas de jugadores y descarga el mercado completo.
- Si el volumen crece, el endpoint agregado/paginado requiere otro alcance y contrato.
- El saldo posterior exacto depende de que el contrato de usuario preserve precisión suficiente.
