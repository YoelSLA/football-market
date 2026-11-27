# Investigación: auditoría de peticiones a servicios web

## Decisión: interceptar el ciclo de vida de Spring MVC

- **Decisión**: implementar la observación transversal mediante un interceptor MVC, con inicio de medición antes del procesamiento del handler y emisión al finalizar, incluido el caso con excepción.
- **Motivo**: el interceptor opera en la frontera de los Controllers, conoce el método/ruta resuelta y se ejecuta alrededor del procesamiento de los endpoints REST. Se integra sin envolver bodies ni modificar Controllers individuales.
- **Alternativas consideradas**:
  - Un filtro Servlet: cubre más etapas HTTP, pero no obtiene directamente el método MVC ni el patrón de ruta resuelto y tiende a inspeccionar parámetros de forma más amplia.
  - Logging por Controller: duplicaría comportamiento en cada endpoint y sería fácil dejar endpoints fuera.
  - Logging de Service: no representa uniformemente método/ruta HTTP y distribuye la responsabilidad por casos de uso.

## Decisión: leer identidad del SecurityContext vigente

- **Decisión**: resolver el usuario a partir de la autenticación establecida por Spring Security; emitir `anonymous` si falta o representa autenticación anónima.
- **Motivo**: el proyecto autentica Bearer JWT mediante Spring Security y convierte el subject validado en el nombre de la autenticación. Reutilizar el contexto evita parsear tokens y evita una segunda validación.
- **Alternativas consideradas**:
  - Parsear JWT desde el header: duplicaría autenticación y aumentaría el riesgo de exposición de credenciales.
  - Recibir identidad desde Controllers: exigiría modificar todos los endpoints y sería propenso a omisiones.

## Decisión: restringir campos del registro

- **Decisión**: incluir solo método, patrón de ruta, identidad, timestamp, duración y parámetros de consulta reconocidos por el endpoint que no sean identificadores; no incluir body ni headers. Los identificadores recibidos como parámetros de consulta son el único dato sensible de esos parámetros y se excluyen. Los demás parámetros reconocidos pueden registrarse. Credenciales y contraseñas se excluyen siempre.
- **Motivo**: cumple el contrato de la issue #99 y la clarificación de la SPEC sin capturar payloads que pueden contener contraseñas, tokens u otros datos personales.
- **Alternativas consideradas**:
  - Registrar todos los parámetros de la petición: descartado porque incluiría datos ajenos al contrato de consulta y podría filtrar información.
  - Registrar bodies o headers: descartado porque la issue solo pide parámetros relevantes y prohíbe exponer credenciales.
- **Alcance de filtrado**: se limita a excluir identificadores recibidos como parámetros de consulta, credenciales y contraseñas, conforme a FR-002/FR-005; no se incorpora un catálogo genérico de datos sensibles ni persistencia de auditoría.

## Decisión: reutilizar SLF4J/Logback actuales

- **Decisión**: emitir el evento mediante el logger existente, sin nueva dependencia ni configuración de destino.
- **Motivo**: la configuración efectiva ya usa SLF4J/Logback gestionados por Spring Boot y define logging de consola/archivo.
- **Alternativas consideradas**:
  - Incorporar una librería de auditoría o un almacén dedicado: descartado expresamente por la SPEC y la issue.

## Evidencia local consultada

- `backend/src/main/java/footballmarket/security/SecurityConfig.java`: cadena stateless, JWT Bearer, endpoints públicos y protegidos.
- `backend/src/main/java/footballmarket/security/PersistedUserJwtAuthenticationConverter.java`: valida identidad y asigna el subject autenticado.
- `backend/src/main/resources/application.yml`: nivel de logging root y configuración actual.
- `backend/src/main/java/footballmarket/features/catalog/presentation/PlayerController.java`: endpoints con `@RequestParam` y `@ModelAttribute`.
- `backend/src/main/java/footballmarket/features/auth/presentation/AuthenticationController.java`: login/registro reciben credenciales en body, que no se incluirá en la auditoría.
- Issue #99: RF-01–RF-05 y CA-01–CA-05; parámetro pedido como relevante de consulta, sin lista cerrada adicional.

## Estado de clarificaciones

La SPEC limita los parámetros a los reconocidos por cada endpoint, excluye los identificadores de consulta, credenciales y contraseñas, y permite registrar los demás parámetros reconocidos. No queda una decisión funcional necesaria para elegir el punto de integración. El plan no añade una política de retención, un esquema de almacenamiento ni un SLA ausentes de la especificación.
