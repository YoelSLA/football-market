# Guía de validación: caché de consultas frecuentes

Esta guía es para la ejecución humana de las pruebas. El agente no ejecuta tests ni builds que
puedan dispararlos. Requiere el backend y PostgreSQL de testing conforme a
`docs/backend/testing.md`; no usar datos ni credenciales productivos.

## Escenarios

1. **Carga fría y repetición**: para cada uno de los seis GET, llamar una vez (fallo controlado si
   no hay datos) y repetir la misma consulta. Confirmar misma respuesta contractual y observar que
   la consulta equivalente reutiliza el resultado durante su TTL.
2. **Separación de claves**: variar de a uno filtros, orden, página, tamaño e `includeStatistics`
   donde aplique; confirmar que cada variante corresponde a su consulta, sin respuesta de otra clave.
3. **Aislamiento P2P**: consultar con dos JWT de usuarios diferentes usando idénticos parámetros.
   Confirmar que resultados con datos personalizados no cruzan usuarios; repetir con el mismo usuario
   y criterios para verificar reutilización.
4. **Invalidación del recálculo**: precargar ranking y mercado oficial, completar un recálculo, y
   volver a consultar. La respuesta posterior debe reflejar el estado publicado.
5. **Invalidación del catálogo**: precargar filtros, listado y detalle, sincronizar el catálogo con
   éxito y repetir las lecturas. Deben reflejar el catálogo confirmado.
6. **Invalidación del mercado**: precargar las consultas de mercado afectadas, publicar o cancelar
   una oferta P2P, completar su compra (venta para el vendedor), realizar una compra oficial o una
   recompra oficial y repetir lecturas. Cada operación exitosa debe invalidar únicamente el mercado
   afectado; una mutación fallida/revertida no debe comunicarse como completada.
7. **Expiración, capacidad y reinicio**: con el reloj o configuración de tests controlada, verificar
   el TTL y límite individual fijado en FR-004. Reiniciar el backend y confirmar que la primera
   lectura vuelve a consultar PostgreSQL y conserva la respuesta HTTP.

## Cobertura por capas

- Tests de Controller existentes actualizados solo si la integración de caché altera el contexto o
  contrato HTTP; confirmar las mismas respuestas y errores.
- Tests de Service/configuración para claves, aciertos, expiración, límites e invalidación en los
  puntos de éxito/fallo/commit correspondientes.
- Pruebas de persistencia con PostgreSQL real cuando la responsabilidad de Service lo requiera;
  respetar categorías y fronteras de `docs/backend/testing.md`.

## Comandos humanos

Desde `backend/`, ejecutar selectivamente las tareas Gradle que correspondan a los tags y
responsabilidades de los tests agregados, conforme a `docs/backend/testing.md`. Para el build
técnico sin tests: `./gradlew build -x test`. El resultado debe registrarse por la persona que lo
ejecuta; escribir tests o compilar no acredita TESTING.
