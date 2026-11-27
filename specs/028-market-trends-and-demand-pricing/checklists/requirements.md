# Specification Quality Checklist: Métricas de tendencias y ajuste de oferta-demanda en cotizaciones

**Purpose**: Validar la integridad y calidad de la especificación antes de continuar a planificación  
**Created**: 2026-11-23  
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No se fijan detalles de implementación (lenguajes, frameworks o estructura interna)
- [x] Se enfoca en el valor para usuarios y necesidades de negocio
- [x] Está redactada para interesados no técnicos, excepto los contratos explícitos solicitados
- [x] Todas las secciones obligatorias están completas

## Requirement Completeness

- [x] No quedan marcadores `[NEEDS CLARIFICATION]`
- [x] Los requisitos son verificables y no ambiguos
- [x] Los criterios de éxito son medibles
- [x] Los criterios de éxito son independientes de la tecnología, salvo el endpoint explícito del alcance
- [x] Todos los escenarios de aceptación están definidos
- [x] Se identifican los casos límite pertinentes
- [x] El alcance está claramente acotado, incluido el frontend excluido
- [x] Se identifican dependencias y supuestos

## Feature Readiness

- [x] Todos los requisitos funcionales tienen criterios de aceptación inequívocos
- [x] Los escenarios principales están cubiertos por historias de usuario
- [x] Los criterios de éxito corresponden a resultados verificables
- [x] No se filtran decisiones de implementación no solicitadas

## Notes

- **Resuelto a partir del issue #96**: el endpoint manual es `POST /api/quotes/recalculate/market-demand`; la consulta reporta los cinco jugadores con mayor volumen de compra, porcentajes de transacciones de compra/venta, sesgo de mercado y volumen total en tokens y créditos.
- **Resuelto**: la ventana predeterminada es las 24 horas anteriores a la consulta, tal como confirmó el usuario; la ventana es configurable.
- **Resuelto**: el factor de mercado se calcula linealmente como `(demandaNeta / 100) × 0,50`, llevando −100..+100 tokens a −50%..+50% del precio de referencia. Sigue aplicándose el límite general de variación y el precio mínimo existentes.
- Las dependencias funcionales son las transacciones existentes (SPEC-020) y el recálculo de cotizaciones existente (SPEC-016).
- **Resuelto**: con el mismo número de transacciones de compra/venta, `marketBias` se define por conteos y el factor de precio por tokens netos; pueden diferir.
- **Resuelto**: sin score deportivo aplicable, el recálculo habitual conserva el comportamiento existente; para ajustar por mercado se usa el endpoint manual exclusivo.
- **Resuelto**: con cero transacciones en la ventana, la consulta devuelve lista vacía, volumen cero y porcentajes/sesgo nulos. El ajuste de mercado es cero en ambos recálculos; el recálculo solo de mercado no cambia precios, mientras que el habitual puede aplicar el componente deportivo si hay score.
- Los límites de factor y precio, la demanda neta cero, la elegibilidad y los fallos individuales ya tienen comportamiento especificado; no se agregan casos excepcionales ajenos al alcance.
