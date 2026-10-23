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
- Components y Hooks respetan las responsabilidades de [roles y flujos](architecture/roles.md) y las [dependencias arquitectónicas](architecture/dependencies.md). No crearlos ni extraer abstracciones solo para reducir líneas: la extracción requiere cohesión, reutilización, encapsulación o separación conceptual.
- Evitar nombres genéricos cuando exista un concepto funcional más preciso. Mantener el estado en el alcance más reducido compatible con su responsabilidad.
- Exports, interfaces locales `Props` y barrels de Components siguen los [artículos 2.º y 8.º de arquitectura](architecture.md#articulo-2). Se comparten estructuras de datos, nunca la interfaz de props de un Component.

## Archivos e imports

- Los archivos de Components y Pages usan `PascalCase`. Los archivos de Hooks usan `camelCase` y empiezan con `use`. Utils, Services, schemas, Mappers y unidades equivalentes tienen nombres descriptivos y consistentes con la responsabilidad principal y la unidad que definen; evitar nombres genéricos o residuales.
- Respetar las [APIs públicas](architecture.md#articulo-8) y las [restricciones de dependencias](architecture/dependencies.md): no realizar deep imports ni usar barrels para evadirlas; evitar dependencias circulares.
- Utilizar los aliases configurados para imports entre módulos arquitectónicos. Dentro del mismo módulo pueden utilizarse imports relativos cuando corresponda. Mantener los imports organizados conforme a las herramientas de formato y linting del proyecto.

## SCSS

- Todos los estilos SCSS utilizan BEM para nombrar clases, distinguiendo Block, Element y Modifier. Cada Page o Component con estilos propios tiene un bloque raíz identificable cuando corresponda; los elementos se nombran según su pertenencia conceptual al bloque. Los modificadores representan variantes o estados y no sustituyen la clase base.
- Las clases se nombran de modo que **el objeto de estilos siempre se acceda con notación de punto**: `styles.portfolioMetric__label` y nunca `styles["portfolio-metric__label"]`. Para que eso sea posible, el nombre de la clase debe ser un identificador válido: el bloque va en `camelCase` y los separadores BEM se conservan —`__` separa el elemento del bloque y `_` separa el estado del elemento—. Así, `.portfolioMetric__value_current` se consume como `styles.portfolioMetric__value_current`.
- El bloque raíz toma el nombre del componente en `camelCase` —`PlayerCard` produce `.playerCard`—, de modo que el bloque, el nombre de archivo y el nombre del módulo queden alineados.
- Cuando una clase dependa de un valor dinámico, se declara un mapa de clases estáticas por variante en lugar de interpolar el nombre —`METRIC_ICON_CLASSES[key]`—: la interpolación exigiría notación de corchete y rompería la regla anterior.
- Utilizar el nesting de SCSS con `&` para expresar elementos, modificadores y pseudoestados de su clase BEM. Mantener las variantes y los pseudoestados asociados a esa clase; no reproducir innecesariamente la jerarquía del DOM ni profundizar el nesting estructural. Evitar selectores dependientes de la estructura HTML, IDs para styling y `!important` salvo necesidad técnica justificada. Evitar clases genéricas cuando pertenecen a un bloque concreto.
- Dentro de un modificador de bloque, seleccionar sus elementos por su clase BEM (por ejemplo, `.layout_floating .layout__content`); `&__content` dentro de `.layout_floating` genera otra clase, no un descendiente. Revisar los selectores CSS emitidos y sus clases JSX después de modificar el nesting.
- No referenciar desde JSX una clase raíz que Sass solo utiliza como prefijo de elementos y no emite. Si el contenedor tiene una responsabilidad visual propia, conservar sus declaraciones al extraer el componente; en caso contrario, omitir la referencia, sin añadir CSS artificial. La compilación no acredita que una clave de CSS Modules exista ni que un selector alcance al elemento previsto.
- Indentar con tabulaciones cada nivel de bloque, escribir una declaración por línea y desarrollar selectores y reglas anidadas de forma legible, sin comprimir bloques en una sola línea. Mantener el formato compatible con Biome; el formateador resuelve las cuestiones puramente mecánicas que no tengan una regla específica del proyecto.
