# Guía de validación: tendencias y demanda de mercado

Guía para validación humana tras IMPLEMENT autorizado. No ejecutar aplicación, SQL, builds ni tests durante PLAN. Requisitos: [spec.md](spec.md); contratos: [market-trends-api.md](contracts/market-trends-api.md) y [market-demand-recalculation-api.md](contracts/market-demand-recalculation-api.md); persistencia: [data-model.md](data-model.md).

## Prerrequisitos

- Backend iniciado con PostgreSQL y migraciones Flyway vigentes.
- Mercado inicializado y datos de mercado disponibles.
- JWT válido y autorización existente para la operación manual.
- Ventana de demanda configurada o default de 24 horas.

## Escenarios HTTP

1. Consultar `GET /api/market/trends` con JWT; comparar período, top-5, cantidades/importes comprados, porcentajes, sesgo y volumen con transacciones confirmadas en la ventana.
2. Verificar compra P2P: se suma una vez como compra, contribuye positivamente a demanda y no aparece simultáneamente como venta.
3. Consultar sin transacciones: lista vacía, volúmenes y total cero, porcentajes y sesgo nulos. Contrastar con actividad equilibrada, que produce neutralidad sin confundirse con ausencia.
4. Ejecutar `POST /api/quotes/recalculate/market-demand`; verificar población elegible, ausencia de consulta de scores, ajuste solo de mercado y auditoría identificable.
5. Ejecutar recálculo habitual con score y actividad: verificar componentes separados, composición, cap y piso.
6. Ejecutar recálculo habitual sin score: comprobar comportamiento vigente sin score; verificar que la operación manual separada aplica mercado.
7. Para un mismo jugador comprobar demandas netas +10, −20 y extremos ±100 tokens antes del cap, así como cap y piso en el resultado.
8. Verificar JWT ausente/inválido, mercado no inicializado, demanda neta cero, jugador no elegible y fallo individual conforme a SPEC-028 y los contratos.

## Verificación backend a cargo del usuario

Ejecutar las pruebas backend definidas en `tasks.md` para la historia que se valida; para US1 corresponden las tareas T005–T007 y T008. El usuario ejecuta los tests. El build permitido para código backend, si aplica en IMPLEMENT, es `./gradlew build -x test` desde `backend/`.
