# Guía humana del flujo SDD — Football Market

> **Propósito:** saber qué hacer, en qué orden y qué revisar al desarrollar una Feature con OpenCode y Spec Kit. Esta guía es operativa para personas; **no sustituye** la Constitución 6.0.0 ni los contratos de los comandos y skills del repositorio. Ante diferencias, prevalecen las fuentes normativas vigentes.

## Flujo rápido

**Preparar → revisar READY → autorizar US → implementar → revisar código → ejecutar tests → aprobar US → CONVERGE → autorizar cierre → DONE.**

El orquestador administra artefactos, dependencias, Trello y evidencias. **Vos tomás las decisiones funcionales, autorizás el trabajo y ejecutás los tests.**

## Paso a paso: responsabilidades separadas

> **Lectura rápida:** 🤖 el agente prepara y registra; 👤 vos decidís, revisás, ejecutás tests y autorizás. Una vista consolidada no elimina las aprobaciones individuales por US.

### 1. Elegí la Feature y verificá el contexto

**👤 VOS:** Identificás qué querés desarrollar, abrís la rama correcta y confirmás la Feature cuando el agente te la muestre.

**🤖 AGENTE:** Detecta rama, identidad de la spec, artefactos y estado; pregunta si hay ambigüedad.

**💬 Mensaje sugerido:**

> Quiero trabajar en la Feature de autenticación. Identificá su estado y orquestá la preparación SDD sin implementar todavía.

**✅ Para avanzar:** La Feature correcta está identificada y no se creó una duplicada.

### 2. SPECIFY — Revisá `spec.md`

**👤 VOS:** Leés alcance, exclusiones, US, criterios de aceptación y casos límite; pedís correcciones si algo no representa lo que querés.

**🤖 AGENTE:** Prepara o actualiza `spec.md` con Spec Kit y te resume decisiones y dudas.

**💬 Mensaje sugerido:**

> Mostrame el alcance, las US, los criterios de aceptación y las dudas de spec.md.

**✅ Para avanzar:** Entendés qué se construirá y cómo se aceptará.

### 3. CLARIFY — Respondé las dudas

**👤 VOS:** Respondés decisiones funcionales, permisos, reglas de negocio y comportamiento esperado.

**🤖 AGENTE:** Detecta ambigüedades, formula preguntas y refleja tus respuestas en la spec.

**💬 Mensaje sugerido:**

> Continuá CLARIFY. Preguntame lo que requiera una decisión funcional y mostrame cómo quedó documentado.

**✅ Para avanzar:** No quedan supuestos funcionales importantes sin resolver.

### 4. PLAN — Revisá la solución técnica

**👤 VOS:** Revisás arquitectura, dependencias, seguridad y cambios relevantes; decidís ante alternativas importantes.

**🤖 AGENTE:** Produce `plan.md` y artefactos técnicos conforme a Spec Kit y normas del repo.

**💬 Mensaje sugerido:**

> Prepará PLAN y resumime arquitectura, dependencias, riesgos y decisiones que necesitan mi aprobación.

**✅ Para avanzar:** El plan respeta la spec y las reglas del proyecto.

### 5. CHECKLIST — Evaluá calidad de requisitos

**👤 VOS:** Evaluás los ítems que exigen criterio humano; solicitás completar requisitos o criterios insuficientes.

**🤖 AGENTE:** Selecciona y genera checklists generales y especializados; distingue pendientes de ítems evaluados.

**💬 Mensaje sugerido:**

> Mostrame los checklists aplicables, por qué los elegiste y cuáles requieren mi evaluación.

**✅ Para avanzar:** Los checklists requeridos están evaluados; generar no equivale a aprobar.

### 6. TASKS — Revisá el desglose

**👤 VOS:** Comprobás que no falten tareas, dependencias, prerrequisitos ni responsabilidades humanas.

**🤖 AGENTE:** Genera `tasks.md` por US y dependencias, sin asignarse ejecución de tests.

**💬 Mensaje sugerido:**

> Mostrame las tareas por US, prerrequisitos compartidos y tareas reservadas al usuario.

**✅ Para avanzar:** Las US y requisitos tienen tareas trazables.

### 7. ANALYZE — Confirmá READY

**👤 VOS:** Leés hallazgos; decidís sobre conflictos importantes y confirmás que el alcance está listo.

**🤖 AGENTE:** Cruza spec, plan, tasks, checklists y Constitución; informa blockers y evidencia de READY. Analyze es estrictamente de lectura: no crea tarjetas.

**💬 Mensaje sugerido:**

> Ejecutá ANALYZE. Informame blockers, hallazgos y si la Feature está READY.

**✅ Para avanzar:** No quedan blockers. Al cerrar READY, el agente crea o reutiliza las tarjetas de cada US y las vincula a la épica; READY no significa permiso para implementar.

### 8. Autorizá IMPLEMENT de las US seleccionadas

**👤 VOS:** Elegís qué US y prerrequisitos compartidos autorizás; podés autorizar varias en una sola respuesta.

**🤖 AGENTE:** Presenta el alcance seleccionable y persiste evidencia de autorización **por cada US**.

**💬 Mensaje sugerido:**

> Autorizo IMPLEMENT de US1, US2 y US3 de la Feature 008, con los prerrequisitos compartidos identificados. No autorizo cambios de alcance.

**✅ Para avanzar:** Cada US autorizada tiene evidencia válida; otras US siguen sin autorización.

### 9. IMPLEMENT — Supervisá el desarrollo

