# Guía de validación — SPEC-026

Esta guía describe escenarios para validar la Feature una vez implementada. No se ejecutó ni se
ejecutará aquí ningún test. La ejecución de tests corresponde al usuario conforme a la Constitución y
a `docs/backend/testing.md`.

## Prerrequisitos

- Backend con PostgreSQL 18 de testing provisionado por Testcontainers y migraciones Flyway
  aplicadas automáticamente por la suite.
- Usuario `USER`, superusuario, jugador visible con cotización vigente positiva, créditos de ambas
  partes y tenencias del usuario preparadas; una parte de la tenencia debe poder reservarse en
  publicaciones P2P para verificar disponibilidad libre.
- Comisión de sistema configurable para los casos de prueba; valor por defecto 10%, rango válido
  10–99%.
- Tests en las categorías de Model, Service, Controller y Repository/Persistence que correspondan
  según las fronteras de `docs/backend/testing.md`.

Para iniciar el backend manualmente desde `backend/`, usar la configuración local y base de datos
habituales:

```bash
./gradlew bootRun
```

Después de configurar un JWT y datos de mercado de desarrollo, enviar solicitudes a
`http://localhost:8080/api/orders/sell-official` y `http://localhost:8080/api/orders/sell` según los
contratos vinculados. No utilizar credenciales ni datos productivos.

## Escenarios de aceptación

1. **Venta oficial y recibo**: con cotización 100, comisión 10% y 10 tokens libres, vender 5.
   Esperado: neto 90 centésimas, total 450; usuario +450 créditos y −5 tokens; superusuario
   −450 créditos y +5 tokens; recibo con partes, cotización, comisión, neto e importe; un registro
   financiero con snapshots correspondientes.
2. **Redondeo y límites de la comisión**: probar un resultado con fracciones exactas y mitad
   (half-up), comisiones 10% y 99% válidas, y 0%/100% inválidas. Esperado: precio entero
   determinista; configuración inválida impide aceptar una operación.
3. **Precio neto cero**: usar cotización de una centésima y comisión 99%; half-up produce cero.
   Esperado: rechazo completo, sin saldos, tenencias ni auditoría modificados.
4. **Rechazos sin efectos**: cantidad inválida, superusuario como iniciador, cotización ausente/no
   positiva, tenencia libre insuficiente, saldo insuficiente del superusuario y fallo al registrar.
   Esperado: ningún saldo, token o registro financiero parcial.
5. **Precio P2P indicado**: con cotización 100, publicar a 80 y 120 (aceptados), 79 y 121
   (rechazados con `409 INVALID_LISTING_PRICE`); un precio entero cero o negativo también responde
   `409 INVALID_LISTING_PRICE`, mientras que un valor no entero o textual responde
   `400 INVALID_REQUEST`.
6. **Precio P2P omitido y congelamiento**: omitir precio con cotización 100; esperado `unitPrice=100`.
   Cambiar luego la cotización y comprar la publicación; esperado: vendedor recibe y comprador paga
   el importe congelado.
7. **Compra oficial existente**: ejecutar el contrato directo de SPEC-010 sin cambios y comprobar
   precio, respuesta, saldos, tenencias y auditoría anteriores.
8. **Consumidores financieros**: consultar el historial del vendedor después del buyback y comprobar
   que se clasifica como venta; reconstruir su posición para comprobar que cantidad y costo
   remanentes incorporan la salida al precio realmente pagado.

## Criterios de verificación

- Para cada rechazo, afirmar estado financiero inalterado y ausencia de un registro confirmado,
  respetando las fronteras de la categoría de prueba.
- Para atomicidad, comprobar rollback real en la suite de persistencia/Service; no inferirla de
  mocks de repositorio.
- Para HTTP, verificar status, DTO, `ErrorResponseDTO`, validaciones, seguridad y anotaciones OpenAPI.
- Ejecutar desde `backend/` las suites seleccionadas por el usuario conforme a la guía local; el
  agente no ejecuta tests. El único build permitido tras cambios de código backend es
  `./gradlew build -x test`.
