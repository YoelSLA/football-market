# Migraciones Flyway con timestamps UTC

Las migraciones nuevas usan `VyyyyMMddHHmmss__descripcion.sql`. Las versiones históricas
V1–V10 siguen siendo válidas y no se convierten a timestamps. El generador usa el reloj UTC
de Java, independientemente de la zona horaria del equipo, y no necesita instalar Flyway
ni conectarse a PostgreSQL. Requiere el JDK del proyecto y el Gradle Wrapper.

## Crear una migración

Desde `backend/`, en Linux:

```bash
./gradlew createMigration -PmigrationDescription=add_market_transaction_index
```

Desde `backend/`, en Windows PowerShell:

```powershell
.\gradlew.bat createMigration -PmigrationDescription=add_market_transaction_index
```

En Windows CMD, usar `gradlew.bat` con los mismos argumentos.
El resultado está en `backend/src/main/resources/db/migration/`, por ejemplo
`V20261017143005__add_market_transaction_index.sql`. Completar el SQL antes del commit.
La tarea no está vinculada al build ni al arranque de la aplicación.

La descripción debe estar en inglés, en `snake_case` ASCII minúsculo, empezar con letra
y tener como máximo 100 caracteres. No se admiten espacios, extensiones ni rutas.
La tarea comprueba nombres de migraciones V existentes, versiones numéricas duplicadas
(incluso con ceros iniciales), fechas de versiones de 14 dígitos y que la nueva versión
supere todas las versiones locales. Mantiene compatibilidad con descripciones históricas.
Las migraciones repetibles `R__...` no participan del orden de versiones.

Si el reloj está atrasado o ya se generó una versión en ese segundo, falla sin generar
una versión futura artificial. Esperar o corregir el reloj antes de reintentar. Un bloqueo
local en `backend/.gradle/` serializa las invocaciones del generador en el mismo checkout;
la creación exclusiva evita sobrescribir archivos. No coordina checkouts ni equipos distintos.
La comparación es contra archivos locales, **no** contra `flyway_schema_history`.

## Ramas paralelas e integración

1. Antes de crear una migración, actualizar la rama con el procedimiento Git acordado y
   revisar las migraciones de la rama compartida. Los timestamps reducen las colisiones,
   pero dos equipos pueden generar la misma versión en el mismo segundo.
2. Antes de integrar, revisar versiones duplicadas, orden y dependencias SQL. Crear después
   no garantiza integrar después. El orden relevante de Flyway es numérico, no el del commit.
3. Si una migración **nunca fue aplicada en ninguna base**, se puede regenerar con un timestamp
   posterior al máximo integrado, trasladar su SQL y retirar solo el archivo no aplicado.
   Coordinarlo con todos los integrantes antes de renombrar o eliminar archivos.
4. Si ya fue aplicada, no renombrarla, editarla ni modificar sus checksums. Si llega una versión
   menor que la máxima aplicada, con el comportamiento normal de Flyway no se ejecutará en
   orden normal y puede bloquear la validación. Detener el despliegue y revisar el historial,
   las dependencias y el SQL con el responsable de la base.
5. Solo si se confirma que la migración pendiente es segura después de las ya aplicadas,
   el responsable puede autorizar `outOfOrder` **para una ejecución y base concretas** mediante
   su herramienta de despliegue. Si usa Spring Boot, la propiedad puntual equivalente es
   `spring.flyway.out-of-order=true`. Retirar el override después y verificar el historial.
   No versionar ese ajuste, no habilitarlo globalmente y no usarlo para resolver duplicados,
   errores de checksum o dependencias incompatibles. Si no es seguro, acordar una remediación
   específica; no ejecutar `repair`, `clean` ni editar el historial como atajo.

## Corrección histórica V9 → V10

`dev` contenía dos V9: auditoría de identidad y dominio de mercado. Se conserva V9 para
`create_player_identity_audit` y se renombra únicamente mercado a
`V10__create_market_domain.sql`, sin modificar sus bytes SQL. Sus dependencias son tablas
previas (`users` y `players`); no depende de objetos de la auditoría ni viceversa.

El renombrado conserva el checksum del contenido, pero **cambia la identidad de versión**.
No se inspeccionaron bases durante este cambio. Antes de usarlo sobre una base existente,
su responsable debe comprobar qué V9 figura aplicada. Si mercado ya fue aplicado como V9,
detenerse y acordar una reconciliación: no ejecutar automáticamente V10, `repair` ni editar
`flyway_schema_history`. La autorización del renombrado no autoriza alterar ninguna base.

## Verificaciones pendientes del usuario

Además del build sin tests, verificar en un checkout desechable Linux y Windows: creación
correcta, descripción ausente/inválida, versiones duplicadas, timestamp futuro o inválido,
dos invocaciones en el mismo segundo y preservación de archivos ante errores. Comprobar
también el orden SQL sobre una base de pruebas autorizada. Estas pruebas no fueron ejecutadas
por el agente y el build no acredita su resultado.
