# Entregas del Trabajo Práctico

## Sprint 1

**Fecha de entrega:** 29/09/2026

**Estado:** finalizado, según confirmación del usuario.

### Core

- [x] Creación de repositorios GitHub
- [x] Configuración en GitHub Actions
- [x] Build corriendo y SUCCESS
- [x] SonarCloud: registrar el proyecto y mantener issues menor a 10
- [x] JWT implementado
- [x] Configuración de Swagger en el back-API (v3)

Referencia: https://swagger.io/

### Modelo

- [x] Modelo mínimo y estructura de datos
- [x] Testing automático unitario según las pautas de la materia

### Funcionalidad

- [x] Creación de usuario y ApiKEY para acceder al resto de los endpoints
- [x] Endpoint de catálogo de jugadores

---

## Sprint 2

**Entrega 2:** mercado de jugadores de fútbol — Beyond thunderdome.

**Fecha de presentación y entrega:** martes 03/11/2026.

**Fuente de la actualización:** comunicado del profesor compartido por el usuario. La fecha original de la planificación era 03/10/2026 y queda reemplazada por la indicada en el comunicado.

### Core

- [ ] Separar profiles de testing para unitarios y e2e

El comunicado identifica este requisito como el único punto Core pendiente de la entrega 2.

### Actualización de los puntos Core originales

Los siguientes puntos dejan de exigirse como pendientes de la entrega 2, según el profesor. Se conservan como antecedente de la planificación; esto no acredita su implementación en este repositorio ni elimina las exigencias de otras entregas.

| Punto original | Aclaración del profesor |
| --- | --- |
| Utilizar HSQLDB para persistir datos (opción H2) | Ya todos usan directamente una base de datos relacional. |
| Crear datos de prueba cuando levanta la aplicación | Estaba relacionado con H2. |
| Documentación de endpoints/APIs con Swagger v3 | Ya se pidió para la entrega 1. |
| Implementar JOB de Coverage | Es parte de Sonar, que ya se pidió para la entrega 1. |

**Aclaración pendiente:** la planificación original también incluía «Estado del build en \"Verde\"». El comunicado afirma que queda un solo punto Core pendiente, pero no menciona expresamente el estado del build entre los puntos dispensados. Se conserva esta referencia sin resolver su vigencia por cuenta propia.

### Modelo

No hay requisitos indicados en esta sección.

### Funcionalidad

- [ ] Cálculo de cotización de jugadores según estrategia
- [ ] Compra/venta de tokens
- [ ] Cotización de jugador a una fecha dada
- [ ] Historial de operaciones
- [ ] Ranking de jugadores según estrategia activa

### Nuevos requisitos de la entrega 2

- [ ] Agregar visualización de datos de usuario con gráficos en el frontend.
- [ ] Pensar una feature adicional para el TP.
- [ ] Planificar la feature adicional.
- [ ] Implementar la feature adicional.
- [ ] Presentar la feature adicional como parte de la entrega.

El objetivo de la feature adicional es ejercitar la creatividad, la capacidad de invención y el desarrollo de producto.

#### Ejemplos propuestos por el profesor (no obligatorios)

- Proceso de análisis de mercado y ajuste de estrategia en tiempo real.
- Webhook al frontend para mostrar una notificación al usuario y permitir una compra en tiempo real.
- Órdenes de compra asincrónicas por medio de un bus de mensajes.

La feature puede agregarse a cualquier parte del TP. El profesor menciona como posibilidades:

- El proceso de scraping y extracción de datos.
- El proceso de órdenes de compra.
- La forma en que se consume el backend.

Estos ejemplos no constituyen una elección de feature para el proyecto ni requisitos adicionales obligatorios.

### Presentación de la feature adicional

- [ ] Contar qué se hizo.
- [ ] Justificar la feature a nivel de producto y comunicar su valor.
- [ ] Justificar la feature a nivel técnico.
- [ ] Realizar una pequeña demo.
- [ ] Profundizar en el código y en los aspectos técnicos de su funcionamiento.

**Pendiente de comunicación del equipo docente:** el tiempo de presentación de cada grupo. El equipo docente resolverá cómo dar más espacio y tiempo a los grupos.

**Aspectos no especificados en el comunicado:** los datos de usuario y los gráficos concretos a visualizar, y el alcance de la feature adicional. No se fijan aquí decisiones de producto o implementación.

---

## Sprint 3

**Fecha de entrega:** 08/12/2026

### Core

- [ ] Crear un test de arquitectura
- [ ] Auditoría de Web-Services
- [ ] Loguear `<timestamp,user,operación/metodo,parámetros,tiempoDeEjecucion>` de los servicios publicados con Spring utilizando Log4j/logback
- [ ] Crear TAG en GitHub
- [ ] Confeccionar Release Notes de entrega 3
- [ ] Configurar Spring Boot Prometheus para métricas
- [ ] Configurar Spring Boot Actuator para endpoints de monitoreo

Referencias:

https://www.baeldung.com/spring-boot-prometheus

https://www.baeldung.com/spring-boot-actuators

Release Notes:

https://drive.google.com/file/d/0BxzNm_rO4O8yZU52eF9QeXR4X3M/view?usp=sharing&resourcekey=0-v26hiy-2NSGhgdBnZ1KlbQ

### Modelo

No hay requisitos indicados en esta sección.

### Funcionalidad

- [ ] Optimizar ranking de jugadores según estrategia activa para alta frecuencia de consultas
- [ ] Endpoint de métricas avanzadas
