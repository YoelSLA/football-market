# Investigación de Plan — SPEC-008

**Fecha**: 2026-10-14. **Estado**: OK; Phase 0 completa tras incorporar la evidencia del contrato. El bloqueo inicial se conserva como antecedente en [plan-evidence.md](plan-evidence.md).

## 1. Cliente HTTP

- **Decisión**: utilizar RestClient con cliente JDK y configuración propia de WhoScored, sin dependencias nuevas.
- **Justificación**: `FootballDataClientConfig.java` y `TheSportsDbClientConfig.java` ya utilizan esta infraestructura, HTTPS, timeouts y redirecciones deshabilitadas. La adaptación y validación pertenecen a Integration.
- **Alternativas consideradas**: WebClient, cliente Apache y navegador automatizado; no hay necesidad acreditada para incorporarlos al alcance de identidad.

## 2. Snapshot y persistencia

- **Decisión**: captura inicial breve en valores inmutables con nombre/equipo locales; descubrimiento y matching fuera de transacción; confirmación aislada por jugador con comprobación actual de referencias.
- **Justificación**: FR-025 permite cambios posteriores de atributos, pero FR-009/014 mantienen protección y unicidad. No guardar por merge entidades detached del inicio: podría sobrescribir catálogo actualizado durante la red.
- **Alternativas consideradas**: transacción alrededor del descubrimiento, snapshot progresivo por jugador y revalidar equipo actual; contradicen FR-018 o FR-025.

`PlayerImageSynchronizationServiceImpl.java` proporciona un precedente de `TransactionTemplate` y `REQUIRES_NEW`, no del snapshot global. Referencia y resultado exitoso deben confirmarse atómicamente. Después de rollback, el error se audita en otra transacción. Constraints y locking breve protegen frente a carreras; un flag en memoria no garantiza integridad distribuida.

## 3. Unicidad y normalización

- **Decisión**: reutilizar referencias externas y agregar `WHOSCORED`. Limitar la nueva unicidad inversa a WhoScored mediante índice único parcial `(player_id)` para ese proveedor, además de `(provider, external_id)` existente; defensa de Model correspondiente sin redefinir contratos de otros proveedores.
- **Justificación**: V3 protege el ID externo pero no jugador/proveedor; `Player.addExternalReference()` permite IDs diferentes del mismo proveedor. FR-009 requiere ambos sentidos. No ampliar la regla a otros proveedores sin evaluar sus consumidores.
- **Alternativas consideradas**: validación previa sin constraint, reemplazo de asociaciones y unicidad global por jugador/proveedor; la primera falla ante concurrencia, la segunda está prohibida y la tercera amplía impacto innecesariamente.

Normalización específica de identidad WhoScored y alias exactos de spec. `TeamNameNormalizer.java` no convierte guiones en espacios, por lo que no se reutiliza como si cumpliera FR-005 ni se altera el matching de otros flujos.

## 4. Aplicación, seguridad y auditoría

- **Decisión**: Service de identificación con interfaz/implementación y agregación interna de auditoría, sin endpoints de consulta ni dependencia entre Services. Model resuelve normalización/matching; Integration valida el proveedor. No hace falta Orchestrator para este caso de uso.
- **Justificación**: matriz arquitectónica vigente. `SecurityConfig.java` protege las sincronizaciones por JWT sin rol específico; no introducir un rol nuevo. Separar errores de descubrimiento por liga de decisiones por jugador.
- **Alternativas consideradas**: lógica en Controller, Service→Service y reutilización directa de auditoría de imágenes; las dos primeras violan responsabilidades, la última no conserva snapshot/contextos requeridos y tiene una retención no especificada aquí.

Resumen reconciliado desde decisiones finales durables, no incrementos duplicados. Auditoría inmutable entre pases, con origen explícito del histórico; no exponer JSON crudo, credenciales ni URLs sensibles.

## 5. Contrato de descubrimiento resuelto

- **Decisión**: contrato [WhoScored](contracts/whoscored.md) basado en [evidencia observada](evidence/whoscored-catalog-contract.md) y cuatro JSON conservados. Recorrer `playerTableStats` con `paging` validado; finalizar tras `currentPage == totalPages`, nunca por cantidad de filas.
- **Justificación**: las muestras acreditan 472 resultados actuales, tamaño 3 y 158 páginas; la final contiene una fila y la posterior está fuera del límite. Histórico: 12562 filas y 4188 páginas. `totalResults` cuenta filas, no identidades deduplicadas. Los totales pueden variar entre observaciones, no dentro de un recorrido válido.
- **Alternativas consideradas**: fin por página corta/vacía, tolerar metadatos inconsistentes o usar nombres de campos supuestos; contradicen FR-002/015/017 o la evidencia.

