<a id="anexos"></a>
# ANEXOS — Ejemplos técnicos no normativos

Consultar el [punto de entrada](../architecture.md) y la [interpretación normativa](../architecture.md#interpretacion-normativa).

Los anexos son ilustrativos y no normativos. Muestran aplicaciones genéricas de las disposiciones existentes, sin añadir requisitos ni excepciones. Los nombres de Pages, Components y clases son neutrales y no identifican funcionalidades del proyecto.

<a id="anexo-a"></a>
## ANEXO A — Ejemplos de organización de directorios

Ilustra los Artículos [3.º](presentation.md#articulo-3) — Ubicación de Components de página, [8.º — APIs de carpetas modulares](../architecture.md#articulo-8) y [32.º — Fondos y responsabilidad visual](presentation.md#articulo-32). Su contenido es exclusivamente ilustrativo y no normativo.

Una Page con dos Components locales puede representarse así; el árbol no exige crear esos Components ni reproducir esta estructura sin una responsabilidad real:

```text
pages/
└── ExamplePage/
    ├── ExamplePage.tsx
    ├── ExamplePage.module.scss
    ├── ExampleHeader/
    │   ├── ExampleHeader.tsx
    │   ├── ExampleHeader.module.scss
    │   └── index.ts
    ├── ExampleContent/
    │   ├── ExampleContent.tsx
    │   ├── ExampleContent.module.scss
    │   └── index.ts
    └── index.ts
```

Como ejemplos de nombres neutrales de wrappers de Page, `.exampleSummary`, `.exampleCollection` y `.examplePage` ilustran el lugar al que corresponde el fondo de Page según el [Artículo 32.º](presentation.md#articulo-32). Los nombres no establecen una convención adicional.

<a id="anexo-b"></a>
## ANEXO B — Ejemplos de importaciones y APIs públicas

Ilustra los Artículos [5.º](presentation.md#articulo-5) — Imports del Design System y [36.º — Capas globales de estilos](presentation.md#articulo-36). Su contenido es exclusivamente ilustrativo y no normativo; las reglas sobre APIs públicas permanecen en los Artículos [6.º](../architecture.md#articulo-6), [8.º](../architecture.md#articulo-8) y [9.º](../architecture.md#articulo-9).

Ejemplo de import de abstracts mediante el alias establecido:

```scss
@use "@/styles/abstracts" as ds;
```

Contraejemplos de imports relativos, incompatibles con el [Artículo 5.º](presentation.md#articulo-5):

```scss
@use "../../styles/abstracts" as ds;
@use "../../../styles/abstracts" as ds;
@use "../../../../styles/abstracts" as ds;
@use "../../../../../styles/abstracts" as ds;
```

La notación abreviada `@use ".../styles/abstracts"` describe el destino de un import, no una ruta ejecutable ni una excepción a la exigencia del alias.
