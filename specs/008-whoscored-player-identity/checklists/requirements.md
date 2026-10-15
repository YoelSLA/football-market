# Checklist de calidad de especificación: Identidad externa de jugadores en WhoScored

**Propósito**: Validar completitud y calidad de requisitos de Specify antes de evaluar Clarify.
**Creado**: 2026-10-13
**Feature**: [spec.md](../spec.md)
**Evaluación**: Revisión documental del agente, 2026-10-13; una iteración, 16/16 criterios satisfechos. Revalidado tras el recorte de alcance, 2026-10-14; 16/16 criterios satisfechos. No acredita gates humanos ni READY global.

**Revalidación vigente, 2026-10-14**: 16/16 tras integrar las decisiones humanas
de identidad; especializada 40/40, evidencia en [evaluation.md](evaluation.md).

## Calidad del contenido

- [x] Sin decisiones de implementación de lenguajes, frameworks o estructura de código.
- [x] Centrado en valor para el operador y necesidades del catálogo.
- [x] Redactado para interesados de producto; hechos técnicos del proveedor separados como restricciones de entrada.
- [x] Todas las secciones obligatorias completadas.

## Completitud de requisitos

- [x] No quedan marcadores de aclaración pendientes.
- [x] Requisitos verificables y sin ambigüedades no documentadas.
- [x] Criterios de éxito medibles.
- [x] Resultados de éxito independientes de tecnologías de implementación.
- [x] Escenarios de aceptación definidos.
- [x] Casos límite identificados.
- [x] Alcance claramente delimitado.
- [x] Dependencias y supuestos identificados.

## Preparación de la especificación

- [x] Requisitos funcionales vinculables a criterios de aceptación.
- [x] Escenarios cubren los flujos principales.
- [x] Resultados esperados definidos mediante criterios de éxito verificables.
- [x] No se prescriben mecanismos de implementación en las historias ni en sus resultados.

## Notas y evidencia de revisión

- Completitud: dos US, 29 FR y ocho SC; identidad, actual, histórico y auditoría cubiertos. SC-002 define cobertura calculable sin umbral mínimo ni equiparar descubiertos con asociados.
- Cobertura US1: escenarios 1–16 y casos límite cubren descubrimiento, matching, referencias, snapshot, datos inválidos y contextos. US2: escenarios 1–6 cubren FR-012–013 y vínculo con el pase previo. FR-019–021 cubren la auditoría durable y su resumen en la respuesta de ejecución, FR-026 la reejecución segura y FR-027–029 validación local, descartes por fila y variantes de identidad; FR-022–024 son restricciones transversales.
- Recorte de alcance 2026-10-14: la revisión de resultados y evidencia mediante API se retira de esta feature; se eliminan la tercera US y el requisito de consulta, y FR-021 se reformula para que el resumen se obtenga en la respuesta de las propias operaciones de ejecución. El detalle por jugador pasa a ser auditoría interna persistida sin proyección de lectura.
- Protección de datos parciales: FR-015 «Ningún descubrimiento incompleto o inválido de una liga puede generar asociaciones» y supuesto de aislamiento por liga distinguen error del proveedor de ausencia de candidatos; la auditoría sí se conserva.
  FR-028 distingue explícitamente filas de identidad inválida: se descartan/auditan de forma individual, sin invalidar por sí solas la liga ni alterar el conteo original usado en paging.
- Paginación: la evidencia real enlazada desde la spec demuestra una última página corta y una página posterior vacía. FR-002 y FR-017 usan `currentPage == totalPages` como cierre y prohíben inferirlo por cantidad de filas; FR-015 exige estructura y metadatos coherentes sin depender exclusivamente del `Content-Type` incorrecto del proveedor.
- Matching: FR-005 define el tratamiento de guiones/espacios y excluye aproximaciones; FR-006–007 exigen coincidencia conjunta de jugador y equipo y prohíben asociar por nombre solamente. El catálogo normativo explicita las 96 equivalencias revisadas de las cinco ligas, incluidas Parma Calcio 1913, Venezia y Lecce, sin inferir alias durante una ejecución. FR-009 protege tanto identificador externo compartido como referencia local contradictoria.
- Histórico: FR-012 y US2 excluyen resueltos y conflictos y requieren pase de origen; los fallos de descubrimiento no se convierten en pendientes genuinos.
- Trazabilidad: FR-019–021 definen ámbito/unidad de contadores y decisión final única por jugador, expuestos en el resumen devuelto por la ejecución sin repetir acceso al proveedor.
- El contrato HTTP en FR-023 y los parámetros en Assumptions son restricciones oficiales/aportadas por el usuario, no elecciones de diseño. No se especifican nuevas rutas, DTO concretos, clases, librerías ni esquemas de persistencia; corresponden a Plan.
- Seguridad y exclusiones explícitas en FR-022–024. Fuentes académicas referenciadas y requisitos futuros preservados.
- Las decisiones de equipo, elegibilidad, normalización y descartes están explicitadas tras las respuestas humanas; evidencia original de Clarify se conserva como antecedente, sin atribuirle esta revisión posterior.
- Plan ya existe y queda alineado documentalmente. La siguiente operación de preparación es Tasks, por encargo separado, con sincronización Trello pendiente. Analyze sigue pendiente; READY NO. Esta revisión no autoriza IMPLEMENT ni aceptación.
