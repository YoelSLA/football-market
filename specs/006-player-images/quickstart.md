# Guía de validación prevista: imágenes de jugadores

Esta guía describe escenarios **para ejecutar el usuario después de la implementación**. El agente no ejecutó tests, builds, proveedores, migraciones ni controles automáticos. Ver [contrato](contracts/api.md) y [modelo](data-model.md) para campos y estados.

## Prerrequisitos

- Java 21, PostgreSQL y Node.js 24; configuración vigente del catálogo y credenciales de usuario autenticado (JWT). Un entorno de desarrollo aislado con datos ficticios y stub de TheSportsDB; no consumir la cuota real para comprobar escenarios masivos.
- Configurar la clave de TheSportsDB localmente sin subirla al repositorio y aplicar las migraciones Flyway sobre una base de prueba; `the-sports-db.audit-retention-days` es opcional. Nunca simular fallos destructivos en datos reales.
- Backend arrancado y frontend local; navegador con DevTools para observar URL y fallback. Para integración con PostgreSQL aislado, Docker/Testcontainers según el proyecto. Las pruebas frontend no se crean por las reglas vigentes.

## Comandos previstos para el usuario (no ejecutados aquí)

Desde `backend/`, una vez implementada la feature y disponibles sus tests:

```bash
./gradlew test
./gradlew bootRun
```

Desde `frontend/`, una vez implementada la UI:

```bash
npm run dev
```

El usuario puede ejecutar los tests backend pertinentes para matching, integración simulada, migración, repositorios, estado/auditoría y contratos HTTP; no inferir éxito de builds sin tests. La ejecución de Spotless/CI/SonarQube, si se requiere, queda también a cargo del usuario.

## Escenarios de aceptación

1. Preparar activos `PENDING` con y sin referencia externa, e inactivos. Autenticar y consultar `GET /api/players`: ambas URLs nullable; datos locales, sin referencias ni auditoría. Confirmar que visita, arranque y `POST /api/players/sync` no inician solicitudes TheSportsDB.
2. Invocar `POST /api/players/images/sync` sin force con proveedor simulado: búsqueda de candidatos sin referencia; consulta directa por externalId para referencia existente. Buscar respuesta sin resultados (incluidas raíces nulas), candidato único válido sin imagen y múltiples candidatos (0, 1 o >1 válidos). Confirmar `NOT_FOUND` sin retry técnico y referencia persistida cuando se resolvió identidad, aunque falte imagen. Con referencia e imágenes previas, simular lookup válido sin resultados: `NOT_FOUND`, conserva referencia y URLs sin rematching ni retry técnico. Separadamente, simular lookup que sí devuelve identidad esperada pero sin nuevas imágenes: `FOUND` con imágenes previas válidas, `NOT_FOUND` sin ellas. Repetir con `force=true` y confirmar que nunca rematchea identidades resueltas.
3. Ensayar nombre principal y alternativo equivalentes, normalización de nombres y equipos, sufijos conocidos, alias controlados de nacionalidad, fecha exacta y Soccer obligatorio. Equipo coincidente admite ausencia de fecha/nacionalidad, rechaza contradicción presente; equipo distinto requiere ambas presentes e iguales. Al consultar una referencia existente, validar solo la identidad devuelta por su ID: incompatibilidad inequívoca de datos presentes ⇒ `FAILED` con referencia e imágenes conservadas; ausencia de datos opcionales sola no implica contradicción. Nunca buscar otro candidato, usar relevancia ni fuzzy matching.
4. Probar imágenes prioritaria/secundaria distintas, iguales, solo secundaria y ninguna; rechazar HTTP, hosts maliciosos por substring y URLs de otros dominios; no hacer HEAD/GET de imagen desde backend. Forzar refresh sin imagen y verificar que mantiene URLs válidas y `FOUND`. En la tarjeta, romper la carga principal y secundaria para comprobar orden de fallback y retrato completo.
5. Sincronización normal: `FOUND`/`FAILED` y `NOT_FOUND` reciente se omiten con items y motivos; `RETRYABLE_ERROR` se reintenta de inmediato; `NOT_FOUND` vencido (30 días por defecto) vuelve a ser elegible. Preparar cambios de `active` durante el run: si el jugador está inactivo al comenzar su evaluación individual, no hay item, contadores ni solicitud; si está activo al comenzar y se inactiva después, su único item y resultado se completan, `evaluated` lo cuenta y `processed` solo aumenta si hubo intento contra TheSportsDB. En runs posteriores sigue excluido mientras esté inactivo y al reactivarse conserva su estado. Confirmar reparación de resolución faltante.
6. Simular timeout, red, `5xx`, `429` con `Retry-After` válido/ausente: tres reintentos posteriores al inicial por defecto, todos compartiendo intervalo mínimo de 2500 ms y cuota ≤30/min; sin cabecera válida esperar ≥60 s. Agotamiento ⇒ `RETRYABLE_ERROR`; `4xx` no 429 ⇒ `FAILED` individual. Probar colisión de referencia: conservar ambas identidades originales, incrementar `failed` y `conflicts`, run `PARTIAL`, continuar con demás jugadores.
7. Con JWT iniciar dos POST concurrentes: el segundo obtiene `409` sin run nuevo. Concluir uno con solo `NOT_FOUND` ⇒ `COMPLETED`; con error individual ⇒ `PARTIAL`; simular fallo global tras persistir items ⇒ error HTTP/run `FAILED` sin perder resultados previos.
8. Consultar historial, resumen y detalle paginados con y sin JWT; `evaluated` coincide con items de activos inspeccionados, `processed` excluye skips, `interrupted` no suma `failed`. Reiniciar con run `RUNNING` y un item incompleto: run `FAILED` con `INTERRUPTED_BY_RESTART`, item `INTERRUPTED`, items terminados intactos. Aplicar plazo de retención en entorno aislado y comprobar ausencia de items huérfanos.
