# Registro de deuda técnica

## 1. Introducción y propósito

Este documento es el registro único y centralizado de la deuda técnica del proyecto Football
Market. Su propósito es que todo problema real detectado durante una implementación, revisión o
refactorización quede escrito, trazado y con criterio de cierre explícito, en lugar de depender de
la memoria, de un chat o de una anotación dispersa en los artefactos de una feature.

El registro no sustituye la documentación normativa ni las especificaciones. Las specs siguen
siendo la verdad funcional de cada feature y la deuda técnica no puede relajar sus requisitos: una
deuda describe trabajo diferido, nunca comportamiento deseado omitido.

## 2. Convenciones de registro

- Cada deuda tiene un identificador único con el formato `TD-NNN`, numerado de forma correlativa.
- **Los identificadores no se reutilizan**, tampoco cuando una deuda se resuelve.
- **No se eliminan las deudas resueltas**: su estado cambia a `Resuelta` y se conserva la entrada
  completa, incluida la fecha de resolución.
- **No se modifica la fecha de creación original** de una deuda, aunque luego se actualice.
- **No se registran deudas especulativas.** Solo problemas reales, comprobados y descritos con
  evidencia.
- **No se duplican deudas.** Si un problema ya está registrado, se actualiza su entrada en lugar de
  crear otra.
- **Toda actualización indica fecha, hora y descripción del cambio** en el historial de la entrada.
- **No se declara una deuda resuelta sin evidencia verificable**, en [testing](backend/testing.md)
  o en la verificación que la deuda misma define.
- Las fechas y horas usan el formato `YYYY-MM-DD HH:MM:SS` en la zona `America/Argentina/Buenos_Aires`,
  obtenidas del reloj del sistema. No se inventan fechas históricas: cuando una deuda se identificó
  antes de existir este registro, la fecha registrada es la de su incorporación al documento.
- Cuando una deuda requiere una decisión del responsable del proyecto, su estado es `Pendiente`
  hasta que esa decisión se tome y se registre.

## 3. Estados posibles

| Estado | Significado |
| --- | --- |
| `Pendiente` | Identificada, sin decisión tomada o pendiente de autorización para resolverla. |
| `En curso` | Con decisión tomada y trabajo autorizado, todavía sin verificar. |
| `Resuelta` | Implementada y verificada con la evidencia que la propia entrada define. |
| `Descartada` | Se evaluó y se descartó, con la justificación registrada en su historial. |

## 4. Prioridades

| Prioridad | Criterio |
| --- | --- |
| `Alta` | Afecta la seguridad, la integridad de los datos o la corrección de un contrato; puede exponer información o permitir un uso indebido. |
| `Media` | Degrada la mantenibilidad, el rendimiento o la consistencia interna sin impacto directo en el usuario. |
| `Baja` | Mejora de legibilidad, limpieza o consistencia sin efecto observable. |

## 5. Índice de deudas técnicas

