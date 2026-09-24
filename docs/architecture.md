# Arquitectura común

## Guía de lectura

Consultar §1–2 en toda tarea que involucre arquitectura, estructura o dependencias arquitectónicas. Para cada área, consultar su [arquitectura del backend](backend/architecture.md) o [arquitectura del frontend](frontend/architecture.md) y sus [convenciones del backend](backend/conventions.md) o [convenciones del frontend](frontend/conventions.md) cuando corresponda a implementación o formato. Para conocer o modificar el stack, tecnologías, versiones o dependencias, consultar [tecnologías del backend](backend/technologies.md) o [tecnologías del frontend](frontend/technologies.md), según corresponda. No es necesario consultar documentos tecnológicos ni del área ajena cuando la tarea no los afecte.

## 1. Reglas comunes y alcance de matrices

Crear o dividir componentes solo por responsabilidades, cohesión o necesidad arquitectónica, nunca por su tamaño. Los roles y estructuras definidos no obligan a crear componentes sin una responsabilidad real. Incorporar comportamiento al responsable correspondiente y exponer solo los contratos necesarios.

Las matrices regulan exclusivamente las dependencias entre los roles arquitectónicos definidos para cada área del proyecto. No regulan dependencias que no constituyan roles arquitectónicos. La ausencia de estas últimas en una matriz no debe interpretarse como una prohibición ni justificar su eliminación; su uso debe ser necesario y compatible con la responsabilidad del componente.

Toda dependencia no autorizada entre roles regulados está prohibida. Las restricciones se aplican tanto a dependencias directas como indirectas y no pueden eludirse mediante intermediarios. No se permiten ciclos entre roles arquitectónicos.

La arquitectura definida prevalece sobre prácticas o estructuras existentes incompatibles. Las incompatibilidades dentro del alcance de la tarea deben corregirse y no conservarse por precedente. Toda nueva área arquitectónica debe definir responsabilidades, componentes y dependencias. Los vacíos normativos se resuelven según la gobernanza de la constitución.

## 2. Convenciones

Las convenciones definen cómo implementar técnicamente la arquitectura. No pueden contradecir las reglas arquitectónicas.

### 2.1. Estructura física

La estructura de paquetes, directorios y archivos debe ser coherente con los roles arquitectónicos definidos.

La estructura física no modifica por sí misma la responsabilidad arquitectónica de un componente.
