# Dependencias del frontend

Matrices y condiciones de dependencias entre roles y módulos. Los contratos de cada rol permanecen en sus artículos, sin duplicar las matrices.

Consultar el [punto de entrada](../architecture.md), la [interpretación normativa única](../architecture.md#interpretacion-normativa) y las [observaciones de consistencia](../architecture.md#consistencia). Las matrices de [dependencias](dependencies.md) se consultan junto con el rol afectado.

## Índice

- [Artículo 41.º — Dependencias dentro de una feature](#articulo-41)
- [Artículo 42.º — Composición de Hooks y uso de shared](#articulo-42)
- [Artículo 43.º — Dependencias globales](#articulo-43)
- [Artículo 44.º — Prohibiciones expresas](#articulo-44)
- [Artículo 45.º — Alcance de las matrices y roles conceptuales](#articulo-45)

## CAPÍTULO ÚNICO — Matrices y límites

<a id="articulo-41"></a>
### Artículo 41.º — Dependencias dentro de una feature

**MAY:** Los roles de una feature utilizan las dependencias directas de la siguiente matriz, dentro de las condiciones documentadas:

| Origen | Destinos permitidos |
| --- | --- |
| Page | Component de la misma feature o shared, Page Hook o Navigation Hook de la misma feature, Model. |
| Component | Component de la misma feature o shared, Hook de la misma feature, Form Hook, Model, Form Model, estado de sesión de `infrastructure/storage` para la pantalla de verificación. |
| Page Hook | Form Hook, Query Hook, Mutation Hook, Navigation Hook de la misma feature, Model, Form Model, estado de sesión de `infrastructure/storage`, `infrastructure/http` para inspección genérica de errores. |
| Query Hook | Service de la misma feature, Model. |
| Mutation Hook | Service de la misma feature, Model, Form Model. |
| Navigation Hook | Hook de la misma feature, Model, estado de sesión de `infrastructure/storage`. |
| Service | Mapper, Model, DTO, Form Model, `infrastructure/http`, adaptador público de `infrastructure`, Service de la misma feature. |
| Mapper | Model, DTO, Form Model, utilidad de expiración de `infrastructure/storage` para la sesión. |
| Form Hook | Model, Form Model, schema o validación de la misma feature, `shared/validation`. |
| Form Model o schema | Model, validación de la misma feature, `shared/validation`. |
| Store de sesión de infraestructura | Persistencia de sesión de `infrastructure/storage`. |
| Model | Ninguna capa técnica o de aplicación. |

**MUST:** Todo formulario depende de su Form Hook conforme al [Artículo 21.º](roles.md#articulo-21). La fila específica de Form Hook, ubicado en `form/hooks/`, define sus dependencias permitidas; `form/schemas/` cumple el rol de schema de la matriz. El Page Hook correspondiente recibe el Form Model y coordina la operación con el Mutation Hook.

**MAY:** La dependencia `Service → Service` existe solo dentro de la misma feature bajo el [Artículo 14.º](roles.md#articulo-14). La dependencia `Service → infrastructure` utiliza solo la API pública de un adaptador técnico necesario, sin autorizar accesos directos al mecanismo concreto.

**MUST NOT:** Un Form Hook depende de Query Hooks o Mutation Hooks para evadir sus fronteras.

<a id="articulo-42"></a>
### Artículo 42.º — Composición de Hooks y uso de shared

**MAY:** `Hook → Hook` dentro de la misma feature se utiliza por composición o coordinación real, no simplemente para reutilizar una request.

**MUST:** Form Hook conserva su responsabilidad específica y no encadena arbitrariamente Query/Mutation Hooks.

**MAY:** Una dependencia hacia una API pública de `shared` existe solo si su responsabilidad ya está autorizada para el consumidor. Page y Component utilizan Components compartidos; Service utiliza `infrastructure/http`; Form utiliza `shared/validation`; y los roles utilizan utilidades puras compartidas que no introduzcan una dependencia prohibida.

<a id="articulo-43"></a>
### Artículo 43.º — Dependencias globales

**MAY:** Los módulos utilizan las dependencias globales de la siguiente matriz, dentro de las condiciones documentadas:

| Origen | Destinos permitidos |
| --- | --- |
| `app` | APIs públicas de features y `shared`, API pública de `infrastructure`. |
| Feature | APIs públicas de `shared` compatibles con su rol y de `infrastructure` conforme a la matriz anterior. |
| Service de feature | API pública de `infrastructure` según la regla anterior. |
| `infrastructure/http` | API pública de `infrastructure/storage` cuando sea necesaria. |
| `shared` | Ningún módulo específico de feature. |
| `infrastructure` | Ningún módulo específico de feature; el store de sesión utiliza el adaptador de almacenamiento del mismo módulo. |

<a id="articulo-44"></a>
### Artículo 44.º — Prohibiciones expresas

Aplican las [reglas arquitectónicas comunes](../../architecture.md). Restricciones expresas:

a) **MUST NOT:** Page o Component depende de Service, HTTP o DTO.

b) **MUST NOT:** Hook depende de DTO o define endpoints HTTP.

c) **MUST NOT:** Service depende de React, Component, Page, Hook o TanStack Query.

d) **MUST NOT:** Model depende de React, Hook, Service, HTTP, DTO o infrastructure.

e) **MUST NOT:** Mapper depende de Service, Hook, HTTP o React. El mapper de sesión utiliza la utilidad de expiración de `infrastructure/storage`.

f) **MUST NOT:** Una feature depende de otra feature.

g) **MUST NOT:** `shared` e `infrastructure` dependen de features.

h) **MUST NOT:** Un módulo utiliza `app`, barrels o intermediarios para evadir estas reglas.

<a id="articulo-45"></a>
### Artículo 45.º — Alcance de las matrices y roles conceptuales

La matriz regula dependencias entre roles arquitectónicos y se interpreta con el alcance definido por las [reglas arquitectónicas comunes](../../architecture.md) §1.

**MAY:** Se utilizan tipos del lenguaje, APIs estándar y dependencias técnicas compatibles con la responsabilidad del componente aunque no aparezcan enumerados.

En esta matriz `Model`, `Form Model` y `DTO` son roles conceptuales distintos aunque sus definiciones se expongan mediante `types/index.ts`: `types/models.ts` contiene los primeros y `types/dtos.ts` los contratos HTTP.

**MUST NOT:** Un consumidor utiliza imports o reexports para acceder a DTO fuera del límite de Service y Mapper.
