# Research: Rediseño del historial personal

## Perfil actual sin snapshots

**Decision**: Reutilizar `MarketTransaction.player → Player.team → Team.league`. Nombre, imágenes,
equipo y liga se documentan como actuales; los datos financieros siguen copiándose del registro
append-only. No hay migraciones, backfills ni reinicialización.

## Consulta escalable

**Decision**: Paginar en PostgreSQL. La consulta de contenido carga buyer, seller, player, team y
league mediante asociaciones to-one en una sentencia; conteo y agregados usan consultas separadas
con los mismos predicados. No se consulta `PlayerRepository` por fila.

La búsqueda reutiliza `catalog_normalize_name`. Los índices actuales de buyer/seller por fecha se
conservan; cualquier índice adicional requiere evidencia del plan SQL. El `OR` comprador/vendedor
es el riesgo conocido.

## Agregados

**Decision**: Calcular conteos, tokens y volúmenes mediante agregación SQL sobre el conjunto
filtrado. No representan rentabilidad ni usan cotizaciones actuales.

## Orden y URL

**Decision**: Traducir un enum cerrado a `Sort`, nunca aceptar columnas arbitrarias. En frontend,
usar `useSearchParams`; los criterios forman parte de la query key y habilitan navegación atrás,
recarga y enlace reproducible.

## Presentación

**Decision**: Solo desktop. Reutilizar `PlayerPortrait`, `Pagination`, `Button`, iconos, formatters
y tokens. La región principal conserva la cadena de altura y el scroll interior existentes; los
estados centran contenido compacto sin expandir filas ni trasladar el scroll al documento. No se incorpora
navegación desde jugador.

## SPECs afectadas

- SPEC-020: contrato backend, modelo y afirmaciones sobre inmutabilidad del nombre.
- SPEC-025: alcance integral backend/frontend.
- SPEC-010/011: no cambian; siguen produciendo el mismo registro confirmado.
- SPEC-014/015: no cambian; no se altera contabilidad ni portfolio.
