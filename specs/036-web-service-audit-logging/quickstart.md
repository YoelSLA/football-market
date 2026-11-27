# Validación manual: auditoría de peticiones REST

La ejecución corresponde al usuario. El agente no ejecuta tests.

## Prerrequisitos

- Backend configurado con sus dependencias y servicios requeridos.
- Aplicación ejecutándose con la configuración de logging habitual.
- Credenciales de prueba y, para la petición autenticada, un JWT válido de un usuario de prueba.

## Escenarios

1. Enviar una petición válida a un endpoint REST protegido con JWT. Confirmar una única entrada con timestamp, usuario autenticado, método, patrón de ruta, parámetros reconocidos y duración en milisegundos.
2. Enviar una petición a un endpoint público sin autenticación (por ejemplo, registro/login, usando datos de prueba). Confirmar que la identidad se indica como `anonymous`.
3. Enviar una petición a un endpoint que termine con error funcional. Confirmar que igualmente se emite el registro de auditoría.
4. Enviar una consulta con parámetros reconocidos no sensibles, identificadores y otros parámetros no reconocidos. Confirmar que se registran los reconocidos no sensibles y se excluyen identificadores y parámetros no reconocidos.
5. Usar credenciales de prueba en una solicitud al endpoint de autenticación. Inspeccionar todos los destinos de logging configurados y confirmar que no aparecen contraseñas, tokens ni identificadores de consulta.
6. Enviar una petición sin parámetros de consulta reconocidos. Confirmar que la auditoría no inventa parámetros.

## Resultado esperado

Cada escenario produce como máximo un registro de auditoría por petición procesada por un endpoint; cada registro respeta los campos y exclusiones definidos en [contracts/audit-log.md](contracts/audit-log.md). No se requiere consultar una tabla ni una interfaz gráfica.

## Pruebas automatizadas

Las pruebas backend que cubran la integración MVC, autenticación/anónimo, finalización con error y filtrado sensible se añadirán y ejecutarán según las normas de testing del backend. Su ejecución y resultado quedan a cargo del usuario.
