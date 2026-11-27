# Guía de Validación Rápida: Tests de Arquitectura

**Feature**: `SPEC-037 | Tests de arquitectura en el backend`
**Fecha**: 2026-11-27
**Rama**: `feat/037-arch-tests`

Esta guía describe los procedimientos y comandos ejecutables para verificar la suite de pruebas de arquitectura y demostrar que previene violaciones estructurales.

---

## 1. Prerrequisitos

* Java 21 configurado en el entorno.
* Gradle wrapper ejecutable en el subdirectorio `backend/`.

---

## 2. Comandos de Validación

### Validar clasificación estática de pruebas
Verifica que las clases de prueba contengan exactamente una `@Tag("unit")` literal conforme a las convenciones de Gradle del repositorio:
```bash
cd backend
./gradlew validateTestClassification
```
*Resultado esperado:* `Clasificación válida: N clases de test.` sin advertencias ni errores.

### Ejecutar exclusivamente la suite de tests de arquitectura
Ejecuta las pruebas de arquitectura implementadas bajo `footballmarket.architecture`:
```bash
cd backend
./gradlew test --tests "footballmarket.architecture.*"
```
*Resultado esperado:* Las 4 clases de prueba ejecutan y aprueban en menos de 3 segundos en memoria.

### Ejecutar la suite rápida completa
Verifica que los tests de arquitectura se encuentren correctamente integrados en el pipeline de pruebas rápidas:
```bash
cd backend
./gradlew unitTest
```
*Resultado esperado:* `BUILD SUCCESSFUL`.

---

## 3. Demostración de Fallo Explícito ante Violaciones (Inyección controlada)

Para verificar el criterio de aceptación **CA-05** (*Las violaciones de arquitectura provocan el fallo explícito de las pruebas correspondientes*), se puede simular una transgresión de dependencias temporal:

1. **Inyección de prueba:** En un controlador cualquiera (ej. `PlayerController`), declarar temporalmente un atributo de tipo repositorio (ej. `PlayerRepository playerRepository`).
2. **Ejecución del test:**
   ```bash
   ./gradlew test --tests "footballmarket.architecture.LayerArchitectureTest"
   ```
3. **Resultado observable:**
   * El test `controllersNoDebenAccederARepositorios` falla inmediatamente.
   * La salida de error describe explícitamente:
     `Architecture Violation: Rule 'no classes that reside in a package '..presentation..' should depend on classes that reside in a package '..persistence..' was violated (1 times): Field <...PlayerController.playerRepository> has type <...PlayerRepository> in (PlayerController.java:XX)`
4. **Restauración:** Revertir la declaración temporal para volver al estado limpio y conforme.
