# Constitución de Football Market

## 1. Documentos normativos

Esta constitución contiene las reglas globales. [Arquitectura](../../docs/architecture.md) define roles, dependencias y convenciones; [testing](../../docs/testing.md) define las pruebas del backend. Ambos tienen la misma obligatoriedad cuando resulten aplicables.

Consultar solo las secciones pertinentes y sus reglas generales, siguiendo las guías de lectura de cada documento. Las instrucciones operativas están en [AGENTS.md](../../AGENTS.md). Specs y Plans no deben repetir restricciones globales salvo consideraciones específicas de la feature.

## 2. Calidad y alcance

- Preferir la solución válida más simple, con responsabilidades coherentes y detalles encapsulados mediante contratos. No crear capas, interfaces, dependencias o abstracciones por costumbre o necesidades hipotéticas; una duplicación no exige por sí sola abstraer.
- No agregar dependencias si las capacidades existentes bastan. Las tecnologías nuevas requieren justificación técnica y aprobación; las versiones administradas por Spring Boot no se sobrescriben sin necesidad justificada.
- Limitar los cambios a la tarea y a la coherencia de sus elementos afectados. Refactorizar solo con beneficio concreto, preservando comportamiento ajeno; revisar consumidores antes de eliminar o renombrar elementos.
- Eliminar código muerto dentro del alcance. Mantener comentarios útiles y actualizados, sin repetir lo evidente.
- Reportar violaciones o fallos preexistentes ajenos al cambio sin ocultarlos ni ampliar el alcance para corregirlos.

## 3. Seguridad

- Validar entradas externas no confiables conforme al contrato y las reglas funcionales. No deshabilitar seguridad para simplificar una implementación.
- Usar la autenticación del proyecto y comprobar autorización de forma independiente, incluidos roles, permisos y pertenencia al recurso cuando corresponda.
- Usar parametrización, binding o escaping seguro; no concatenar entrada externa cuando exista un mecanismo seguro equivalente. No implementar criptografía propia.
- Mantener secretos, credenciales y tokens fuera del código y del repositorio; no filtrar información sensible en respuestas, logs, errores o configuraciones versionadas.
- Aplicar mínimo privilegio, valores seguros por defecto y comunicaciones seguras, incluido HTTPS cuando corresponda. Corregir vulnerabilidades relevantes introducidas o agravadas por el cambio.

## 4. Contratos y sincronización

Los consumidores dependen de contratos, no de detalles internos. Todo cambio observable es un cambio de contrato: evaluar compatibilidad, incluidos campos, obligatoriedad, significado y errores. No introducir incompatibilidades silenciosas; tratar contratos con consumidores desconocidos como potencialmente utilizados.

Actualizar en el mismo cambio los elementos afectados: Spec, implementación, tests del backend, DTO, documentación, OpenAPI, Postman y configuración. Resolver discrepancias entre Spec y código según el comportamiento acordado; nunca cambiar la Spec para justificar una desviación no acordada ni alterar tests o controles para ocultar fallos.

Las convenciones HTTP y de documentación del backend se definen en arquitectura §2.7–2.8.

## 5. Idioma

Código nuevo o modificado en inglés: identificadores, paquetes, endpoints y campos JSON. Los nombres impuestos por contratos externos conservan su forma. Los nombres descriptivos de tests deben estar en español; el resto de su código sigue la regla general.

Documentación propia, Specs, Javadoc, OpenAPI y mensajes de validación en español; documentación externa puede conservar su idioma. Nombres de ramas, commits, Pull Requests e Issues en inglés. No traducir código existente fuera del alcance: aplicar estas convenciones a los elementos nuevos o modificados.

## 6. Verificación

El agente tiene prohibido ejecutar tests, incluso indirectamente. Los tests del backend los ejecuta el usuario; si se necesitan resultados, solicitar los casos concretos y su salida. El frontend queda temporalmente exento de testing: no crear ni ejecutar tests ni incorporar infraestructura para ellos.

El único control ejecutable por el agente es el build sin tests del área cuyo código cambió, con los comandos de AGENTS.md. Los cambios exclusivamente documentales no requieren build.

Se puede entregar con el build aprobado y los tests del backend explícitamente pendientes del usuario. Un fallo preexistente verificablemente ajeno al cambio no bloquea la entrega, pero debe reportarse. SonarQube, Quality Gates y otros controles opcionales quedan a cargo del usuario y no condicionan la finalización. El cambio debe dejar coherentes todos sus elementos afectados; toda ampliación del alcance requiere justificación explícita.

## 7. Gobernanza

Los agentes no pueden modificar, ignorar ni reinterpretar la Constitution ni los documentos normativos aplicables para justificar o facilitar una implementación incompatible. Solo pueden modificarlos cuando el usuario solicite explícitamente una modificación normativa.

Ante un conflicto necesario, proponer la enmienda antes del cambio incompatible. Si los documentos, la Spec y el código no resuelven una decisión normativa relevante, solicitar aclaración sin inventar reglas o excepciones.

**Versión**: 3.1.0 | **Ratificación**: 2026-08-31 | **Última enmienda**: 2026-09-21
