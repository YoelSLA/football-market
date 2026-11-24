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

### Autorización de monitoreo

- **Decisión**: Reutilizar el JWT Resource Server y el validador de identidad persistida existente.
  Prometheus requiere autenticación. Solo las rutas exactas de health e info reciben acceso
  anónimo; los subendpoints y cualquier otro endpoint de Actuator no se permiten.
- **Rationale**: Cumple las clarificaciones de SPEC-032 y evita inventar roles o mecanismos de
  autenticación. El comportamiento general `anyRequest().authenticated()` ya protege rutas no
  permitidas explícitamente.
- **Alternativas consideradas**: Permitir todas las rutas `/actuator/**` (abre subendpoints y
  contradice FR-006); exponer Prometheus sin autenticación (contradice FR-001/003); agregar un rol
  nuevo (la spec ordena reutilizar la seguridad existente y no define roles de dominio nuevos).

### Salud e información no sensible

- **Decisión**: Conservar `management.endpoint.health.show-details: never`; mantener info pública
  sin configurar propiedades `management.info.*` con datos sensibles.
- **Rationale**: La configuración ya satisface la respuesta mínima de health y actualmente no
  aporta valores de info. Se reutiliza esta protección en lugar de crear filtros o DTO propios.
- **Alternativas consideradas**: Proteger ambos endpoints (contradice la clarificación); publicar
  detalles de componentes o valores de configuración (contradice FR-005/007/009).

## Resultado de investigación de contexto

- `backend/build.gradle.kts` declara Spring Boot Actuator; en el bloque de dependencias revisado
  no aparece un registry Prometheus.
- `application.yml` expone actualmente una lista configurable de Actuator con default
  `health,info`; debe sustituirse por el límite fijo exigido por la spec.
- `SecurityConfig` permite anónimamente `/actuator`, health/info y metrics, incluidos varios
  subpaths. El diseño debe retirar el acceso anónimo amplio y proteger explícitamente Prometheus.
- Health ya usa `show-details: never`.
- No hay datos persistidos ni entidades de dominio necesarias para esta feature.
- Fuentes funcionales: `spec.md` de SPEC-032, en particular sus clarificaciones, FR-001–FR-009 y
  SC-001–SC-004. Fuentes técnicas: `application.yml`, `SecurityConfig.java`,
  `backend/build.gradle.kts` y `docs/backend/technologies.md`.