**👤 VOS:** Seguís el progreso y respondés ante cambios de alcance o bloqueos. **No delegás la ejecución de tests.**

**🤖 AGENTE:** Implementa únicamente US y prerrequisitos autorizados, respeta dependencias y prepara pruebas cuando corresponda; no las ejecuta.

**💬 Mensaje sugerido:**

> Continuá con las US autorizadas. Informame progreso y detenete si aparece una decisión nueva.

**✅ Para avanzar:** El código autorizado está preparado para revisión.

### 10. CODE REVIEW — Revisá y aprobá código

**👤 VOS:** Revisás cambios, criterios de aceptación, seguridad y riesgos; aprobás o rechazás explícitamente **cada US**.

**🤖 AGENTE:** Entrega Code Review Report por US; puede mostrar un resumen consolidado para facilitar la revisión.

**💬 Mensaje sugerido:**

> Prepará el Code Review consolidado, separando hallazgos y aprobación pendiente de cada US.

**✅ Para avanzar:** Cada US aprobada tiene su gate y evidencia de Code Review; las rechazadas vuelven a corrección.

### 11. TESTING — Ejecutá y validá las pruebas

**👤 VOS:** **Ejecutás personalmente** los tests aplicables, registrás comandos, resultados, entorno y fecha; aprobás o rechazás Testing por US.

**🤖 AGENTE:** Entrega Testing Report e instrucciones exactas; registra tus resultados sin inventar ejecuciones. Respeta excepciones como frontend.

**💬 Mensaje sugerido:**

> Estos son los resultados que ejecuté: [comandos, resultados, fecha]. Registralos y mostrame qué US pueden aprobar TESTING.

**✅ Para avanzar:** Los gates de Testing tienen resultados humanos verificables. El agente nunca ejecuta tests, tampoco indirectamente.

### 12. US DONE — Verificá las aprobaciones

**👤 VOS:** Confirmás que todas las US requeridas tienen Code Review y Testing aprobados y no hay pendientes ocultos.

**🤖 AGENTE:** Comprueba gates y evidencia; sincroniza los estados permitidos con Trello.

**💬 Mensaje sugerido:**

> Mostrame las US requeridas, sus gates y si se cumplen los prerrequisitos de CONVERGE.

**✅ Para avanzar:** Todas las US requeridas están DONE.

### 13. CONVERGE — Revisá la consistencia final

**👤 VOS:** Leés los hallazgos y autorizás explícitamente cualquier corrección posterior; repetís revisiones humanas afectadas cuando haga falta.

**🤖 AGENTE:** Ejecuta CONVERGE solo cuando las US requeridas están DONE; diagnostica y anexa tareas de remediación a `tasks.md` cuando corresponda, sin ejecutar tests ni implementar correcciones.

**💬 Mensaje sugerido:**

> Ejecutá CONVERGE conforme al comando local. Mostrame hallazgos y remediaciones; no implementes correcciones sin autorización.

**✅ Para avanzar:** CONVERGE es satisfactorio y vigente, o las remediaciones se resuelven y se repite el flujo afectado.

### 14. Autorizá el cierre de la Feature

**👤 VOS:** Revisás evidencia de US DONE, CONVERGE y blockers; emitís una **autorización final independiente**.

**🤖 AGENTE:** Presenta el resumen de evidencias y registra FEATURE COMPLETION AUTHORIZATION en el formato canónico.

**💬 Mensaje sugerido:**

> Revisé la evidencia y CONVERGE de Feature 008. Autorizo explícitamente el cierre de la Feature 008.

**✅ Para avanzar:** La autorización final está registrada y vigente.

### 15. FEATURE DONE — Verificá el resultado

**👤 VOS:** Comprobás el estado final, reportes y diff; autorizás commits o push por separado si corresponde.

**🤖 AGENTE:** Sincroniza documentación y Trello; entrega resumen de alcance, US, gates, CONVERGE y archivos cambiados.

**💬 Mensaje sugerido:**

> Dame el resumen final de la Feature, las US DONE, la evidencia y el diff.

**✅ Para avanzar:** La Feature está DONE con trazabilidad completa.

## Si interrumpís una sesión

Decí: «Continuá la Feature 008. Reconstruí el estado desde los artefactos y la evidencia registrada. Antes de avanzar, indicame la próxima acción permitida y si existe alguna autorización pendiente».

No vuelvas a aprobar automáticamente algo que ya tenga evidencia válida; tampoco supongas que una columna de Trello constituye una autorización.

## Cómo leer Trello

| Columna | Interpretación práctica |
|---|---|
| BACKLOG | Trabajo identificado, todavía no preparado. |
| SPECIFYING | Definición y preparación SDD. |
| READY | Preparado, con tarjetas de US sincronizadas; pendiente de autorización para implementar. |
| IN PROGRESS | Implementación o trabajo autorizado en curso. |
| CODE REVIEW | Revisión humana del código por US. |
| TESTING | Validación humana de pruebas por US. |
| DONE | US aprobada o Feature cerrada con sus condiciones cumplidas. |

**Nota:** el flujo normal de la Feature no necesariamente utiliza todas las columnas por las que pasa cada US. Seguí las transiciones definidas en [`sdd-workflow.md`](sdd-workflow.md), documento normativo subordinado a la Constitución.

## Regla de oro

**El agente prepara, implementa, documenta y mantiene la trazabilidad; las personas deciden, revisan, ejecutan tests y autorizan los gates.** Esta guía describe el lifecycle vigente informado para Constitución 6.0.0; si las normas del repositorio cambian, actualizala antes de usarla como referencia.
