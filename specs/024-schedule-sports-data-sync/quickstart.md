# Guía de validación humana

Esta guía describe comprobaciones posteriores a una implementación autorizada; **no acredita que exista ni que haya sido ejecutada**. Los tests los ejecuta el usuario, no el agente.

## Prerrequisitos

Backend implementado, JDK 21 y configuración de desarrollo del proyecto. Las pruebas automatizadas deben sustituir proveedores y reloj cuando corresponda, sin credenciales reales ni navegador real. Para verificaciones con persistencia usar PostgreSQL aislado mediante Testcontainers y Docker, según `docs/backend/testing.md`. No usar datos de producción.

## Pruebas automatizadas

Desde `backend/`, ejecutar la suite aplicable tras implementar y clasificar las pruebas:

```bash
./gradlew fastTest
```

Si se agregan pruebas de integración requeridas para preservar la fachada, ejecutar además:

```bash
./gradlew integrationTest
```

Esperado: los tests verifican cron y zona, orden exacto JSON → scraping → evaluación, ausencia de etapas posteriores ante fallo o interrupción y continuidad ante parcial sin fallo global. Deben incluir `PROVIDER_UNAVAILABLE` con resumen `PARTIAL` o `COMPLETED`, error seguro y etapa en logs, ausencia de secretos, disponibilidad posterior de operaciones manuales y no invocación de cotizaciones. La ejecución humana y su resultado deben registrarse por separado.

## Comprobación operativa

En un entorno de desarrollo preparado con datos y proveedores autorizados, mantener la aplicación activa en el horario semanal y consultar el registro operativo existente. Debe observarse inicio, etapas ordenadas y resultado final; ante fallo, etapa y causa segura. No modificar el horario de producción ni ejecutar procesos reales en un entorno no autorizado para acelerar esta comprobación. Las fechas y selección de campaña pertenecen a cada operación, como describe [el contrato](contracts/scheduled-execution.md).
