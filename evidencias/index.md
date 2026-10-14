# Evidencias e historial global — Football Market

> Punto de entrada del sistema de historial y evidencias (piloto).
> Documental: no altera el workflow SDD, ni estados, ni gates.

| Sección | Ruta |
| --- | --- |
| Historial global de interacciones | [`history/index.md`](history/index.md) |
| Índice de evidencias global | [`evidence/index.md`](evidence/index.md) |
| Historial de 009-player-detail | [`../specs/009-player-detail/history/index.md`](../specs/009-player-detail/history/index.md) |
| Evidencias de 009-player-detail | [`../specs/009-player-detail/evidence/index.md`](../specs/009-player-detail/evidence/index.md) |

## Alcance del piloto

- Escritura permitida: `specs/009-player-detail/**` y `evidencias/**`.
- SPEC-005: reservada para una expansión posterior (no se escribe).
- SPEC-008: excluida (no se lee ni se escribe).
- La base de datos de OpenCode solo se abre en modo lectura (`mode=ro`);
  nunca se copia completa ni se leen las tablas de cuentas y credenciales.
