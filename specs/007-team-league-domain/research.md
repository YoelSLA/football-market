# Investigación y decisiones vigentes — 2026-10-10

- **Esquema final desde creación:** solo DEV y datos descartables. Reorganizar V2/V3/V5 con plan de recreación explícito; mantener V1/V4 y sus responsabilidades. No repair ni ejecutar sobre historial anterior.
- **Asociaciones obligatorias:** sin migración legacy no existe motivo funcional para Player sin Team. No crear sujetos irresolubles; conservar asociación previa y registrar observaciones externas/internas según RF-015/031.
- **Casos simples:** JSONB deduplicado, sin status/historial completo. Revisión/materialización canónica resolvía una certificación de transición ahora retirada, no una necesidad US01–US05.
- **Atomicidad:** mantener snapshot previo y commit local único, rollback/replay acotado reconocido. No commits por Player ni HTTP en transacción.
- **Concurrencia:** instancia única sincronizada; DB UNIQUE/FK siguen necesarias. Sin HA/barrera distribuida. Enriquecimiento conserva bloqueo/revalidación breve por Team.
- **TheSportsDB:** conservar US04. searchteams.php no acredita completitud relevante; abstenerse con COMPLETENESS_UNPROVEN, no asumir Premium ni unicidad por una fila. Evaluación válida consume nombre, fallo técnico no; Retry-After vigente.
- **Presentación:** B-V5-04 ranking existente conservado, no relacionado con infraestructura canónica. Frontend clasifica leagueName y usa neutral en rename desconocido.
- **Cardinalidad/opcionales:** B-V5-01–05 conservados; ninguna simplificación selecciona/elimina referencias o escoge opcionales por orden.

Alternativa rechazada: mantener R1/R2 y V5.1 por compatibilidad con una BD DEV que el usuario acepta recrear. Sustituida por esta decisión; no conserva autoridad operativa.
