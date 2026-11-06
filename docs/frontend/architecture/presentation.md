# Organización visual y recursos del frontend

Encapsulación de Components de Page, estilos, distribución de alturas y recursos compartidos. No reemplaza las convenciones de implementación.

Consultar el [punto de entrada](../architecture.md), la [interpretación normativa única](../architecture.md#interpretacion-normativa) y las [observaciones de consistencia](../architecture.md#consistencia). Las matrices de [dependencias](dependencies.md) se consultan junto con el rol afectado.

## Índice

- [Artículo 3.º — Ubicación de Components de página](#articulo-3)
- [Artículo 4.º — Delegación visual y Components locales](#articulo-4)
- [Artículo 5.º — Imports del Design System](#articulo-5)
- [Artículo 31.º — Cadena de altura](#articulo-31)
- [Artículo 32.º — Fondos y responsabilidad visual](#articulo-32)
- [Artículo 33.º — Scroll y responsive](#articulo-33)
- [Artículo 34.º — Prevención de regresiones visuales](#articulo-34)
- [Artículo 35.º — API única de iconos](#articulo-35)
- [Artículo 36.º — Capas globales de estilos](#articulo-36)
- [Artículo 37.º — Procedencia y tolerancia a fallos de recursos](#articulo-37)

## CAPÍTULO PRIMERO — Components locales e imports

<a id="articulo-3"></a>
### Artículo 3.º — Ubicación de Components de página

**MUST:** Los Components creados exclusivamente para una Page se ubican bajo la subcarpeta de componentes locales de esa Page (`pages/<Page>/components/<Component>/`).

**MUST NOT:** Esos Components se colocan en `features/<feature>/components/` ni se dispersan en la raíz de `pages/<Page>/`.

**MUST:** La carpeta `components/` de la feature queda reservada para Components **compartidos entre distintas Pages o funcionalidades** de la feature.

**MUST NOT:** Se mueven Components compartidos existentes a carpetas locales si otros consumidores los utilizan.

**MUST:** La colocalización es jerárquica y recursiva en todo `frontend/src/`, incluidos `app`, layouts, providers y `shared`. Un Component privado de otro reside dentro de la carpeta de su propietario. Un Component compartido entre sus hijos permanece en el nivel común más cercano; entre Pages, en `components/` de la feature; entre features, en `shared/components/` únicamente si es genérico. Se comprueban consumidores efectivos y responsabilidad de composición: un único importador no demuestra por sí solo propiedad. Las fronteras arquitectónicas prevalecen sobre el anidamiento físico; por ejemplo, la UI de sesión sigue en auth aunque la consuma el guard global.

**MUST:** Cada Component local de Page tiene su propia carpeta con:
- Archivo TSX de implementación
- SCSS Module propio
- `index.ts` barrel para exportar su API pública

Esta regla refuerza la encapsulación y cohesión: los componentes de página viajan junto a la página que los usa, y su ciclo de vida está ligado a ella.

Ejemplo ilustrativo: véase el [Anexo A](examples.md#anexo-a).

<a id="articulo-4"></a>
### Artículo 4.º — Delegación visual y Components locales

**MUST:** Las Pages actúan como orquestadores declarativos de la vista. Se encargan principalmente de:
- Componer la estructura visual declarativa de la pantalla.
- Conectar hooks y estado cuando corresponda.
- Configurar layouts.
- Coordinar el flujo unidireccional de Props hacia sus componentes hijos.

**MAY:** Cuando una Page presenta una estructura sencilla sin responsabilidades visuales complejas, compone sus elementos declarativamente de forma directa, sin obligatoriedad de extraer componentes ni crear hooks artificiales.

**MUST:** Cuando una Page presenta responsabilidades visuales, estructurales o interactivas diferenciadas, las delega a Components locales propios bajo `pages/<Page>/components/<Component>/` conforme al principio de responsabilidad única (SRP), manteniendo alta cohesión y bajo acoplamiento.

Criterios de extracción basados en SRP:
- **Responsabilidad visual o funcional independiente:** Áreas con comportamiento, presentación o interacción delimitada (por ejemplo, encabezados contextuales, paneles de métricas o formularios).
- **Lógica condicional o iterativa:** Cuadrículas, tablas o listados con renderizado condicional de colecciones.
- **Encapsulación de estilos:** Necesidad de aislar reglas CSS/BEM en su propio módulo SCSS para evitar selectores profundamente anidados o clases masivas en el contenedor superior.
- **Sub-elementos con identidad propia:** Componentes locales complejos descomponen recursivamente sus partes privadas en subcomponentes locales cuando cada una gestiona una preocupación visual específica (por ejemplo, filas, tarjetas, avatares o desgloses).

**MUST NOT:** Se crean componentes monolíticos que concentren maquetado DOM intrincado, múltiples ramas condicionales no triviales y responsabilidades dispares en un único archivo.

**MUST NOT:** Se extraen elementos triviales, contenedores vacíos o abstracciones sin responsabilidad clara. Evitar la fragmentación excesiva y la indirección cognitiva innecesaria es el propósito de esta restricción.

#### Arquetipos funcionales orientativos

A título puramente ilustrativo y orientativo, las responsabilidades diferenciadas de una Page suelen agruparse conceptualmente en:
- **Contenedores de layout o estructura:** Gestionan el encuadre visual, la cadena de altura o el scroll.
- **Encabezados:** Proporcionan contexto, títulos, resumen o acciones globales.
- **Controles y filtros:** Capturan criterios de búsqueda, selección o parámetros de visualización.
- **Visualización principal:** Presentan los datos nominales (tablas, cuadrículas, listados, formularios).
- **Paginación y rangos:** Navegación entre conjuntos de datos segmentados.
- **Representación de estados:** Información visual de carga, errores o vacíos.

**MUST NOT:** Estos arquetipos se interpretan como estructuras obligatorias, nombres fijos o jerarquías rígidas. Cada pantalla determina qué componentes requiere según su necesidad funcional real.

#### Modularización de estados de UI

**SHOULD:** La representación de estados transitorios o no nominales (carga, error con opción de reintento, ausencia de registros o filtros sin coincidencias) se modulariza según las necesidades de la pantalla y la experiencia de usuario:
- Mediante un componente coordinador de estados cuando los estados alternativos reemplazan globalmente la vista principal.
- Mediante componentes de estado localizados en la región visual afectada cuando otros elementos (como encabezados o barras de filtros) deban permanecer visibles e interactivos.

**MUST:** Los Components locales de Page siguen SRP: cada uno gestiona una preocupación visual diferenciada y recibe datos y acciones mediante Props.

**MUST NOT:** Los Components locales de presentación gestionan por sí mismos el estado global o de carga/error de la Page, o conectan directamente Services o HTTP evadiendo el flujo `Page / Component → Hook → Service`.

<a id="articulo-5"></a>
### Artículo 5.º — Imports del Design System

**MUST:** Todos los archivos SCSS que importen los abstracts del design system utilizan **exclusivamente** el alias `@`.

**MUST NOT:** Se utilizan imports relativos para esos abstracts.

Esta regla aplica a **todos** los archivos `.scss` bajo `frontend/src/`. El alias `@` está configurado en `tsconfig.json` y `vite.config.ts` y resuelve a `src/`.

La forma canónica es `@use "@/styles/abstracts" as ds;`.

Ejemplos ilustrativos: véase el [Anexo B](examples.md#anexo-b).

## CAPÍTULO SEGUNDO — Layouts y distribución visual

<a id="articulo-31"></a>
### Artículo 31.º — Cadena de altura

Para garantizar un comportamiento consistente y prevenir regresiones visuales en las páginas privadas que usan `PageLayout`, se establecen las siguientes reglas:

Cadena de altura (height chain):

a) **MUST:** `AppLayout` fija la altura al viewport (`height: 100vh / 100dvh`) y oculta el overflow del documento.

b) **MUST:** `AppLayout__content` es un flex container columna con `flex: 1`, `min-height: 0`, `overflow: hidden`.

c) **MUST:** `PageLayout` envuelve la totalidad de cada Page privada dentro de `AppLayout`, recibe únicamente `children` y participa en la cadena de altura con `display: flex`, `flex: 1`, `flex-direction: column`, `min-height: 0` y `min-width: 0`.

d) **MUST NOT:** `PageLayout` impone regiones de cabecera, contenido o pie, variantes de composición, paginación ni semántica de documento. Cada Page define esas responsabilidades en su propio wrapper.

e) **MUST:** El wrapper de cada Page tiene `display: flex`, `flex: 1`, `flex-direction: column`, `min-height: 0` y `min-width: 0`. La Page define una única autoridad principal de scroll vertical cuando la necesita y puede mantener fuera de ella sus regiones fijas.

f) **MUST:** `PageLayout` limita el overflow en desktop para preservar el espacio asignado por `AppLayout`; en móvil libera ese límite para permitir el scroll documental natural.

<a id="articulo-32"></a>
### Artículo 32.º — Fondos y responsabilidad visual

a) **MUST:** El fondo de Page (sólido, gradiente o imagen) se declara en el wrapper de la Page.

b) **MUST NOT:** `PageLayout` define fondo propio. El wrapper y las regiones internas de cada Page conservan su responsabilidad visual.

c) **MUST:** El posicionamiento, la reserva de espacio y el fondo de una paginación pertenecen a la Page que la compone; no se implementan mediante variantes de `PageLayout`.

Ejemplos ilustrativos de wrappers: véase el [Anexo A](examples.md#anexo-a).

<a id="articulo-33"></a>
### Artículo 33.º — Scroll y responsive

a) **MUST:** En desktop (≥ tablet), el documento no scrollea. Cada Page que lo necesite define una única región vertical principal dentro de `PageLayout`; overlays y controles con desplazamiento independiente justificado no alteran esa autoridad.

b) **MUST:** En móvil (< tablet), `PageLayout` y la región vertical principal de la Page liberan el overflow para que el scroll vuelva al documento natural.

c) **MUST:** Las Pages que posicionan una paginación en modo compacto son responsables de anclarla y reservar su banda sin superponer el contenido.

<a id="articulo-34"></a>
### Artículo 34.º — Prevención de regresiones visuales

a) **MUST NOT:** Se agrega fondo, padding o composición específica de una feature en `PageLayout`.

b) **MUST NOT:** Se usan alturas fijas (`height: Xpx`) en contenedores de Page.

