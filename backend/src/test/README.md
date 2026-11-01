# Tests del backend por funcionalidad

Los paquetes de `java/footballmarket/features/` reflejan los de `src/main/java`: la
funcionalidad es el primer nivel y el paquete del componente probado determina el resto.
`application` incluye Services y coordinación; `domain`, reglas y datos de dominio;
`infraestructure`, persistencia y configuración propia; `presentation`, contratos HTTP.
`security` y `e2e` dentro de cada funcionalidad agrupan permisos y journeys respectivamente.
Las categorías y fronteras siguen las [normas de testing](../../../docs/backend/testing.md).

## Correspondencia funcional

Una funcionalidad técnica puede participar en varios requerimientos; el directorio no sustituye
los criterios ni las tareas de cada SPEC. La correspondencia se consulta en [specs](../../../specs/).

| Paquete bajo `features/` | Funcionalidad | SPEC relacionadas |
| --- | --- | --- |
| `auth` | Registro, login y usuario actual | 001, 003 |
| `catalog` | Catálogo, equipos, ligas y detalle de jugadores | 002, 007, 009 |
| `images` | Imágenes de jugadores | 006 |
| `identity` | Identificación de jugadores en WhoScored | 008 |
| `statistics` | Extracción y consulta de estadísticas | 012, 009 |
| `valuation` | Valoraciones estadísticas | 013 |
| `market` | Compras, publicaciones y ventas de tokens | 010, 011; inicialización compartida con 016 |
| `portfolio` | Portfolio, contabilidad de posiciones e historial | 014, 015, 020 |
| `quote` | Cotizaciones e inicialización del mercado | 016 |
| `ranking` | Rankings estadísticos | 018 |

Las integraciones externas están en `integrations/<provider>/`, la seguridad transversal en
`security/`, el logging en `common/config/` y los helpers reutilizados entre funcionalidades en
`support/`. `TestFixtures` y `ValuationDatabaseSupport` se trasladaron a `support` porque sus
consumidores pertenecen a distintas funcionalidades. La base `ListingRepositorySupport` permanece
con los tests de persistencia de `market` para conservar su acceso de paquete.

## Ejecución a cargo del usuario

Desde `backend/`, con Docker disponible para integración y E2E:

```powershell
.\gradlew.bat test
.\gradlew.bat test --tests 'footballmarket.features.portfolio.*'
.\gradlew.bat test --tests 'footballmarket.features.market.presentation.OrderControllerTest*'
```

Las suites `unitTest`, `lightweightTest`, `integrationTest`, `e2eTest` y `fastTest` siguen
seleccionando por `@Tag`, no por directorio. El sufijo `*` en la selección de una clase incluye
sus grupos `@Nested`. Los filtros antiguos con `footballmarket.controllers`, `services`,
`models` o `repositories` deben usar ahora el paquete de la funcionalidad correspondiente.

## Alcance y autorización de la reorganización

El 2026-11-01 el usuario confirmó expresamente reorganizar los tests para reflejar `main/java`,
ajustar paquetes e imports sin cambiar casos, assertions ni comportamiento productivo, y actualizar
la regla de organización de `docs/backend/testing.md`. Este cambio transversal es técnico: no crea
una nueva funcionalidad ni atribuye aprobación de revisión, testing o cierre a las SPEC afectadas.
La ejecución y aceptación de las pruebas quedan pendientes del usuario.
