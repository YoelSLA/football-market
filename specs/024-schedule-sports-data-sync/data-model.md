# Modelo de datos: ejecución semanal deportiva

No se crea tabla, migración, entidad persistida ni historial de scheduler. Las entidades de SPEC-012 y SPEC-013 conservan sus invariantes, identidad de jugador, auditoría y fechas UTC propias.

## Ejecución automática conceptual

| Concepto | Valores y reglas |
| --- | --- |
| Disparo | Domingo 12:00 `America/Argentina/Buenos_Aires`, con aplicación activa. |
| Etapa | `JSON`, `SCRAPING`, `EVALUATION`; orden fijo y llamadas síncronas. |
| Resultado | Completada, parcial sin fallo global, o fallida; observable en el registro operativo, sin nuevo modelo persistido. |
| Fallo | Etapa y código/motivo seguro que comunique la operación; si no hay detalle seguro disponible, identidad segura sin inventar causa. |

Transiciones: disparo → JSON → scraping → evaluación → resultado terminal. Cualquier fallo global o interrupción va directamente a fallo terminal y omite etapas posteriores; `PARTIAL` sin fallo global sigue adelante. El resultado parcial no implica un umbral de cobertura. La persistencia de datos válidos y las transacciones son responsabilidad de las operaciones reutilizadas.

## Señal interna

La fachada de sincronización comunica por una vía interna el fallo global y su causa segura, independientemente de `status`. Esta señal no sustituye ni modifica el resumen manual y no se serializa en el contrato HTTP. El diseño detallado está en [contracts/scheduled-execution.md](contracts/scheduled-execution.md).
