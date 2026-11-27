# Migraciones Flyway con timestamps UTC

## Convención única

Todas las migraciones versionadas usan exclusivamente:

```text
VyyyyMMddHHmmss__description.sql
```

- `yyyyMMddHHmmss` es un timestamp UTC real, válido y de catorce dígitos.
- `description` está en inglés, `snake_case` ASCII, comienza con una letra y tiene hasta cien caracteres.
- No se admiten versiones ordinales, timestamps futuros, migraciones repetibles ni archivos SQL fuera de esta convención.
- La versión ordena la ejecución de Flyway y debe respetar las dependencias del esquema.

El historial vigente comienza en `V20261107030001__create_users.sql` y fue reconstruido para una
instalación limpia. No es compatible con `flyway_schema_history` del ciclo anterior: las bases de
desarrollo son descartables y deben recrearse por decisión de su responsable, nunca por la
aplicación ni por un agente.

## Crear una migración

Desde `backend/`:

```bash
./gradlew createMigration -PmigrationDescription=add_market_transaction_index
```

En Windows se utiliza `gradlew.bat` con los mismos argumentos. La tarea valida todos los nombres y
timestamps existentes, rechaza duplicados, toma el reloj UTC real, exige una versión posterior a la
máxima local y crea el archivo de forma exclusiva. El lock local no coordina otras ramas, checkouts
o equipos.

## Estados de publicación e inmutabilidad

Una migración está **integrada** cuando forma parte de `dev`. Desde ese momento es inmutable byte
por byte: no se modifica SQL, comentarios, formato, nombre, ubicación ni finales de línea; tampoco
se elimina o copia bajo otra versión. Toda corrección se expresa en una migración posterior.

Una migración que existe solo en una rama local y nunca fue integrada puede renombrarse o
regenerarse para resolver el orden antes del merge. Si esa migración ya fue aplicada en una base
PostgreSQL local, cambiarla invalida el checksum registrado: el responsable debe recrear esa base
descartable antes de volver a usarla. No se ejecuta `repair` para encubrir la modificación.

`.githooks/flyway-migrations.sh` compara una rama con `dev` y rechaza modificaciones, eliminaciones,
renombres y copias de migraciones integradas, además de nuevas versiones no posteriores a la máxima
publicada. CI ejecuta el mismo control. La excepción autocontenida del corte inicial deja de aplicar
cuando el marcador de la reconstrucción ya existe en `dev`.

## Ramas y PostgreSQL local

Cada desarrollador utiliza una base PostgreSQL local independiente. No se comparten bases entre
ramas con historiales distintos. Antes de generar y antes de integrar una migración se actualiza la
rama desde `dev`, se valida el layout y se revisan orden y dependencias.

Si otra rama integró una versión posterior, una migración todavía local puede regenerarse con un
timestamp UTC actual. Una migración ya integrada nunca se renumera. `outOfOrder` permanece
deshabilitado y no se usa como mecanismo normal de integración.

## Configuración segura

Desarrollo y testing declaran explícitamente:

```yaml
spring:
  flyway:
    validate-on-migrate: true
    out-of-order: false
    baseline-on-migrate: false
    clean-disabled: true
```

`repair`, `clean` y la edición de `flyway_schema_history` no forman parte del flujo normal. La
aplicación nunca elimina una base. La recreación local requiere una acción humana deliberada sobre
el destino exacto.

## Validaciones

`./gradlew validateMigrationLayout` es estática: no abre conexiones ni ejecuta SQL. `check` y Sonar
dependen de ella. La suite de integración cubre PostgreSQL 18 vacío, checkpoints incrementales,
`Flyway.validate()` y el esquema final; Hibernate conserva `ddl-auto: validate`.

Conforme a las reglas del repositorio, los agentes no ejecutan tests. Los comandos humanos son:

```bash
./gradlew validateMigrationLayout
./gradlew integrationTest
./gradlew build
```
