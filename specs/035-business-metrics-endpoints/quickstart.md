# Validación manual: métricas de negocio

## Prerrequisitos

- Backend iniciado con la configuración de desarrollo y base PostgreSQL del proyecto.
- Cuenta válida para obtener un JWT mediante el flujo de autenticación existente.
- `BASE_URL` apuntando al backend local y `TOKEN` conteniendo el JWT. No incluir credenciales reales en ejemplos ni documentación.

## Consultar ambos alcances

```bash
curl -i -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/business-metrics/historical"
curl -i -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/business-metrics/current"
```

Verificar `200 OK`, los seis campos exactos y los tipos definidos en [contracts/http.md](contracts/http.md); `queriedAt` debe ser UTC ISO 8601. Con una base sin actividad, todos los contadores y volúmenes deben ser cero.

## Comprobar protección

```bash
curl -i "$BASE_URL/api/business-metrics/historical"
curl -i "$BASE_URL/api/business-metrics/current"
```

Ambas solicitudes sin token deben responder `401 Unauthorized`.

## Comprobar agregados y reinicio

1. Preparar actividad confirmada a través de los flujos existentes: compra, recompra/venta oficial, liquidación P2P y cancelación de publicación; incluir una publicación vencida y una operación rechazada.
2. Consultar ambos endpoints. Confirmar que cada fila aporta una vez tokens e importe; la liquidación P2P se clasifica como compra, la recompra oficial como venta; solo `CANCELLED` incrementa cancelaciones. Una solicitud que ejecute varias operaciones debe contribuir una vez por operación completada.
3. Reiniciar el backend y volver a consultar: los contadores `current` deben volver a cero mientras `historical` conserva los agregados persistidos.
4. Revisar que ningún campo o etiqueta exponga identidades individuales y que las rutas no alteren `/actuator/prometheus`.

## Pruebas automatizadas (ejecución a cargo del usuario)

Desde `backend/`, ejecutar las pruebas de Controller, Service y Repository definidas para esta feature en `tasks.md` cuando se genere en la fase correspondiente. El agente no ejecuta tests ni builds durante PLAN.
