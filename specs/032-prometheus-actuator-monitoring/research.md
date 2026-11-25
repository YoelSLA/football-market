# Investigación: métricas Prometheus y monitoreo seguro

## Decisiones

### Publicación de métricas mediante Actuator y Micrometer

- **Decisión**: Reutilizar `spring-boot-starter-actuator` existente y añadir el registry
  `micrometer-registry-prometheus` solo si la configuración efectiva no lo incluye. Mantener las
  versiones bajo la gestión de dependencias de Spring Boot 4.1.1.
- **Rationale**: Actuator por sí solo ya está declarado, pero la configuración inspeccionada no
  incluye un registry Prometheus; ese registry proporciona el endpoint `/actuator/prometheus`.
  Spring Boot/Micrometer aporta métricas de JVM y HTTP sin una capa de métricas propia.
- **Alternativas consideradas**: Desarrollar un endpoint/exportador propio (duplicaría capacidades
  existentes y ampliaría la superficie); desplegar un servidor Prometheus (fuera de alcance y no
  necesario para exponer el scrape endpoint).

### Exposición HTTP explícita

- **Decisión**: Reemplazar la exposición actual configurable por una lista explícita que contenga
  exclusivamente `prometheus,health,info`; no usar `*` ni una variable de entorno que permita
  habilitar otros endpoints.
- **Rationale**: FR-006 exige que esos sean los únicos endpoints HTTP expuestos. La lista
  configurable existente permite exponer endpoints adicionales y no cumple el contrato cerrado.
- **Alternativas consideradas**: Conservar la variable de lista abierta (no garantiza el límite
  normativo); exponer `metrics` en lugar de `prometheus` (no satisface la ruta y formato pedidos).

### Acceso a los endpoints de monitoreo

- **Decisión**: Permitir anónimamente las rutas exactas `/actuator/prometheus`,
  `/actuator/health` y `/actuator/info`. Mantener la autenticación y autorización actuales para la
  API de dominio y no abrir `/actuator/**`. No exigir JWT, roles ni permisos para Prometheus.
- **Rationale**: La clarificación vigente de SPEC-032 establece explícitamente que cualquier
  cliente que alcance el backend puede consultar las métricas. La seguridad se preserva limitando
  estrictamente la superficie expuesta y evitando publicar datos sensibles.
- **Alternativas consideradas**: Exigir JWT a Prometheus (contradice FR-003/004); permitir todas
  las rutas `/actuator/**` (contradice FR-006); modificar la seguridad de endpoints de dominio (no
  es necesario para cumplir la feature).

### Salud e información pública

- **Decisión**: Conservar health mínimo con `show-details: never` y `show-components: never`.
  Configurar info exclusivamente con el nombre `Football Market` y la descripción
  `API de mercado de jugadores de fútbol`.
- **Rationale**: La spec exige estos valores estáticos y prohíbe otros valores de entorno o
  configuración. Se reutiliza Actuator sin crear Controllers ni DTOs propios.
- **Alternativas consideradas**: Proteger health/info (contradice la clarificación); publicar
  detalles de componentes, build o configuración (contradice FR-005/007/009).

### Métricas incluidas

- **Decisión**: Utilizar métricas estándar de Spring Boot/Micrometer para CPU del sistema y proceso,
  memoria, hilos y pausas de GC de JVM, además del recuento y duración de solicitudes HTTP. El
  estado general se consulta por `/actuator/health`; no se añade instrumentación de negocio ni
  percentiles de latencia.
- **Rationale**: Corresponde a las métricas expresamente acordadas en la aclaración vigente y evita
  instrumentación propia.
- **Alternativas consideradas**: Crear métricas de negocio o percentiles personalizados
  (explícitamente fuera del alcance); representar health como métrica duplicada (la spec asigna el
  estado a `/actuator/health`).

## Resultado de investigación de contexto

- `backend/build.gradle.kts` declara Spring Boot Actuator; en el bloque de dependencias revisado
  no aparece un registry Prometheus.
- `application.yml` expone actualmente una lista configurable de Actuator con default
  `health,info`; debe sustituirse por el límite fijo exigido por la spec.
- `SecurityConfig` permite anónimamente `/actuator`, health/info y metrics, incluidos varios
  subpaths. El diseño debe retirar el acceso anónimo amplio, permitir las tres rutas exactas
  requeridas y mantener cerrados los demás endpoints y subpaths.
- Health ya usa `show-details: never`.
- No hay datos persistidos ni entidades de dominio necesarias para esta feature.
- Fuentes funcionales: `spec.md` de SPEC-032, en particular sus clarificaciones, FR-001–FR-009 y
  SC-001–SC-004. Fuentes técnicas: `application.yml`, `SecurityConfig.java`,
  `backend/build.gradle.kts` y `docs/backend/technologies.md`.