c) **MUST:** Se utiliza la cadena de flex con `min-height: 0`. Al crear una nueva Page privada, `PageLayout` envuelve su wrapper completo y este sigue la cadena de altura (`flex: 1`, `flex-direction: column`, `min-height: 0`).

d) **MUST:** Se verifica visualmente en desktop, tablet, móvil y modo compacto tras cualquier cambio en layouts compartidos.

## CAPÍTULO TERCERO — Estilos y recursos gráficos

<a id="articulo-35"></a>
### Artículo 35.º — API única de iconos

**MUST:** `shared/icons` es el **punto único de exportación de los iconos** de la aplicación. Reexporta desde `lucide-react` cada icono que usa Football Market, junto con el tipo `LucideIcon`. Todos los consumidores utilizan `@/shared/icons`. Cada icono se exporta una sola vez con su nombre canónico y es el consumidor quien le da significado mediante su etiqueta, su texto alternativo y su rol en la interfaz.

**MUST NOT:** Los consumidores importan de `lucide-react` directamente. El módulo `shared/icons` importa de `app` o de `features`.

Así, el inventario de iconos queda declarado en un solo lugar, un cambio de biblioteca se resuelve en ese módulo y no en cada consumidor, y el módulo no introduce ciclos.

<a id="articulo-36"></a>
### Artículo 36.º — Capas globales de estilos

