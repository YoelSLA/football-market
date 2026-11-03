# Quickstart — SPEC-007 (operativo mínimo)

Documento operativo de validación, generado por la fase Plan de Spec Kit. Cubre solo lo necesario
para ejecutar el backend, sus pruebas, la recreación de la base DEV y la verificación del esquema
`Player → Team → League`. No describe decisiones de diseño: véanse [spec.md](spec.md),
[plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md),
[tasks.md](tasks.md) y [contracts/api.md](contracts/api.md).

Todo lo descrito aquí lo ejecuta una persona. El agente no ejecuta tests, builds ni operaciones
sobre PostgreSQL, y este documento no aprueba gates.

## 1. Requisitos para ejecutar el backend

- JDK 21 (`JavaLanguageVersion.of(21)` en `backend/build.gradle.kts`).
- Docker operativo: el perfil `dev` usa PostgreSQL propio y los tests usan Testcontainers.
- PostgreSQL 18 accesible, con la base DEV creada y vacía (§4).
- Variables de entorno:
  - `JWT_SECRET` — obligatoria, sin valor por defecto.
  - `THE_SPORTS_DB_API_KEY` — requerida por `application.yml` para el enriquecimiento.
  - `THE_SPORTS_DB_BASE_URL` — opcional, por defecto
    `https://www.thesportsdb.com/api/v1/json`.
  - football-data.org: las credenciales de esa API se configuran por properties del área de
    integraciones; no se documentan aquí ni se versionan.

Arranque:

```sh
docker compose up -d          # PostgreSQL 18 en localhost:5432, base football_market_dev
cd backend
./gradlew bootRun
```

`application.yml` fija `ddl-auto: validate` y `application-dev.yml` habilita Flyway sobre
`classpath:db/migration`. En el primer arranque contra una base vacía, Flyway crea el esquema y
Hibernate valida los mappings contra él.

## 2. Comandos de tests

Desde `backend/`, con Docker disponible para los contenedores de PostgreSQL de los tests:

```sh
./gradlew test                                   # suite completa
./gradlew test --tests '*TeamLeagueMigrationTest'          # instalación limpia y esquema final
./gradlew test --tests '*PlayerMigrationTest'              # historial V2–V3
./gradlew test --tests '*PlayerImageMigrationTest'         # historial V3–V4
./gradlew test --tests '*PlayerCatalogPersistenceTest'
./gradlew test --tests '*PlayerSnapshotConsistencyPersistenceTest'
./gradlew test --tests '*TeamProtectionPersistenceTest'
./gradlew test --tests '*TeamResolutionAttemptPersistenceTest'
./gradlew test --tests '*PendingReviewCaseTest'
./gradlew test --tests '*PlayerCatalogServiceTest'
./gradlew test --tests '*TeamIdentityAndRetirementTest'
./gradlew test --tests '*TeamEnrichmentServiceTest'
```

Build sin tests, permitido al agente tras modificar código del backend:

```sh
./gradlew build -x test
```

El build no compila ni ejecuta las pruebas y no acredita TESTING. Los tests de backend los ejecuta
siempre la persona.

## 3. Procedimiento de recreación de PostgreSQL DEV

Solo DEV. Este procedimiento **no** está autorizado por la aprobación de la feature y no se ejecuta
como parte de la implementación: se documenta para que la persona decida cuándo aplicarlo.

El historial de migraciones de SPEC-007 cambió de forma intencional. El binario nuevo **no** debe
aplicarse sobre la base creada por el historial anterior: sus checksums no coinciden y V5 ya crea el
esquema definitivo. No usar `flyway repair`.

Requisitos previos: confirmar que `football_market_dev` en `localhost:5432` está dedicada
exclusivamente a football-market y que no contiene datos que interese conservar.

```sh
# 1. Detener solo los procesos de football-market que escriben en la base.
#    No reiniciar PostgreSQL ni cerrar sesiones de otros proyectos.

# 2. Recrear únicamente la base del proyecto.
docker compose stop                            # detiene el servicio postgres de ESTE compose
docker compose down -v                         # elimina el volumen postgres_data de ESTE compose
docker compose up -d

# 3. Arrancar el backend: Flyway aplica V1–V5 sobre la base vacía.
cd backend && ./gradlew bootRun
```

`docker compose down -v` borra el volumen `postgres_data` del `compose.yml` del repositorio. Antes
de ejecutarlo, confirmar que ese volumen contiene solo datos descartables de football-market; en
caso dudoso, detenerse y pedir confirmación. Para conservar una copia previa usar `pg_dump` sobre
`football_market_dev` antes del paso 2.

## 4. Verificación de Flyway V1–V5

En una instalación limpia el historial debe quedar `V1 V2 V3 V4 V5`: sin V5.1, sin V6 y sin
migraciones de transición.

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Resultado esperado: cinco filas, `success = true`, versiones `1` a `5` sin huecos ni
version=`5.1` o `6`.

Comprobación del contenido físico de las migraciones:

```sh
ls backend/src/main/resources/db/migration
```

Debe listar únicamente V1, V2, V3, V4 y `V5__create_team_league_domain.sql`.

## 5. Verificación del esquema Team/League

Sobre la base ya migrada, en una transacción de solo lectura:

```sql
BEGIN READ ONLY;

-- 1. No existen columnas legacy de equipo ni de liga en players.
SELECT column_name, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'players'
ORDER BY column_name;

-- 2. Las tablas del dominio definitivo existen y no hay tablas retiradas.
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- 3. Todo Player está asociado y la liga se deriva del Team.
SELECT count(*) FILTER (WHERE p.team_id IS NULL) AS players_sin_team,
       count(*) FILTER (WHERE l.id IS NULL)     AS players_sin_league
FROM players p
LEFT JOIN teams t ON t.id = p.team_id
LEFT JOIN leagues l ON l.id = t.league_id;

-- 4. Restricciones de integridad y unicidad vigentes.
SELECT conrelid::regclass AS tabla, conname, contype
FROM pg_constraint
WHERE connamespace = 'public'::regnamespace
  AND conrelid::regclass::text IN
      ('players', 'teams', 'leagues', 'player_external_references',
       'team_external_references', 'league_external_references', 'pending_review_cases')
ORDER BY 1, 2;

COMMIT;
```

Criterios:

- `players` contiene `team_id` y no contiene `team` ni `league`; `team_id` es `NOT NULL`.
- No existen `catalog_transition`, `review_case_revisions`, `review_evidence_members` ni
  `review_evidence_results`.
- `players_sin_team` y `players_sin_league` son `0` sobre un catálogo sincronizado.
- Las FK `players.team_id → teams.id` y `teams.league_id → leagues.id` aparecen como `contype = 'f'`,
  y las unicidades `(provider, external_id)` y `(owner, provider)` de las tres tablas de referencias
  como `contype = 'u'`.

Sobre la base recién creada el catálogo está vacío: la comprobación 3 solo es significativa después
de la primera sincronización. Para obtenerla, ejecutar antes `POST /api/players/sync`.
