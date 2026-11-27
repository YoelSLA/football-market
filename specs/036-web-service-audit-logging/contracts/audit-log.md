# Contrato del registro de auditoría HTTP

## Alcance

Una petición entrante procesada por un endpoint REST publicado con Spring produce un registro mediante SLF4J/Logback al finalizar, también si el procesamiento del endpoint termina con error. No cubre llamadas salientes a proveedores externos.

## Campos observables

El registro identifica:

- timestamp;
- usuario autenticado o `anonymous`;
- método HTTP;
- ruta/operación sin query string;
- parámetros de consulta reconocidos por el endpoint, excepto identificadores; los demás parámetros reconocidos pueden registrarse;
- duración en milisegundos.

## Protección de datos

- No registrar bodies ni cabeceras.
- No registrar identificadores recibidos como parámetros de consulta, credenciales, contraseñas ni JWT.
- Los demás parámetros de consulta reconocidos por el endpoint no se consideran sensibles y pueden registrarse.
- Omitir parámetros no reconocidos por el endpoint.
- No persistir estos eventos en una tabla ni ofrecer una interfaz gráfica para consultarlos.

El formato de serialización y el nombre exacto del logger no son parte del contrato funcional; se reutiliza la configuración de logging existente.
