# Quickstart: Validación del historial personal de transacciones

## Propósito

Guía para que el desarrollador valide la futura implementación de SPEC-025. PLAN no ejecuta estos
comandos ni acredita sus resultados.

## Prerrequisitos

- Backend y frontend configurados según la documentación del repositorio.
- Usuario autenticado con operaciones confirmadas para el escenario con datos.
- Usuario autenticado sin operaciones para el escenario vacío.
- Endpoint `GET /api/users/me/transactions` disponible sin cambios.

## Controles técnicos del frontend

Ejecutados por el usuario desde `frontend/` en los gates previstos por `frontend/AGENTS.md`:

```bash
npm run biome:fix
npm run build
```

No existen tests frontend para esta feature por la exención temporal vigente.

## Secuencia de implementación y revisión

1. Implementar lógica, estado, contratos y árbol JSX semántico conforme al plan.
2. El usuario ejecuta los controles técnicos y revisa integridad de datos y rutas.
3. Detenerse en el gate humano obligatorio.
4. Solo con autorización explícita, implementar SCSS y maquetación fina.
5. El usuario repite controles y confirma visualmente en el navegador.

## Escenarios funcionales

### 1. Historial con compras y ventas

1. Iniciar sesión con un usuario que tenga ambos tipos.
2. Abrir `Cartera > Historial de transacciones`.
3. Confirmar la ruta `/portfolio/transactions` y la pestaña activa.
4. Verificar una aparición por transacción, orden recibido y los siete datos obligatorios.
5. Confirmar `BUY → COMPRA`, `SELL → VENTA` y valores históricos con dos decimales.
6. Confirmar que el nombre del jugador no sea interactivo.

### 2. Carga y error

1. Observar la consulta pendiente y confirmar que no aparezcan cero operaciones ni filas previas.
2. Simular una respuesta fallida mediante las herramientas del entorno del desarrollador.
3. Confirmar mensaje, aclaración de que no se modificaron operaciones y botón `Reintentar`.
4. Restaurar la respuesta y verificar que el reintento muestre datos o vacío.

### 3. Historial vacío

1. Iniciar sesión con un usuario sin operaciones.
2. Confirmar que `[]` se presente como vacío exitoso, no error.
3. Activar `Ir al mercado` y verificar el destino `/market/buy`.

### 4. Privacidad de sesión

1. Consultar con dos usuarios distintos y comparar sus historiales.
2. Confirmar que no exista selector de identidad ni exposición cruzada.
3. Invalidar la sesión y verificar que actúe la protección global.

## Validación responsive y visual

- **1920 × 913 px**: navbar de 108 px, área neta de 805 px, sin scroll global; solo las filas
  desplazan dentro del panel.
- **769–1024 px**: cuatro grupos visuales, todos los datos y sin scroll horizontal.
- **360–768 px**: tarjetas en una columna, scroll documental y datos completos.
- En todos los rangos: padding, margin y gap propios de la página no superan 8 px.
- Verificar nombres largos, valores cero, singular/plural y una colección extensa.

### Protocolo medible de localización (SC-007)

1. Preparar un dataset de 20 operaciones ficticias representativas con compras y ventas.
2. Presentar la pantalla completamente cargada a una persona sin conocimiento previo del
   historial.
3. Indicar una transacción por su identificador y solicitar jugador, tipo e importe total.
4. Iniciar el cronómetro al comunicar la consigna y detenerlo cuando la persona responda los tres
   datos.
5. Registrar como éxito solo una respuesta completa y correcta en un tiempo menor o igual a 10
   segundos.

La ejecución y el registro del resultado corresponden a la validación posterior a la
implementación; esta guía no los acredita durante PLAN o TASKS.

Comparar la composición con `docs/wireframes/transaction-history.md` y el contrato con
`contracts/transaction-history.md`.

## Límites de aceptación

La validación debe rechazar filtros, búsqueda, ordenamiento interactivo, paginación, exportación,
imágenes, cotización actual, rentabilidad, navegación al jugador o cualquier modificación backend.