WhoScored devuelve JSON con `Content-Type: text/html`. RestClient obtendrá el cuerpo acotado como bytes y el parser JSON existente lo interpretará explícitamente, sin confiar en el MIME ni cambiar los converters de los otros clientes. Los contratos externos y datos adaptados siguen separados de Model/DTO. Se ignoran métricas de rendimiento y `statColumns`.

`User-Agent` será una property no vacía del cliente dedicado, enviada mediante `defaultHeader`. Usar inicialmente el valor observado; no es una credencial ni requiere navegador, dependencia o infraestructura adicional. La evidencia no necesita cookies, Referer ni X-Requested-With. Errores 403/429 se registran sin reintento; 429 respeta Retry-After antes de otra solicitud del cliente.

## 6. Parámetros operativos

- **Decisión**: defaults de diseño: páginas de 100 filas, intervalo 2500 ms entre inicios de requests, conexión 5 s, lectura 20 s, máximo 10000 páginas por liga y 5 MiB por respuesta. Valores positivos y configurables; tamaño de página entre 1 y 100, `User-Agent` no vacío/sin CR-LF, HTTPS y redirecciones deshabilitadas.
- **Justificación**: limitar recursos y recorridos; estos valores son elecciones conservadoras locales, no límites oficiales ni un SLA del proveedor. Una liga que exceda un límite se registra como incompleta y no genera referencias.
- **Alternativas consideradas**: límites ilimitados, retries y ejecución paralela de ligas; aumentan carga o permiten recorridos sin control sin necesidad funcional.

El pacer comparte el ritmo entre requests de este cliente por instancia, fuera de transacciones; no adquiere dos veces el mismo permiso. Se recorren ligas secuencialmente. No se promete rate limiting distribuido; las constraints sí protegen referencias entre instancias concurrentes.

## 7. Ámbito local y resultados parciales

- **Decisión**: bindings explícitos entre cinco `leagueId` internos existentes y tournament IDs 2/3/4/5/22; no confundir con IDs Football-Data. Capturar todos los jugadores persistidos asociados a esas ligas, sin agregar un filtro `active`/`current` no requerido. Un histórico toma únicamente NO_MATCH/AMBIGUOUS del actual terminado y los que aún no tengan referencia, usando un snapshot nuevo al inicio del histórico.
- **Justificación**: respeta FR-004/012/025 y los IDs internos de SPEC-007. Validar bindings y existencia antes de consultar la red; IDs concretos de la DB son preparación operativa, no una decisión funcional abierta.
- **Alternativas consideradas**: inferir la liga por nombre, usar IDs de otro proveedor o resolver todo el catálogo en histórico; no respaldadas por el contrato.

Resultados locales se evalúan contra la unión de ligas completas. Para evitar falsos pendientes, si no hay coincidencia válida y la liga local capturada falló, registrar DISCOVERY_ERROR; si su liga fue completa, NO_MATCH dentro del ámbito válido declarado. Una coincidencia única o ambigüedad en ligas válidas puede registrarse aun con otras ligas fallidas. Los errores del proveedor siguen contándose por liga, no como NO_MATCH. Reglas y reconciliación en [modelo](data-model.md).

## 8. HTTP y persistencia de auditoría

- **Decisión**: ejecución manual síncrona, 200 con resumen incluso PARTIAL/FAILED cuando el resultado se registró; 404/409 para origen ausente/no elegible. Sin consultas HTTP de auditoría. Auditoría propia de run, ámbitos, candidatos deduplicados y items con snapshot y referencias a candidatos de su run.
- **Justificación**: reutiliza patrón de sincronización vigente sin introducir workers, polling obligatorio ni retención no solicitada. Campos y códigos estables definidos en [HTTP](contracts/http.md).
- **Alternativas consideradas**: async 202 sin infraestructura, un único contador mutable y mezclar errores de liga con jugadores ficticios; no satisfacen trazabilidad/reconciliación.

**Desconocidos técnicos pendientes**: ninguno para cerrar Plan. Configurar bindings reales y ejecutar validaciones después de implementación permanece a cargo de la fase correspondiente. La investigación no acredita requests nuevos al proveedor, tests ni resultados operativos.

## 9. Decisiones humanas de checklist — 2026-10-14

Reglas incorporadas en Spec FR-005/009/027–029 y tabla normativa: marcas latinas
selectivas, Pd/White_Space, alias fijos validados por el usuario y control de
colisiones antes de red. La cobertura se refiere al catálogo persistido, sin
umbral de éxito. Descartar filas de identidad inválida con auditoría propia, no
invalidar la liga; conservar validación estructural/paging sobre filas originales.
Datos locales inválidos tienen resultado LOCAL_DATA_ERROR y no son elegibles
históricos. Referencias iniciales múltiples generan CONFLICT sin reparación
automática. Variantes del mismo ID se comparan dentro de cada contexto completo.
No se agregan dependencias, consulta de resultados ni versionado dinámico.
