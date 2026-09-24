# Convenciones del frontend

## Guía de lectura

Consultar la [arquitectura del frontend](architecture.md) para responsabilidades, ubicaciones y dependencias; este documento regula naming, formato y estilo de implementación. Ninguna convención autoriza a evadir las responsabilidades, límites ni restricciones arquitectónicas. El stack, las versiones y las dependencias tecnológicas se documentan en [tecnologías del frontend](technologies.md); la configuración efectiva prevalece.

## TypeScript

- En el código TypeScript propio está prohibido utilizar explícitamente `any`; esta regla no exige modificar código generado ni declaraciones externas. Preferir tipos concretos y utilizar `unknown` con el narrowing correspondiente cuando el valor sea realmente desconocido.
- Nombrar interfaces, aliases de tipos y demás tipos nominales en `PascalCase`; variables y funciones, en `camelCase`. Mantener una convención consistente para las constantes según su naturaleza y alcance, distinguiendo las constantes compartidas de los valores locales.
- Los tipos que representen contratos HTTP terminan en `DTO`, en mayúsculas. Mantener separados los tipos de la lógica ejecutable cuando así lo determina la arquitectura.
- Evitar assertions de tipos utilizadas únicamente para silenciar al compilador. Explicitar tipos en las fronteras cuando ayude a expresar el contrato del módulo; evitar anotaciones redundantes cuando la inferencia de TypeScript sea suficiente.

## React

- Nombrar Components y Pages en `PascalCase`; los Hooks comienzan con `use` y reciben nombres que expresan su responsabilidad.
- Components y Hooks respetan las responsabilidades y dependencias de [arquitectura](architecture.md). No crearlos ni extraer abstracciones solo para reducir líneas: la extracción requiere cohesión, reutilización, encapsulación o separación conceptual.
- Evitar nombres genéricos cuando exista un concepto funcional más preciso. Mantener el estado en el alcance más reducido compatible con su responsabilidad.

## Archivos e imports

- Los archivos de Components y Pages usan `PascalCase`. Los archivos de Hooks usan `camelCase` y empiezan con `use`. Utils, Services, schemas, Mappers y unidades equivalentes tienen nombres descriptivos y consistentes con la responsabilidad principal y la unidad que definen; evitar nombres genéricos o residuales.
- Respetar las APIs públicas y las restricciones de imports de [arquitectura](architecture.md): no realizar deep imports ni usar barrels para evadirlas; evitar dependencias circulares.
- Utilizar los aliases configurados para imports entre módulos arquitectónicos. Dentro del mismo módulo pueden utilizarse imports relativos cuando corresponda. Mantener los imports organizados conforme a las herramientas de formato y linting del proyecto.

## SCSS

- Todos los estilos SCSS utilizan BEM para nombrar clases en `kebab-case`, distinguiendo Block, Element y Modifier. Cada Page o Component con estilos propios tiene un bloque raíz identificable cuando corresponda; los elementos se nombran según su pertenencia conceptual al bloque. Los modificadores representan variantes o estados y no sustituyen la clase base.
- Utilizar el nesting de SCSS con `&` para expresar elementos, modificadores y pseudoestados de su clase BEM. Mantener las variantes y los pseudoestados asociados a esa clase; no reproducir innecesariamente la jerarquía del DOM ni profundizar el nesting estructural. Evitar selectores dependientes de la estructura HTML, IDs para styling y `!important` salvo necesidad técnica justificada. Evitar clases genéricas cuando pertenecen a un bloque concreto.
- Indentar con tabulaciones cada nivel de bloque, escribir una declaración por línea y desarrollar selectores y reglas anidadas de forma legible, sin comprimir bloques en una sola línea. Mantener el formato compatible con Biome; el formateador resuelve las cuestiones puramente mecánicas que no tengan una regla específica del proyecto.
