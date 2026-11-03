# Importación local de escudos

Desde `frontend/`:

```bash
node scripts/import-team-logos.mjs
node scripts/import-team-logos.mjs --refresh-teams
node scripts/import-team-logos.mjs --keep-pending
```

Requisitos existentes en el entorno: Node.js 24, Docker con PostgreSQL de desarrollo y Python 3 (parser XML de la biblioteca estándar). No instala paquetes. `FOOTBALL_DB_CONTAINER` permite seleccionar otro contenedor con el mismo esquema/base de desarrollo.

## Inventario y fuentes

`--keep-pending` conserva los pendientes sin repetir sus consultas; permite reanudar únicamente el registro de recursos ya incorporados (incluidos aliases), sin nuevas descargas.

La primera ejecución y `--refresh-teams` consultan **solo lectura** `teams JOIN leagues` en `football_market_dev`. Se incluyen todos los equipos persistidos, no una temporada externa ni solamente una página de jugadores. `teams.json` conserva nombres, IDs, liga, estado y fecha del inventario. Las siguientes ejecuciones reutilizan ese inventario; refrescarlo después de actualizar los equipos.

Los recursos pertenecen a la feature, en `src/features/players/assets/teams/<liga>/`. No son assets transversales de `shared`.

La búsqueda usa los mecanismos oficiales de Wikidata (`wbsearchentities`, `wbgetentities`) para confirmar nombre/alias y país, y obtener el logo declarado en P154. Solo acepta una identidad y un logo sin fecha de finalización ni señales de versión histórica. Los equipos ambiguos quedan pendientes: la clasificación automática no sustituye una revisión humana antes de publicar.

Commons (`query`, `imageinfo`, `extmetadata`) aporta procedencia y derechos. Se prefieren SVG; si P154 apunta a PNG se requiere alfa y al menos 128 px en cada dimensión. No se buscan sustitutos aproximados ni se descargan logos históricos. No se modifica el contenido de las imágenes.

Biome excluye únicamente los SVG externos de `src/features/players/assets/teams/**/*.svg`, incluidos futuros escudos importados. Sus títulos y propiedades exportadas por editores gráficos no se adaptan a reglas del código propio: se conservan los bytes y hashes originales. Esta exclusión no reemplaza la validación de seguridad del importador ni la revisión visual humana; el script, el mapeo TypeScript y los registros JSON siguen dentro del alcance de Biome. La accesibilidad de las imágenes se resuelve en los componentes que las presentan.

## Política conservadora de derechos

La incorporación automática admite únicamente **Public domain** o **CC0** con `AttributionRequired=false` y procedencia documentada. Otras licencias —incluidas CC BY/SA— quedan pendientes, sin descargar, hasta implementar y verificar todas sus obligaciones. Una declaración de Commons no es garantía jurídica universal: los derechos de marca subsisten y el uso no implica respaldo del club. Antes de publicación comercial se debe revisar el uso concreto y las restricciones territoriales.

`registry.json` registra cada equipo, estado, motivo si está pendiente y fecha. Para incorporados incluye archivo, entidad Wikidata, título Commons, URL de origen/descarga, autor, crédito, licencia, enlace a licencia, restricciones, hash SHA-256 y metadatos originales (incluidos avisos de derechos). Este registro debe distribuirse junto con los recursos. No presentar estos assets como propios ni eliminar sus avisos.

Referencias:

Los textos de metadatos procedentes de las páginas de Commons conservan su atribución mediante `sourceUrl`; están sujetos a CC BY-SA 4.0 (https://creativecommons.org/licenses/by-sa/4.0/). Los datos estructurados de Wikidata son CC0. Estas condiciones del registro son distintas de la licencia de cada imagen.

- https://www.wikidata.org/w/api.php
- https://commons.wikimedia.org/w/api.php
- https://commons.wikimedia.org/wiki/Commons:Licensing
- https://commons.wikimedia.org/wiki/Commons:Non-copyright_restrictions
- https://foundation.wikimedia.org/wiki/Policy:Terms_of_Use

## Reanudación, límites y seguridad

`index.ts` es el mapeo generado con imports estáticos de los recursos aprobados. Los archivos descartados se conservan como evidencia local y no se importan ni se empaquetan en la aplicación. No editar este archivo generado: reejecutar el importador.

Una sola solicitud a la vez, separadas al menos 1,1 segundos. Hasta tres intentos, timeout y espera adicional ante 429/503. Hay límite de 2 MB por recurso. Se valida XML con el parser estándar y se rechazan scripts, entidades, referencias externas y SVG activos; para PNG se comprueban cabecera, dimensiones y alfa. El proceso no renderiza imágenes ni acredita comprobación visual.

Persiste el registro después de cada equipo. Reutiliza archivos cuyo hash y estructura siguen válidos; no sobrescribe recursos existentes. Una alteración se registra como pendiente para revisión, no se corrige destructivamente. Una nueva ejecución reintenta los pendientes. No se dispara desde `build`, hooks ni el navegador.

`getTeamLogo` importa URLs empaquetadas por Vite: normaliza Unicode, espacios y mayúsculas y usa únicamente nombres y aliases de entidades verificadas. No elimina palabras en runtime ni hace coincidencias parciales. Aliases que colisionan se resuelven como `null`. Solo estados `downloaded` con asset existente entran en el mapeo. PlayerCard mantiene el placeholder tanto si falta el recurso como si falla su carga.