| ID | Título | Área | Prioridad | Estado | Registrada |
| --- | --- | --- | --- | --- | --- |
| [TD-001](#td-001--protección-de-los-endpoints-de-monitoreo-de-spring-boot-actuator) | Protección de los endpoints de monitoreo de Spring Boot Actuator | Backend / Seguridad | Alta | Pendiente | 2026-10-14 16:29:48 |
| [TD-002](#td-002--la-guardia-de-arranque-de-la-fuente-de-cotización-es-inalcanzable) | La guardia de arranque de la fuente de cotización es inalcanzable | Backend / Configuración | Media | Pendiente | 2026-10-16 20:07:06 |

## 6. Detalle de las deudas técnicas

### TD-001 — Protección de los endpoints de monitoreo de Spring Boot Actuator

| Campo | Valor |
| --- | --- |
| **ID** | TD-001 |
| **Fecha y hora de registro** | 2026-10-14 16:29:48 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Alta |
| **Área afectada** | Backend / Seguridad |
| **Fecha de resolución** | — |

**Descripción.** Durante el desarrollo se decidió permitir temporalmente el acceso sin
autenticación a los endpoints de monitoreo de Spring Boot Actuator, para poder consultarlos desde
Postman durante el diagnóstico local. La configuración habilita `/actuator`, `/actuator/health`,
`/actuator/info` y `/actuator/metrics`.

La exposición por HTTP de las métricas es doblemente restrictiva: requiere la habilitación
explícita por entorno `MANAGEMENT_EXPOSED_ENDPOINTS=health,info,metrics`, que el repositorio no
define por defecto. Aun así, la autorización es abierta para las rutas de monitoreo.

**Motivo del aplazamiento.** La medida responde a una decisión consciente y temporal del
responsable del proyecto: el monitoreo es una herramienta de diagnóstico local y no está prevista
para ningún despliegue público. Implementar la protección antes de que exista un despliegue real
sumaría complejidad arquitectónica —autorización por roles o un puerto de gestión independiente—
que el proyecto todavía no necesita.

**Impacto técnico.** Una exposición accidental en producción podría revelar información operativa:
consumo de memoria de la JVM, uso de CPU del sistema y del proceso, número de hilos, tiempo de
actividad y métricas de las solicitudes HTTP servidas. `/actuator/info` quedaría accesible en todos
los perfiles; actualmente no expone datos porque no hay propiedades `management.info.*` declaradas,
pero esa condición depende de la configuración y no de un control de acceso. `/actuator/health` no
es parte del riesgo: `show-details` es `never` y solo informa el estado general.

**Solución propuesta.** Implementar controles de acceso adecuados para los endpoints de monitoreo.
Las alternativas a evaluar son:

- **Autorización administrativa.** Requiere un rol con autoridad real, que el sistema todavía no
  implementa: hoy `role` es informativo y no existe autorización por rol.
- **Restricciones de red.** Puerto de gestión independiente ligado a la loopback. Es la única
  variante que no depende del cliente, pero tiene un coste conocido: al compartir puerto los
  endpoints, `/actuator/health` pasaría también a quedar ligado a la loopback y dejaría de ser
  alcanzable desde fuera.
- **Separación del puerto de gestión.** Puede combinarse con la anterior y permite tratar la
  seguridad de ese contexto de forma independiente.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Haber implementado el control de acceso elegido para `/actuator/info` y `/actuator/metrics/**`.
2. Haber verificado el comportamiento efectivo de esa restricción en ejecución.
3. Mantener `/actuator/health` accesible sin información interna sensible.
4. No volver a exponer por error `env`, `heapdump`, `threaddump`, `configprops`, `beans` ni
   `shutdown`, cuya exposición está hoy vedada en todos los perfiles.

**Referencias.**

- `backend/src/main/java/footballmarket/config/SecurityConfig.java`
- `backend/src/main/resources/application.yml`
- `docs/backend/technologies.md`
- `specs/trello.md`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 16:29:48 | Registro inicial en `docs/TECHNICAL_DEBT.md`. La anotación previa existía en `specs/trello.md` y en `docs/backend/technologies.md`; la fecha de creación original de esas anotaciones no se conoce, por lo que se registra la de incorporación a este documento. |
| 2026-10-14 16:52:29 | Actualización de la referencia al mecanismo de habilitación: `FOOTBALLMARKET_MONITORING_LOCAL_METRICS` se reemplaza por `MANAGEMENT_EXPOSED_ENDPOINTS=health,info,metrics`. La corrección del arranque no altera el alcance, la prioridad ni el estado de la deuda, que continúa `Pendiente`. Sin cambios funcionales: la exposición sigue siendo abierta para las rutas de monitoreo. |

### TD-002 — La guardia de arranque de la fuente de cotización es inalcanzable

| Campo | Valor |
| --- | --- |
| **ID** | TD-002 |
| **Fecha y hora de registro** | 2026-10-16 20:07:06 (`America/Argentina/Buenos_Aires`) |
| **Estado** | `Pendiente` |
| **Prioridad** | Media |
| **Área afectada** | Backend / Configuración |
| **Fecha de resolución** | — |

**Descripción.** `QuoteSimulationConfiguration` registra la guardia `quoteSourceAvailabilityGuard`
como un `SmartInitializingSingleton` que lanza `MissingQuoteSourceException` cuando el contexto no
contiene ningún `QuoteSource`. Ese callback se invoca al final de la inicialización de los singletons,
es decir, **después** de que Spring haya instanciado los beans, por lo que nunca se alcanza cuando la
ausencia de la fuente ya provocó un fallo anterior. `BuyOrderServiceImpl` es un `@Service` con
inyección de constructor de `private final QuoteSource quotes;`, y su resolución falla durante la
instanciación, en una fase anterior.

La consecuencia observable es que el error de arranque real es
`NoSuchBeanDefinitionException: No qualifying bean of type 'footballmarket.integrations.QuoteSource'`,
y no `MissingQuoteSourceException`. Esta última solo se produce dentro de
`QuoteSimulationConfigurationTest`, que usa `ApplicationContextRunner` y por tanto no registra
`BuyOrderServiceImpl`.

**Motivo del aplazamiento.** El contrato vigente no exige esa clase de excepción concreta.
`specs/010-buy-tokens/contracts/quote-source.md` formula el requisito en tres lugares como «el
arranque falla con un error de configuración explícito» y «la aplicación no inicia», sin nombrar
`MissingQuoteSourceException` en ninguno de ellos. La única mención normativa del tipo está en
`specs/010-buy-tokens/plan.md`, y es una justificación de clasificación —el fallo de arranque
pertenece a la integración de cotización y no al negocio—, no una exigencia de comportamiento.

El comportamiento exigido por el contrato se cumple: sin fuente válida la aplicación no inicia, y el
mensaje identifica el tipo de bean ausente. Corregir el diagnóstico exigiría una decisión
arquitectónica que el contrato no reclama.

**Impacto técnico.** La guardia queda como código muerto en la aplicación real y
`MissingQuoteSourceException` solo es observable en pruebas. El diagnóstico de un despliegue mal
configurado es menos preciso de lo previsto, porque expone una excepción de cableado interna en lugar
del error de configuración de dominio. No afecta la seguridad, la integridad de los datos ni ningún
contrato HTTP, y no altera el fallo de arranque exigido.

**Solución propuesta.** Evaluar, sin adoptar por defecto:

- **Validador previo a la instanciación.** Reemplazar la guardia por un `BeanFactoryPostProcessor`
  que compruebe si existe alguna definición de bean de tipo `QuoteSource`. Es la única variante que
  produce el error de dominio previsto sin tocar la inyección obligatoria, pero inspecciona
  definiciones de beans, lo que resulta frágil frente a `@Conditional` y no encaja con el uso de
  `ApplicationContextRunner` de la prueba actual.
- **Aceptar el diagnóstico actual y retirar la guardia.** Eliminar el `SmartInitializingSingleton` y
  `MissingQuoteSourceException`, y documentar que el fallo de arranque se manifiesta como error de
  cableado. Es la alternativa de menor complejidad y la que respeta literalmente el contrato.

**Criterios de resolución.** La deuda no se considera resuelta hasta:

1. Haber elegido y aplicado una de las alternativas evaluadas.
2. Haber verificado en ejecución que, sin fuente real y con `market.quote.simulator.enabled=false`,
   la aplicación no inicia.
3. Mantener intacta la prioridad de la fuente real: con una fuente real registrada, el simulador no
   se registra aunque su propiedad esté habilitada.
4. No dejar código muerto en `QuoteSimulationConfiguration`.

**Referencias.**

- `backend/src/main/java/footballmarket/config/QuoteSimulationConfiguration.java`
- `backend/src/main/java/footballmarket/services/impl/BuyOrderServiceImpl.java`
- `backend/src/test/java/footballmarket/config/QuoteSimulationConfigurationTest.java`
- `backend/src/test/resources/application-test.yml`
- `specs/010-buy-tokens/contracts/quote-source.md`
- `specs/010-buy-tokens/plan.md`

**Historial de cambios.**

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-16 20:07:06 | Registro inicial. La limitación se detectó al corregir el arranque de los contextos de prueba de SPEC-010, tras retirar de `QuoteSimulationConfigurationTest` una `@Configuration` anidada que contaminaba el barrido de componentes y enmascaraba la ausencia de `QuoteSource`. Se verificó en los artefactos normativos que `MissingQuoteSourceException` no es un requisito contractual, por lo que se mantiene el comportamiento actual. Sin cambios funcionales. |

## 7. Historial de actualizaciones del registro

| Fecha y hora | Cambio |
| --- | --- |
| 2026-10-14 16:29:48 | Creación del registro centralizado con TD-001. Se consolidaron como entrada las referencias dispersas que existían en `specs/trello.md` y `docs/backend/technologies.md`, conservando la traza hacia ellas. |
| 2026-10-16 20:07:06 | Alta de TD-002: la guardia de arranque de la fuente de cotización se registra después de la fase que ya falla, por lo que `MissingQuoteSourceException` es inalcanzable en la aplicación real. El contrato exige que la aplicación no inicie, sin nombrar esa excepción, de modo que el comportamiento se mantiene sin cambios y la limitación queda documentada. |
