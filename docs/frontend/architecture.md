# Arquitectura del frontend

## Guía de lectura

Consultar primero las [reglas arquitectónicas comunes](../architecture.md) y luego las especificaciones pertinentes del mapa de consulta de este documento. Las reglas de naming, formato y estilo de implementación están en [convenciones del frontend](conventions.md); las tecnologías, versiones y dependencias, en [tecnologías del frontend](technologies.md). Estos documentos se consultan según el área afectada.

## Índice

- [Interpretación de términos normativos](#interpretacion-normativa)
- [Mapa de consulta](#mapa-de-consulta)
- [Organización general](#organización-general)
- [Identificadores y correspondencia de los 46 artículos](#identificadores-y-compatibilidad)
- [Correspondencia de secciones anteriores](#correspondencia-de-secciones-anteriores)
- [Observaciones de consistencia](#consistencia)

<a id="interpretacion-normativa"></a>
## Interpretación de términos normativos

Este documento adapta el vocabulario de [RFC 2119](https://www.rfc-editor.org/rfc/rfc2119) y [RFC 8174](https://www.rfc-editor.org/rfc/rfc8174) a una especificación arquitectónica interna del proyecto. Solo estos cinco términos, en mayúsculas y como etiquetas al comienzo de una disposición, expresan los niveles normativos definidos aquí:

| Etiqueta | Significado |
| --- | --- |
| **MUST:** | Obligación estricta. |
| **MUST NOT:** | Prohibición estricta. |
| **SHOULD:** | Recomendación fuerte que admite alternativas justificadas. |
| **SHOULD NOT:** | Práctica desaconsejada que admite excepciones justificadas. |
| **MAY:** | Opción permitida, no una obligación. |

### Reglas de interpretación

**MUST:** Las excepciones a requisitos estrictos están documentadas expresamente; esta disposición no autoriza a crear excepciones.

**MUST:** Ante contradicciones entre artículos, el agente informa el conflicto en lugar de resolverlo unilateralmente.

**MUST NOT:** Una recomendación se utiliza para justificar el incumplimiento de una obligación.

Los ejemplos técnicos, árboles, explicaciones y descripciones de la implementación actual no crean requisitos adicionales. Las condiciones y excepciones de las disposiciones conservan su alcance. Las tablas de dependencias expresan permisos limitados por las condiciones de sus artículos, no obligaciones de utilizar todos los destinos enumerados.

**SHOULD:** Las futuras revisiones conservan la numeración y los identificadores de los artículos siempre que sea posible.

## Mapa de consulta

Este archivo conserva la organización general y los límites modulares. Cada artículo tiene una única ubicación canónica; el índice de correspondencia enlaza las disposiciones especializadas sin resumirlas ni sustituirlas. La distribución documental no altera la gobernanza del proyecto.

| Tarea o responsabilidad | Documento | Artículos |
| --- | --- | --- |
| Organización general, encapsulación y APIs públicas | Este documento | 1.º, 2.º, 6.º–9.º, 46.º |
| UI, Hooks, Services, HTTP, DTO, Model, Mapper, formularios y auxiliares | [Roles y flujos](architecture/roles.md) | 10.º–22.º, 38.º–40.º |
| Estado, sesión, router, providers y contratos de navegación/layouts | [Estado y composición global](architecture/composition.md) | 23.º–27.º, 29.º–30.º |
| Components locales, estilos, responsive y recursos | [Organización visual y recursos](architecture/presentation.md) | 3.º–5.º, 31.º–37.º |
| Imports entre roles o módulos y revisión de límites | [Dependencias](architecture/dependencies.md) junto con el rol afectado | 41.º–45.º |
| Rutas y comportamiento concreto, ubicaciones y procedencias actuales | [Implementación actual](architecture/implementation.md) — informativo | 28.º y descripciones separadas de otros artículos |
| Aplicaciones genéricas de las reglas | [Anexos](architecture/examples.md) — no normativos | Anexos A y B |

Las [convenciones](conventions.md) conservan naming, formato y estilo de implementación; [tecnologías](technologies.md) conserva el inventario tecnológico. Las referencias coincidentes a imports, props e iconos en esos documentos no amplían las reglas arquitectónicas: sus fuentes son los artículos aquí indexados.

El [informe de auditoría de componentes](architecture/component-audit.md) registra el inventario completo, propietarios, recuperaciones y límites de las verificaciones de esta reorganización; es informativo, no una fuente normativa adicional.

Para una tarea localizada se consulta el contrato del rol y su matriz, además de la organización general aplicable. Los ejemplos son auxiliares; las descripciones de implementación no sustituyen las disposiciones. Consultar también las [observaciones de consistencia](#consistencia), que siguen siendo visibles aunque los artículos estén en archivos distintos.

## Organización general

<a id="articulo-1"></a>
### Artículo 1.º — Organización por feature

**MUST:** La unidad principal de organización funcional del frontend es la feature. La arquitectura mantiene alta cohesión, encapsulación y separación por responsabilidad.

<a id="articulo-2"></a>
### Artículo 2.º — Estructura conceptual y responsabilidades generales

La organización conceptual de `src/` comprende `app/`, `features/`, `infrastructure/`, `shared/`, `styles/` y `main.tsx`. El Component raíz se encuentra en `app/App/`, con su barrel local; `main.tsx` lo compone con los providers.

**MAY:** Dentro de una feature existen, según su responsabilidad, `assets/`, `components/`, `constants/`, `form/`, `hooks/`, `pages/`, `types/` y `utils/`, así como archivos de Service y Mapper en la raíz de la feature. `types/` admite `ui.ts` para los tipos de UI compartidos de la feature.

**MUST:** `assets/` contiene recursos estáticos específicos de la feature cuando no son globales ni compartidos. `form/` separa `hooks/` y `schemas/`; `hooks/` separa `mutations/`, `navigation/`, `pages/` y `queries/` según las responsabilidades existentes. Las APIs de las carpetas modulares se rigen por el [Artículo 8.º](#articulo-8). Las definiciones TypeScript de Models se agrupan en `types/models.ts`, sin carpeta física `models/`; `types/` separa `dtos.ts` de `models.ts`.

Los Models siguen siendo un rol arquitectónico.

**MUST:** Los tipos de props de un Component se declaran en el archivo del propio Component, junto a su implementación: son parte de su contrato local y no pasan a `types/` mientras ese Component sea su único consumidor. Un tipo de UI compartido —una variante, un estado o una forma de datos que consumen varios Components o varias features— pertenece al módulo que lo posee: en `shared/` cuando lo comparten varias features, y en la feature que lo define cuando solo lo usa esa. En ambos casos se consume por la API pública del módulo correspondiente, nunca por deep import.

**MUST:** Todo Component React, incluidas Pages, layouts y providers propios, utiliza `export default` con función nombrada. Si recibe propiedades, declara en su TSX una `interface Props` local y no exportada; conserva los tipos originales mediante extensión cuando corresponda. Se comparten tipos de datos, no interfaces de props. Los componentes sin propiedades no declaran una interfaz vacía. Estas convenciones alcanzan componentes existentes y futuros; no afectan exports de Hooks, utilidades, constantes ni tipos de datos.

Estas carpetas representan posibilidades, no una estructura obligatoria.

**MUST NOT:** Se crean carpetas, archivos, roles o abstracciones vacías para uniformar features.

**MUST:** La creación o división de Components y demás elementos se rige por las [reglas arquitectónicas comunes](../architecture.md) §1: responde a responsabilidades, cohesión, reutilización o necesidad arquitectónica, no a cantidad de líneas ni tamaño visual.

**MUST:** `app` se reserva para composición y configuración global: router, layouts y providers globales, store global, configuración global de TanStack Query y composición entre features. `main.tsx` es un bootstrap mínimo. Configuraciones globales como `queryClient` pertenecen conceptualmente a `app`, no a la raíz de `src` como contenedor arbitrario.

**MUST NOT:** `app` contiene lógica específica de una feature.

`features` contiene las unidades funcionales.

**MUST:** Cada feature encapsula sus Pages, Components, Hooks, Services, Models, Mappers, formularios y demás elementos específicos que realmente necesite.

**MUST:** `shared` contiene únicamente elementos genuinamente reutilizables entre features.

**MUST NOT:** Se mueven elementos allí preventivamente por una posible reutilización futura. `shared` recibe conceptos específicos de una feature para permitir que otra dependa de ellos o se utiliza para evadir restricciones entre features.

`infrastructure` contiene el cliente y los helpers HTTP transversales (`http/`), así como el adaptador del navegador, la persistencia y el estado de sesión (`storage/`).

**MUST NOT:** Components, Pages, Hooks y Services acceden directamente a mecanismos concretos como `localStorage` o `sessionStorage`.

**MUST:** El acceso queda encapsulado por el adaptador de almacenamiento. Los consumidores respetan la matriz del [Artículo 41.º](architecture/dependencies.md#articulo-41).

**MUST:** Los estilos específicos de una Page o Component permanecen colocalizados con ellos mediante CSS Modules o SCSS Modules cuando corresponda. `src/styles` queda reservado para estilos globales, como base, resets, abstracts, variables y mixins.

<a id="articulo-6"></a>
### Artículo 6.º — API pública de features

**MUST:** Cada feature expone explícitamente su API pública mediante `index.ts`, aunque actualmente no tenga consumidores externos. Un consumidor externo importa exclusivamente desde esa API y no desde archivos internos. La API pública exporta únicamente lo que corresponda exponer; no obliga a publicar elementos internos.

<a id="articulo-7"></a>
### Artículo 7.º — Aislamiento entre features

**MUST NOT:** Una feature depende directa o indirectamente de otra feature, incluso mediante la API pública de esta última.

**MUST:** La composición de múltiples features se realiza desde `app` utilizando sus APIs públicas.

**MAY:** `app` compone múltiples features.

**MUST NOT:** `app` absorbe su lógica interna.

<a id="articulo-8"></a>
### Artículo 8.º — APIs de carpetas modulares

**MUST:** Cada Page se organiza como módulo autocontenido bajo `pages/<Page>/`, con su implementación, estilos exclusivos y `index.ts`. `pages/index.ts` agrega únicamente las APIs de las Pages que corresponde exponer. Los Components independientes siguen el mismo criterio bajo `components/<Component>/`, colocalizando sus archivos propios. Para otras unidades independientes con archivos asociados se aplica la misma organización cuando corresponda, sin crear carpetas por mera uniformidad ni separar archivos que forman una misma responsabilidad. Los recursos compartidos entre varias unidades permanecen en una ubicación común apropiada dentro de su feature, sin duplicarlos ni atribuirlos artificialmente a una de ellas.

**MUST:** Cada carpeta modular con código del frontend tiene un `index.ts` que define su API para los demás módulos autorizados. Cualquier consumidor situado fuera de esa carpeta importa sus elementos exclusivamente mediante ese `index.ts`, sin deep imports a archivos internos.

**MUST:** El barrel local de cada Component contiene `export { default } from "./NombreDelComponente";`. Los barrels de agrupación pueden conservar APIs nombradas mediante `export { default as NombreDelComponente } from "./NombreDelComponente";`. Los tipos públicos se exponen desde sus módulos y la agrupación correspondiente, no desde el barrel local del Component.

**MAY:** Dentro de la misma carpeta se utilizan imports directos entre archivos, especialmente para evitar auto-imports y ciclos a través del propio barrel.

Esta regla concierne a módulos de código: recursos como hojas de estilo colocalizadas se importan por su ruta. Los directorios contenedores de módulos sin API propia no requieren un barrel vacío.

**MUST:** El `index.ts` de una carpeta interna de una feature expone solo lo necesario a otros módulos permitidos de esa feature. El `index.ts` raíz de la feature reexporta desde esas APIs internas únicamente aquello que necesitan consumidores externos autorizados.

La API interna no convierte sus elementos en parte de la API pública externa.

**MUST NOT:** Un barrel se utiliza para eludir la matriz de dependencias o introducir ciclos.

**MUST NOT:** Una forma de import se utiliza para evadir las dependencias permitidas.

La elección entre aliases e imports relativos se rige por [convenciones](conventions.md).

<a id="articulo-9"></a>
### Artículo 9.º — APIs de shared

**MUST:** Los submódulos de `shared` exponen APIs públicas cuando corresponda, de modo que sus consumidores no conozcan arbitrariamente su estructura interna.

<a id="articulo-46"></a>
### Artículo 46.º — Decisiones arquitectónicas no definidas

**MUST:** Cuando la documentación vigente no defina una decisión arquitectónica necesaria, se aplica la gobernanza de la constitución: consultar al usuario antes de introducir una regla, capa, patrón, abstracción o ubicación nueva.

## Identificadores y compatibilidad

Se conservan los 46 números y títulos originales; los artículos no se renumeran por archivo ni por dominio. El identificador explícito `articulo-N` acompaña al número global y permite enlaces independientes del título. El Artículo 28.º conserva su carácter descriptivo, ahora en implementación actual. Los encabezados de capítulos agrupan responsabilidades de cada módulo sin introducir precedencias normativas.

La correspondencia siguiente identifica la única ubicación de cada artículo. Las anclas anteriores de los artículos trasladados se conservan aquí como puntos de navegación, no como copias de sus disposiciones; las antiguas Partes y anexos también conservan destinos de consulta.

| Artículo original | Ubicación canónica (mismo número y título) |
| --- | --- |
| 1.º | [Organización por feature](#articulo-1) |
| 2.º | [Estructura conceptual y responsabilidades generales](#articulo-2) |
| <a id="artículo-3º--ubicación-de-components-de-página"></a><a id="articulo-3"></a>3.º | [Ubicación de Components de página](architecture/presentation.md#articulo-3) |
| <a id="artículo-4º--delegación-visual-y-components-locales"></a><a id="articulo-4"></a>4.º | [Delegación visual y Components locales](architecture/presentation.md#articulo-4) |
| <a id="artículo-5º--imports-del-design-system"></a><a id="articulo-5"></a>5.º | [Imports del Design System](architecture/presentation.md#articulo-5) |
| 6.º | [API pública de features](#articulo-6) |
| 7.º | [Aislamiento entre features](#articulo-7) |
| 8.º | [APIs de carpetas modulares](#articulo-8) |
| 9.º | [APIs de shared](#articulo-9) |
| <a id="artículo-10º--responsabilidades-de-page"></a><a id="articulo-10"></a>10.º | [Responsabilidades de Page](architecture/roles.md#articulo-10) |
| <a id="artículo-11º--responsabilidades-de-component"></a><a id="articulo-11"></a>11.º | [Responsabilidades de Component](architecture/roles.md#articulo-11) |
| <a id="artículo-12º--organización-y-frontera-de-hooks"></a><a id="articulo-12"></a>12.º | [Organización y frontera de Hooks](architecture/roles.md#articulo-12) |
| <a id="artículo-13º--especialización-de-hooks"></a><a id="articulo-13"></a>13.º | [Especialización de Hooks](architecture/roles.md#articulo-13) |
| <a id="artículo-14º--responsabilidades-de-service"></a><a id="articulo-14"></a>14.º | [Responsabilidades de Service](architecture/roles.md#articulo-14) |
| <a id="artículo-15º--http-transversal-y-errores"></a><a id="articulo-15"></a>15.º | [HTTP transversal y errores](architecture/roles.md#articulo-15) |
| <a id="artículo-16º--credenciales-de-solicitudes-autenticadas"></a><a id="articulo-16"></a>16.º | [Credenciales de solicitudes autenticadas](architecture/roles.md#articulo-16) |
| <a id="artículo-17º--dto-y-contratos-tipados"></a><a id="articulo-17"></a>17.º | [DTO y contratos tipados](architecture/roles.md#articulo-17) |
| <a id="artículo-18º--separación-y-responsabilidades-de-model"></a><a id="articulo-18"></a>18.º | [Separación y responsabilidades de Model](architecture/roles.md#articulo-18) |
| <a id="artículo-19º--transformaciones-mediante-mapper"></a><a id="articulo-19"></a>19.º | [Transformaciones mediante Mapper](architecture/roles.md#articulo-19) |
| <a id="artículo-20º--form-model-y-estructura-de-formularios"></a><a id="articulo-20"></a>20.º | [Form Model y estructura de formularios](architecture/roles.md#articulo-20) |
| <a id="artículo-21º--configuración-mediante-form-hook"></a><a id="articulo-21"></a>21.º | [Configuración mediante Form Hook](architecture/roles.md#articulo-21) |
| <a id="artículo-22º--schemas-y-autoridad-de-validación"></a><a id="articulo-22"></a>22.º | [Schemas y autoridad de validación](architecture/roles.md#articulo-22) |
| <a id="artículo-23º--estado-remoto-y-limpieza-de-caché"></a><a id="articulo-23"></a>23.º | [Estado remoto y limpieza de caché](architecture/composition.md#articulo-23) |
| <a id="artículo-24º--alcance-de-los-stores-y-estado-local"></a><a id="articulo-24"></a>24.º | [Alcance de los stores y estado local](architecture/composition.md#articulo-24) |
| <a id="artículo-25º--autenticación-y-almacenamiento"></a><a id="articulo-25"></a>25.º | [Autenticación y almacenamiento](architecture/composition.md#articulo-25) |
| <a id="artículo-26º--router-global-y-navegación-de-features"></a><a id="articulo-26"></a>26.º | [Router global y navegación de features](architecture/composition.md#articulo-26) |
| <a id="artículo-27º--layouts-y-providers-globales"></a><a id="articulo-27"></a>27.º | [Layouts y providers globales](architecture/composition.md#articulo-27) |
| <a id="artículo-28º--rutas-y-comportamiento-actual-del-guard"></a><a id="articulo-28"></a>28.º | [Rutas y comportamiento actual del guard](architecture/implementation.md#articulo-28) |
| <a id="artículo-29º--composición-de-navegación-global"></a><a id="articulo-29"></a>29.º | [Composición de navegación global](architecture/composition.md#articulo-29) |
| <a id="artículo-30º--catálogo-de-layouts-y-components-compartidos"></a><a id="articulo-30"></a>30.º | [Catálogo de layouts y Components compartidos](architecture/composition.md#articulo-30) |
| <a id="artículo-31º--cadena-de-altura"></a><a id="articulo-31"></a>31.º | [Cadena de altura](architecture/presentation.md#articulo-31) |
| <a id="artículo-32º--fondos-y-responsabilidad-visual"></a><a id="articulo-32"></a>32.º | [Fondos y responsabilidad visual](architecture/presentation.md#articulo-32) |
| <a id="artículo-33º--scroll-y-responsive"></a><a id="articulo-33"></a>33.º | [Scroll y responsive](architecture/presentation.md#articulo-33) |
| <a id="artículo-34º--prevención-de-regresiones-visuales"></a><a id="articulo-34"></a>34.º | [Prevención de regresiones visuales](architecture/presentation.md#articulo-34) |
| <a id="artículo-35º--api-única-de-iconos"></a><a id="articulo-35"></a>35.º | [API única de iconos](architecture/presentation.md#articulo-35) |
| <a id="artículo-36º--capas-globales-de-estilos"></a><a id="articulo-36"></a>36.º | [Capas globales de estilos](architecture/presentation.md#articulo-36) |
| <a id="artículo-37º--procedencia-y-tolerancia-a-fallos-de-recursos"></a><a id="articulo-37"></a>37.º | [Procedencia y tolerancia a fallos de recursos](architecture/presentation.md#articulo-37) |
| <a id="artículo-38º--funciones-auxiliares-puras"></a><a id="articulo-38"></a>38.º | [Funciones auxiliares puras](architecture/roles.md#articulo-38) |
| <a id="artículo-39º--fuentes-de-datos-simulados"></a><a id="articulo-39"></a>39.º | [Fuentes de datos simulados](architecture/roles.md#articulo-39) |
| <a id="artículo-40º--constants-y-query-keys"></a><a id="articulo-40"></a>40.º | [Constants y query keys](architecture/roles.md#articulo-40) |
| <a id="artículo-41º--dependencias-dentro-de-una-feature"></a><a id="articulo-41"></a>41.º | [Dependencias dentro de una feature](architecture/dependencies.md#articulo-41) |
| <a id="artículo-42º--composición-de-hooks-y-uso-de-shared"></a><a id="articulo-42"></a>42.º | [Composición de Hooks y uso de shared](architecture/dependencies.md#articulo-42) |
| <a id="artículo-43º--dependencias-globales"></a><a id="articulo-43"></a>43.º | [Dependencias globales](architecture/dependencies.md#articulo-43) |
| <a id="artículo-44º--prohibiciones-expresas"></a><a id="articulo-44"></a>44.º | [Prohibiciones expresas](architecture/dependencies.md#articulo-44) |
| <a id="artículo-45º--alcance-de-las-matrices-y-roles-conceptuales"></a><a id="articulo-45"></a>45.º | [Alcance de las matrices y roles conceptuales](architecture/dependencies.md#articulo-45) |
| 46.º | [Decisiones arquitectónicas no definidas](#articulo-46) |

### Correspondencia de secciones anteriores

Las referencias históricas se conservan en specs, tareas completadas, informes y conversaciones: describen el documento de su momento y no se reescriben como si hubieran consultado archivos nuevos. Esta tabla permite resolverlas; las referencias actuales pueden utilizar los enlaces canónicos del índice de artículos.

| Referencia anterior | Ubicación actual |
| --- | --- |
| <a id="parte-primera"></a>Primera Parte; §1.1 | [Organización general](#organización-general), [Components locales](architecture/presentation.md#articulo-3) y [APIs modulares](#articulo-8) |
| §1.2 | [APIs públicas](#articulo-6), [aislamiento](#articulo-7) y [APIs modulares](#articulo-8) |
| <a id="parte-segunda"></a>Segunda Parte; §1.3–§1.9 | [Roles y flujos](architecture/roles.md) |
| <a id="parte-tercera"></a>Tercera Parte; §1.10–§1.11 | [Estado y autenticación](architecture/composition.md#articulo-23), [implementación](architecture/implementation.md#sesion-y-autenticacion) |
| §1.12 | [Router y composición global](architecture/composition.md#articulo-26), [rutas actuales](architecture/implementation.md#articulo-28) y [navegación](architecture/composition.md#articulo-29) |
| <a id="parte-cuarta"></a>Cuarta Parte; §1.12.1 | [Organización visual y recursos](architecture/presentation.md) |
| §1.13 | [Utilidades y constantes](architecture/roles.md#articulo-38) |
| <a id="parte-quinta"></a>Quinta Parte; §1.14 | [Dependencias](architecture/dependencies.md) y [vacíos arquitectónicos](#articulo-46) |
| <a id="anexos"></a>Anexos | [Ejemplos técnicos no normativos](architecture/examples.md) |
| <a id="anexo-a"></a>Anexo A | [Organización de directorios](architecture/examples.md#anexo-a) |
| <a id="anexo-b"></a>Anexo B | [Importaciones y APIs públicas](architecture/examples.md#anexo-b) |

<a id="consistencia"></a>
## Observaciones de consistencia

Estas observaciones son informativas: no crean permisos, excepciones ni precedencias entre artículos. Conservan visibles los puntos detectados antes de la modularización.

| Artículos o descripción | Resultado del análisis |
| --- | --- |
| [10.º](architecture/roles.md#articulo-10) y [41.º](architecture/dependencies.md#articulo-41) | **Pendiente de decisión humana.** La Page sin Page Hook utiliza los Hooks correspondientes según el texto, pero su fila solo enumera Page Hook o Navigation Hook. No se amplía ni restringe esa fila. |
| [4.º](architecture/presentation.md#articulo-4) y [11.º](architecture/roles.md#articulo-11) | El primero trata específicamente Components locales de Page y prohíbe conectar Hooks; el segundo permite Hooks al Component general. Se conservan ambos alcances sin extender el permiso general a los locales. Si se pretende permitir Hooks en estos últimos, requiere decisión humana. |
| [31.º](architecture/presentation.md#articulo-31) y [33.º](architecture/presentation.md#articulo-33) | **Pendiente de precisar el alcance responsive.** El texto global de altura/overflow convive con el scroll natural móvil. El código de `AppLayout` cambia a altura automática y overflow visible bajo `$bp-mobile`, mientras `PageLayout` cambia bajo `$bp-tablet`; no se inventa un breakpoint común ni se modifica ninguna condición. |
| [32.º](architecture/presentation.md#articulo-32) y [34.º](architecture/presentation.md#articulo-34) | **Pendiente de precisar la excepción.** La prohibición de fondos internos y de `background-color` convive con el fondo propio del footer compacto. No se redefine el elemento al que corresponde ese fondo. |
| [Ubicación de layouts](architecture/implementation.md#ubicacion-de-layouts) | **Descripción actualizada.** `AuthGuard` está en `app/layouts/AuthGuard`. Coexisten dos `AuthLayout`: el global recuperado en `app/layouts/AuthLayout`, que contiene un `Outlet` y no está conectado al router actual, y el específico de auth en `features/auth/components/AuthLayout`, utilizado por login y registro con título, descripción, contenido y panel visual. Son implementaciones distintas; no se eliminan ni se intercambian para ajustar la documentación. |
| [Colores literales](architecture/implementation.md#estilos-de-la-implementacion) | La referencia previa a una sección de inconsistencias de SPEC-005 tiene una deuda de trazabilidad ya registrada en [TD-018](../TECHNICAL_DEBT.md#td-018--colores-literales-en-los-módulos-de-estilo-de-componentes). Se conserva como antecedente, sin declararla resuelta. |
