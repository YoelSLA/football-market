# Modelo de datos: auditoría de petición

## Evento de auditoría de petición

Representación efímera emitida al finalizar una petición HTTP procesada por un endpoint REST Spring. No es una entidad persistida.

| Campo | Significado | Regla |
| --- | --- | --- |
| `timestamp` | Instante en que se genera el registro | Fecha/hora del evento, con zona/offset inequívoco. |
| `user` | Identidad que atendió la petición | Usuario autenticado del contexto de seguridad o literal `anonymous`. |
| `method` | Método HTTP recibido | Valor del método HTTP. |
| `operation` | Endpoint/operación atendida | Patrón de ruta MVC resuelto, sin query string. |
| `parameters` | Parámetros de consulta reconocidos por el endpoint, excepto identificadores | Ausente/vacío cuando no hay parámetros registrables; identificadores de consulta, credenciales y contraseñas excluidos. Los demás parámetros reconocidos pueden registrarse. |
| `durationMs` | Tiempo de atención de la petición | Duración no negativa expresada en milisegundos. |

## Relaciones y ciclo de vida

- Un evento corresponde a una petición entrante completada por un endpoint REST.
- Se genera también cuando el procesamiento del endpoint termina con error.
- El evento vive únicamente en el mensaje de logging; no se almacena en tabla ni se expone mediante UI/API.

## Datos excluidos

No forman parte del evento: body, cabeceras HTTP, token Bearer, identificadores recibidos como parámetros de consulta, contraseña ni credenciales. Los demás parámetros de consulta reconocidos por el endpoint no se consideran sensibles y pueden registrarse. No se registran parámetros que el endpoint no reconozca.
