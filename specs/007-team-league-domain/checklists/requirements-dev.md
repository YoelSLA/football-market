# Checklist humano — alcance DEV simplificado

Sin marcar por el agente; no acredita implementación ni ejecución.

- [ ] US01–US05 conservan criterios funcionales; US06 y RF-017/034/036, SC-009/010 retirados con antecedente.
- [ ] RF-004 obligatorio en Model/JPA/SQL; no altas irresolubles ni Players sin Team.
- [ ] Identidad, cardinalidad, protección, opcionales, presentación y continuidad B-V5-01–05 conservadas.
- [ ] Atomicidad/foto completa/replay reconocido y TheSportsDB independiente preservados.
- [ ] Solo casos deduplicados necesarios; ninguna dependencia canónica o de transición vigente.
- [ ] Historial Flyway limpio y recreación DEV explícita; no repair ni ejecución sobre BD anterior.
- [ ] Tests observables suficientes en Testcontainers; no tests sobre BD DEV ni reflexión de privados.
- [ ] Recreación, tests, review y cierre siguen requiriendo autorización/validación humanas correspondientes.
