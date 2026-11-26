# Investigación: caché de consultas frecuentes

## Spring Cache con Caffeine

- **Decisión**: integrar Spring Cache y Caffeine con seis cachés nombradas, cada una con expiración
  tras escritura y capacidad máxima propias.
- **Fundamento**: Spring Boot proporciona la abstracción de caché y puede configurar un
  `CaffeineCacheManager` cuando Caffeine está presente. La documentación de Boot consultada
  contempla configurar cachés por `CaffeineSpec`, bean `Caffeine` o propiedades y documenta
  `maximumSize` y `expireAfterWrite`.
- **Alternativas consideradas**: Redis, excluido por FR-009; proveedor simple, insuficiente para
  políticas diferenciadas; implementación manual ajena a Spring Cache, duplicaría infraestructura.
- **Fuente de configuración local**: `backend/build.gradle.kts`,
  `backend/gradle/libs.versions.toml` y `docs/backend/technologies.md`. La dependencia no está aún
  declarada y la versión de Spring Boot vigente es 4.1.1; usar versiones administradas por Boot
  donde corresponda.
- **Referencia externa**: [Spring Boot — Caching](https://docs.spring.io/spring-boot/reference/io/caching.html).

## Claves de consulta

- **Decisión**: la identidad de una entrada se basa en el record normalizado de consulta, incluida
  paginación, filtros, orden y valores que cambian el cuerpo. El detalle del jugador usa su ID.
  El ranking global usa una clave constante; una consulta paginada usa todos sus criterios.
- **Fundamento**: los DTO y Records actuales exponen las entradas que gobiernan cada resultado;
  los valores predeterminados se resuelven antes de entrar al Service.
- **P2P**: resolver el ID local desde el principal autenticado antes de consultar la caché y
  combinarlo con `P2PMarketQuery`. El email/sub del JWT no se usa como clave sustitutiva del ID ni
  se acepta un ID proporcionado por cliente.
- **Alternativas consideradas**: URL literal (representaciones equivalentes no necesariamente
  colapsan); subconjunto de parámetros (colisiones); identidad de cliente (riesgo de contaminación
  entre usuarios).

## Invalidación y transacciones

- **Decisión**: retirar las entradas afectadas una vez finalizado correctamente cada proceso.
  En Services transaccionales de mercado, manejar el evento tras commit.
- **Fundamento**: protege el estado frente a rollback y evita mantener respuestas previas después
  de completar una escritura.
- **Evidencia local**: el recálculo publica y completa mediante `QuoteRecalculationServiceImpl`;
  la sincronización de catálogo confirma con `TransactionTemplate` antes de retornar; las
  operaciones financieras tienen Services transaccionales.
- **Alternativas consideradas**: esperar TTL (no satisface consistencia inmediata exigida);
  invalidar antes de ejecutar (no distingue fallos/reversiones); dependencia directa entre Services
  (contraria a la matriz arquitectónica).
