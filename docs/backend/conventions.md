# Convenciones del backend

## Guía de lectura

Consultar la [arquitectura del backend](architecture.md) para responsabilidades, ubicaciones, contratos y dependencias; este documento regula las convenciones de implementación. Ninguna convención permite evadir sus límites arquitectónicos. El stack, versiones y dependencias tecnológicas se documentan en [tecnologías del backend](technologies.md), y las reglas de pruebas en [testing](testing.md).

## DTO y Model

- Todos los DTO del contrato HTTP, tanto Request como Response, se implementan como `record`.
- Los Model no deben utilizar `Builder` ni `@Setter`. Sus cambios de estado se realizan conforme a las invariantes definidas en [arquitectura](architecture.md) §1.5.

## Mapper

- Los Mapper son `final`, sin estado mutable, con constructor privado y métodos `static`. No son componentes Spring ni se utilizan mediante instancias.
- Un Mapper que pueda invocarse fuera de un endpoint HTTP validado con Jakarta Validation y reciba `null` debe lanzar la excepción de mapeo definida por el proyecto. No se requiere una validación de `null` específica si solo es alcanzable desde un endpoint cuya validación estructural ya impida ese caso. No convertir `null` en valores por defecto.

## Excepciones

- El código de la aplicación no lanza directamente excepciones genéricas provistas por Java, como `RuntimeException`, `IllegalArgumentException` o `IllegalStateException`, para representar errores funcionales, de aplicación o de dominio. Utilizar las categorías definidas en [arquitectura](architecture.md) §1.6.

## Referencias a miembros de instancia en Java

- Toda clase productiva utiliza `this` al acceder a sus atributos de instancia e invocar sus propios métodos de instancia, incluso sin ambigüedad. Aplica también a métodos triviales y al código de los `record` definido explícitamente por el proyecto.
- En los tests aplica la excepción a esta convención productiva definida en [testing](testing.md) §16.
- No aplica a miembros `static`, variables locales, parámetros que no sean miembros de instancia ni código generado automáticamente por Java, Lombok, frameworks u otras herramientas.

## Documentación de endpoints y Javadoc

- Todo endpoint funcional nuevo o modificado se documenta en OpenAPI y se incorpora o actualiza en la colección Postman versionada en la misma tarea. Ambos reflejan propósito, método, ruta, parámetros, headers, autenticación, DTO, validaciones, códigos y errores aplicables. Los ejemplos deben poder utilizarse sin credenciales reales. Documentar el significado de campos DTO y, cuando aporte valor, ejemplos y restricciones. Spring REST Docs se rige por [testing](testing.md) §8.
- Javadoc es obligatorio en clases públicas. En métodos productivos, incluidos los `public` y `protected`, y en constructores explícitos, es obligatorio cuando sea necesario para documentar propósito, contrato, comportamiento, parámetros, retorno, excepciones, precondiciones o cualquier aspecto no evidente por sí mismo. No se exige en métodos triviales o evidentes ni debe repetir redundantemente el código. Los métodos sobrescritos conservan documentación accesible; `{@inheritDoc}` basta si cubre el contrato completo.
- El Javadoc de una declaración se ubica siempre antes de todas sus anotaciones.
- En tests, métodos de prueba y ciclo de vida no requieren Javadoc; helpers solo cuando necesitan explicación. Modificar el cuerpo de un método exceptuado no lo obliga a tener Javadoc.
