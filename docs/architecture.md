# Arquitectura común

## Guía de lectura

Consultar §1–2 en toda tarea que involucre arquitectura, estructura o dependencias arquitectónicas. Para backend, consultar la [arquitectura del backend](backend/architecture.md); para frontend, consultar la [arquitectura del frontend](frontend/architecture.md). Para conocer o modificar el stack, tecnologías, versiones o dependencias, consultar [tecnologías del backend](backend/technologies.md) o [tecnologías del frontend](frontend/technologies.md), según corresponda. No es necesario consultar documentos tecnológicos ni del área ajena cuando la tarea no los afecte.

## 1. Reglas comunes y alcance de matrices

Crear o dividir componentes solo por responsabilidades reales, cohesión o necesidad arquitectónica, nunca por cantidad de líneas o archivos. Las estructuras enumeradas no obligan a crear carpetas o roles vacíos. Incorporar comportamiento al responsable correspondiente y exponer solo contratos necesarios.

Las matrices regulan dependencias entre roles arquitectónicos del proyecto, no todos los tipos utilizados. Java/TypeScript, colecciones, value types y colaboradores técnicos como `List`, `Clock`, `PasswordEncoder`, `JwtEncoder` o configuración pueden utilizarse cuando sean necesarios y compatibles con la responsabilidad del componente. Su ausencia en la matriz no justifica prohibirlos o eliminarlos.

Entre los roles regulados, toda dependencia no autorizada está prohibida. Ni colaboradores técnicos, frameworks, dependencias transitivas ni intermediarios permiten eludir restricciones expresas. No se permiten ciclos.

La arquitectura prevalece sobre estructuras existentes: refactorizar incompatibilidades dentro del alcance, sin conservarlas por costumbre. Toda nueva área debe definir responsabilidades, componentes y dependencias. Los vacíos normativos se resuelven según la gobernanza de la constitución.

## 2. Convenciones

Las convenciones definen cómo implementar técnicamente la arquitectura. No pueden contradecir las reglas arquitectónicas.

### 2.1. Estructura física

La estructura de paquetes, directorios y archivos debe ser coherente con los roles arquitectónicos definidos.

La estructura física no modifica por sí misma la responsabilidad arquitectónica de un componente.
