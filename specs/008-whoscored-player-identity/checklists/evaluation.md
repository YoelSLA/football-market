# Evaluación de la checklist de identidad — SPEC-008

**Fecha**: 2026-10-14. Revisión documental y marcado autorizados por el usuario.
**Resultado vigente**: 40/40 criterios cubiertos en [identity.md](identity.md).
Antecedente: primera revisión 25/40, con 15 pendientes; resueltos mediante respuestas
explícitas del usuario y documentación actualizada en esta misma fecha.
Se evalúan requisitos y diseño, no código ni ejecución. Sin operaciones en Trello.

## Evidencia de los criterios marcados

| Criterios | Evidencia y conclusión |
| --- | --- |
| CHK001, CHK005, CHK015 | [Spec](../spec.md), FR-005–007: normalización, coincidencia conjunta, resultados por número de IDs válidos y atributos excluidos. |
| CHK006, CHK008, CHK033 | Spec FR-003, FR-006–007, FR-013 y caso de transferencias: conservar contextos; sin equipo coincidente no hay asociación; el histórico permite normalización directa o alias. |
| CHK012, CHK014 | [Modelo](../data-model.md), §5: equivalencias de pares normalizados, comparación conjunta en un mismo contexto y ausencia de inferencias adicionales. |
| CHK016, CHK017, CHK018 | Spec FR-006–007, FR-012–013, US1 escenarios 3/9 y supuestos: comparación global tras excluir ligas inválidas, nunca nombre solamente; mismas reglas en ambos pases. |
| CHK020, CHK036 | Spec FR-008–009, FR-014/026; modelo §1 y §5: unicidad por proveedor, protección local y clasificación de carreras sin sobrescritura. |
| CHK021 | SC-001 define conjunto controlado, coincidencias válidas por nombre/equipo y cero asociaciones por nombre solamente. El resultado esperado se contrasta con ese conjunto, sin depender de cobertura real del proveedor. |
| CHK023 | SC-003/004, modelo §1/§2/§5: invariantes de referencia, selección de pendientes del origen y conservación de referencias; permiten comparar estado anterior/posterior y pertenencia de nuevas asociaciones. |
| CHK024 | US2 escenario 4, FR-003/013 y modelo §4: un candidato por ID, contextos conservados y descubierto contado una vez. |
| CHK025, CHK026 | FR-006/009 y modelo §5, resolución global: comparación por equipo para homónimos; varios propietarios locales del mismo candidato producen CONFLICT antes de escribir. |
| CHK028 | FR-025, US1 escenario 10 y modelo §5: snapshot persistido coherente en transacción breve REPEATABLE_READ al iniciar el pase; red tras commit. |
| CHK029 | FR-010/012, US2 escenario 1 y modelo §2: solo NO_MATCH/AMBIGUOUS sin referencia; conflictos y errores excluidos. |
| CHK031 | FR-005 y modelo §5: sin abreviaciones inferidas, coincidencia aproximada ni eliminación adicional de puntuación. Apodos, iniciales o números diferentes no se equiparan al nombre local. |
| CHK035 | Catálogo normativo: una equivalencia no listada MUST NOT inferirse ni agregarse durante la ejecución; tabla fija para todo el TP. |
| CHK038 | FR-021/024 y supuestos: consulta detallada y corrección manual explícitamente fuera de alcance; conflictos preservados, sin aprobación automática. |
| CHK039 | FR-007 y modelo §5: solo cuentan IDs que cumplen nombre y equipo simultáneamente; candidatos descartados por equipo no crean ambigüedad. |
| CHK040 | Los 15 faltantes iniciales tienen decisión humana, evidencia y resolución documental abajo; no quedan contradicciones detectadas abiertas en esta revisión. No sustituye la fase Analyze. |

## Resolución de los 15 pendientes iniciales

| Criterio | Decisión humana incorporada y evidencia vigente |
| --- | --- |
| CHK002 | FR-027, US1 escenario 11 y modelo: LOCAL_DATA_ERROR con motivo; no asociación/NO_MATCH ni histórico; continúa el resto. |
| CHK003 | FR-028 y contrato WhoScored: enteros positivos numéricos o cadenas decimales; canonicalizar ceros; fila inválida descartada/auditada sin invalidar liga. |
| CHK004 | FR-028, US1 escenario 13: teamId ausente permitido con nombre válido; teamName inválido descarta fila; equipo fuera de tabla admite igualdad normalizada. |
| CHK007 | FR-009, US1 escenario 14: máximo una referencia; varias preexistentes generan CONFLICT, conservar sin elegir/borrar, excluir matching/histórico. |
| CHK009 | FR-005: NFD y marcas Mn/Mc/Me asociadas solo a base LATIN; sin transliteración ø/ß. |
| CHK010 | FR-005: sustituir categoría Unicode Pd por espacio, incluidos guiones internos/Unicode. |
| CHK011 | FR-005: White_Space y Pd → U+0020, colapsar secuencias mixtas y quitar extremos en orden explícito. |
| CHK013 | Tabla normativa y US1 escenario 16: validar colisiones normalizadas antes de red; varios nombres externos por local permitidos sin colisión inversa. |
| CHK019 | Usuario confirmó cobertura solo de equipos persistidos de las cinco ligas, no plantillas de temporada; criterio ajustado a ese alcance y tabla validada humanamente. No se atribuye inspección de DB al agente. |
| CHK022 | SC-002: cobertura real reproducible con contadores, denominador cero no aplicable; éxito sin umbral. 83% solo antecedente contextual. |
| CHK027 | FR-029, US1 escenario 15 y modelo: variantes por contexto completo, ID único; varios locales coincidentes generan CONFLICT antes de escribir. |
| CHK030 | No aplicable a cambio en curso: usuario confirmó tabla fija para todo el TP, documentado en sección normativa. |
| CHK032 | FR-005: conservar marcas de escrituras no latinas y no transliterar. |
| CHK034 | Validación explícita por el usuario, 2026-10-14, de tabla aportada/revisada para el TP; sin revalidación por ejecución. |
| CHK037 | Referencia única «SPEC-008 — Catálogo normativo de alias de equipos» para plan/tareas/tests; tabla fija sin versionado dinámico por decisión humana. |

## Checklist general

[requirements.md](requirements.md) conserva sus 16 marcadores, revalidados tras
incorporar las decisiones. Spec tiene dos US, 29 FR (FR-001–029) y ocho SC.

Las reglas funcionales se modificaron solo para incorporar decisiones humanas
explícitas. No se ejecutaron tests/build ni nuevas operaciones de Spec Kit. La
checklist no acredita Analyze, READY, implementación ni gates humanos. Sincronización
de estas decisiones en Trello pendiente por instrucción del usuario de posponerla.
