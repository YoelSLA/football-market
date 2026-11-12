# Integration Requirements Checklist: Historial paginado

**Feature**: [spec.md](../spec.md), [plan.md](../plan.md) y [contracts/transaction-history.md](../contracts/transaction-history.md)
**Propósito**: Evaluar la coherencia de integración frontend-backend, consistencia de contratos y consultas.

---

## 1. Criterios de integración y contratos

- [x] CHK001 El contrato define parámetros de consulta, defaults, límites, respuesta wrapper y códigos de error.
- [x] CHK002 La identidad procede exclusivamente del token JWT de la sesión y no existe selector de usuario.
- [x] CHK003 Los campos financieros históricos se distinguen con precisión del perfil actual del jugador.
- [x] CHK004 La nulabilidad de imágenes y la obligatoriedad de jugador, equipo y liga están especificadas.
- [x] CHK005 La consulta de contenido, conteo y agregados comparten exactamente los mismos predicados SQL.
- [x] CHK006 Los cuatro indicadores se calculan sobre el conjunto filtrado y no se interpretan como rentabilidad.
- [x] CHK007 La paginación se realiza en base de datos y define comportamiento ante páginas fuera de rango.
- [x] CHK008 El orden es cerrado (`NEWEST`, `OLDEST`, `AMOUNT_DESC`, `AMOUNT_ASC`) con desempates deterministas.
- [x] CHK009 La búsqueda se ejecuta sobre el nombre actual normalizado del jugador.
- [x] CHK010 El rango temporal valida que el instante inicial sea estrictamente anterior al final.
- [x] CHK011 La estrategia de repositorio evita consultas N+1 y llamadas frontend adicionales.
- [x] CHK012 La firma del endpoint y su DTO wrapper están alineados entre frontend y backend.
- [x] CHK013 Las dependencias y responsabilidades de SPEC-020 están claramente delimitadas.
- [x] CHK014 Flujos contables y operaciones de mercado (`SPEC-010/011`, `SPEC-014/015`) no sufren alteraciones.
- [x] CHK015 No se introducen migraciones de base de datos, snapshots históricos redundantes ni backfills.
- [x] CHK016 Tipos DTO, modelos de dominio, mapper, servicio, repositorio y OpenAPI están integrados.
- [x] CHK017 Los search params de la URL y la query key definen la fuente de verdad del estado frontend.
- [x] CHK018 La navegación y el manejo global de errores de autenticación permanecen consistentes.
- [x] CHK019 Los cuatro estados de visualización (carga, error, vacío, sin resultados) y sus acciones están diferenciados.
- [x] CHK020 La verificación visual y de integración está prevista de acuerdo a las directrices de testing.