**MUST:** `src/styles` se organiza en dos capas globales: `styles/base/` con el reset y los estilos elementales, y `styles/abstracts/` con los símbolos compartidos —`breakpoints`, `tokens/` (colores, espaciado, layout) y `themes/`—. `styles/index.scss` es el único punto de entrada global y delega el reset en `base/`. Los valores de presentación se consumen desde `abstracts`.

Los símbolos compartidos se consumen mediante `@use`. La exigencia del alias se conserva en el [Artículo 5.º](#articulo-5); véase el [Anexo B](examples.md#anexo-b) para las notaciones ilustrativas.

La divergencia documentada sobre colores se conserva en [Estilos de la implementación](implementation.md#estilos-de-la-implementacion).

<a id="articulo-37"></a>
### Artículo 37.º — Procedencia y tolerancia a fallos de recursos

**MUST:** Se documentan ambas procedencias de los recursos gráficos decorativos:

a) **Recursos locales:** importados en tiempo de build y empaquetados por Vite.

b) **Servicios externos:** consultados en runtime.

**MUST:** Los servicios externos tienen marcador de posición accesible y comportamiento tolerante a fallos conforme a `tp/enunciado.md` §8.

Ninguno de los dos constituye un contrato de datos ni una ampliación de la API del backend.

Procedencias concretas: véase [Recursos de la implementación](implementation.md#recursos-de-la-implementacion).
