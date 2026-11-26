# Plan de implementación: Auditoría de peticiones a servicios web

**Rama**: `feat/036-web-service-audit-logging` | **Fecha**: 2026-11-26 | **Spec**: [spec.md](spec.md)

<!--
Documentación viva: describe exclusivamente el diseño técnico vigente. Forma parte de la tríada
permanente junto a spec.md y tasks.md. No genera Tasks ni autoriza IMPLEMENT.
-->

## Resumen

Registrar en el logging SLF4J/Logback las peticiones entrantes a endpoints REST publicados por Spring, incluidas las que terminan con error. Se reutilizarán Spring MVC y Spring Security existentes para obtener la ruta resuelta, parámetros de consulta reconocidos distintos de identificadores, identidad autenticada o `anonymous`, timestamp y duración en milisegundos. El registro no persistirá en base de datos ni incluirá cuerpos, cabeceras, identificadores de consulta, credenciales ni contraseñas.

## Contexto técnico

**Lenguaje/versión**: Java 21.
**Dependencias principales**: Spring Boot 4.1.1, Spring Web MVC, Spring Security, SLF4J/Logback gestionados por Spring Boot.
**Persistencia**: N/A; solo logging existente.
**Verificación**: tests de backend definidos por las normas del área, ejecutados por el usuario; el agente no ejecuta tests.
**Plataforma**: servicio backend Spring Boot.
**Tipo de proyecto**: API web.
**Objetivos de rendimiento**: medir y registrar duración en milisegundos; no se define un SLA para el mecanismo de auditoría.
**Restricciones**: no registrar credenciales, contraseñas ni datos sensibles; no añadir dependencias, persistencia, UI ni auditoría de llamadas a terceros.
**Alcance**: endpoints REST entrantes publicados por la aplicación; registro incluso cuando el procesamiento del endpoint termina con error.

## Constitution Check (previo a investigación)

- **Simplicidad y proporcionalidad**: PASS. Reutilizar MVC, Security y el logging actual; sin infraestructura, dependencia o persistencia nueva.
- **Seguridad e integridad**: PASS. Excluir cuerpos, cabeceras, identificadores de consulta, credenciales y contraseñas; registrar únicamente parámetros reconocidos distintos de identificadores.
- **Coherencia de contratos**: PASS. La salida de auditoría es un contrato operativo documentado en esta SPEC; no altera contratos HTTP.
- **Trazabilidad y documentación viva**: PASS. Mantener los artefactos vigentes de la SPEC y el historial en Git.
- **Verificación y autorización**: PASS. Diseñar pruebas backend para ejecución humana; PLAN no habilita implementación ni ejecuta pruebas.

## Investigación y decisiones

Las decisiones y alternativas están desarrolladas en [research.md](research.md). Se selecciona un interceptor de Spring MVC como punto común para las invocaciones de Controllers, porque dispone del método manejado y del patrón de ruta resuelto, y su callback de finalización permite registrar peticiones completadas con excepción. La autenticación se lee del `SecurityContext` establecido por la cadena actual de Spring Security. No se crea un Service de aplicación: la auditoría es transversal de presentación y no un caso de uso de dominio.

## Diseño y contratos

- El interceptor inicia el cronometraje al recibir la petición MVC y emite un solo registro al finalizarla, también cuando hay excepción.
- El evento contiene timestamp, identidad, método HTTP, patrón de ruta/operación, parámetros de consulta reconocidos por el endpoint excepto identificadores y duración en milisegundos.
- Los parámetros se obtienen del contrato del handler; no se inspeccionan body ni headers. Los identificadores recibidos como parámetros de consulta se omiten antes de construir el mensaje; los demás parámetros reconocidos pueden registrarse. Credenciales y contraseñas no se registran.
- Usuario autenticado: nombre del `Authentication` válido del contexto de seguridad. Contexto ausente o anónimo: `anonymous`.
- Se usa el logger SLF4J existente y el nivel/formato compatibles con la configuración actual de Logback; no se añade una tabla ni un endpoint de consulta.
- [data-model.md](data-model.md) define la representación efímera del evento; [contracts/audit-log.md](contracts/audit-log.md) define el contrato observable del registro; [quickstart.md](quickstart.md) describe comprobaciones manuales.

## Comprobación constitucional posterior al diseño

- **Simplicidad y mínimo alcance**: PASS. Un interceptor y el registro existente satisfacen el alcance sin una capa adicional.
- **Seguridad**: PASS condicionado a que los identificadores de consulta se filtren antes de formatear/loguear y que no se registren body, headers, Authorization, JWT ni contraseñas.
- **Arquitectura backend**: PASS. Integración en la frontera HTTP común; no se introducen dependencias entre roles de negocio ni acceso a persistencia.
- **Contratos sincronizados**: PASS. La salida de log queda definida en los artefactos de la SPEC; no hay API HTTP nueva o modificada.
- **Verificación**: PASS condicionado a añadir pruebas backend apropiadas en la fase de implementación; quedan a cargo del usuario.

No se identifican violaciones constitucionales que requieran Complexity Tracking.

## Estructura del proyecto

```text
specs/036-web-service-audit-logging/
├── spec.md
├── plan.md
├── tasks.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── audit-log.md

backend/src/main/java/footballmarket/
├── common/
│   └── presentation/
│       ├── RequestAuditInterceptor.java
│       └── HttpMvcConfiguration.java
└── security/
    └── ...                   # SecurityContext existente, consumido sin modificar JWT

backend/src/test/java/footballmarket/
└── common/
    └── presentation/
        └── RequestAuditInterceptorTest.java
```

**Decisión de estructura**: la auditoría es transversal a endpoints REST, por lo que su integración pertenece a `common/presentation` en el límite HTTP, no a una feature de negocio ni a `security`. Las rutas de las clases nuevas coinciden con las referenciadas por T001–T003; la identidad se consume desde el `SecurityContext` existente sin modificar JWT.

## Límites

Este plan documenta el diseño técnico y no constituye autorización para implementar. Cualquier implementación requiere una autorización humana explícita y vigente para el alcance, además de cumplir las condiciones de avance del workflow SDD.
