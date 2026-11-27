# Contrato de Reglas de Arquitectura

**Feature**: `SPEC-037 | Tests de arquitectura en el backend`
**Fecha**: 2026-11-27
**Rama**: `feat/037-arch-tests`

Este documento define el catálogo formal de reglas ejecutables de ArchUnit que constituyen el contrato de integridad estructural del backend.

---

## 1. Reglas de Capas y Dependencias (`LayerArchitectureTest`)

### RULE-L01: Presentación no accede a Infraestructura ni Persistencia
* **Descripción:** Ninguna clase ubicada en paquetes `..presentation..` puede acceder a clases ubicadas en paquetes `..infra*structure..` o `..persistence..`.
* **Condición ArchUnit:**
  ```java
  noClasses()
      .that().resideInAPackage("..presentation..")
      .should().dependOnClassesThat()
      .resideInAnyPackage("..persistence..", "..infrastructure..", "..infraestructure..");
  ```
* **Violación esperada:** Acceso de un `@RestController` directamente a un `JpaRepository` o clase de infraestructura sin pasar por un `Service`.

### RULE-L02: Dominio no depende de Capa Web ni Presentación
* **Descripción:** Las entidades y clases de dominio (`..domain..`) no deben importar ni depender de clases de presentación (`..presentation..`) ni del framework web de Spring (`org.springframework.web..`).
* **Condición ArchUnit:**
  ```java
  noClasses()
      .that().resideInAPackage("..domain..")
      .should().dependOnClassesThat()
      .resideInAnyPackage("..presentation..", "org.springframework.web..");
  ```

### RULE-L03: Orquestadores no dependen de Persistencia, Integraciones ni otros Orquestadores
* **Descripción:** Las clases en `footballmarket.orchestrators..` no deben depender de repositorios, integraciones de terceros ni de otros orquestadores.
* **Condición ArchUnit:**
  ```java
  noClasses()
      .that().resideInAPackage("footballmarket.orchestrators..")
      .should().dependOnClassesThat()
      .resideInAnyPackage("..persistence..", "..infrastructure..", "..infraestructure..", "footballmarket.integrations..", "footballmarket.orchestrators..");
  ```

---

## 2. Reglas de Ciclos y Modularidad (`FeatureCyclesTest`)

### RULE-C01: Ausencia de ciclos entre paquetes de Features
* **Descripción:** No deben existir dependencias circulares directas ni indirectas entre los paquetes de funcionalidades (`footballmarket.features.*`), salvo los acoplamientos históricos aislados e inventariados en la deuda técnica [TD-041](../../docs/TECHNICAL_DEBT.md).
* **Condición ArchUnit:**
  ```java
  slices()
      .matching("footballmarket.features.(*)..")
      .should()
      .beFreeOfCycles()
      .ignoreDependency(resideInAPackage("..features.auth.."), resideInAPackage("..features.market.."))
      .ignoreDependency(resideInAPackage("..features.businessmetrics.."), resideInAPackage("..features.market.."))
      .ignoreDependency(resideInAPackage("..features.catalog.."), resideInAPackage("..features.identity.."))
      .ignoreDependency(resideInAPackage("..features.catalog.."), resideInAPackage("..features.images.."))
      .ignoreDependency(resideInAPackage("..features.catalog.."), resideInAPackage("..features.market.."))
      .ignoreDependency(resideInAPackage("..features.catalog.."), resideInAPackage("..features.statistics.."))
      .ignoreDependency(resideInAPackage("..features.market.."), resideInAPackage("..features.portfolio.."))
      .ignoreDependency(resideInAPackage("..features.market.."), resideInAPackage("..features.quote.."))
      .ignoreDependency(resideInAPackage("..features.quote.."), resideInAPackage("..features.valuation.."))
      .ignoreDependency(resideInAPackage("..features.ranking.."), resideInAPackage("..features.valuation.."))
      .ignoreDependency(resideInAPackage("..features.statistics.."), resideInAPackage("..features.valuation.."));
  ```

### RULE-C02: Ausencia de ciclos entre integraciones externas
* **Descripción:** Los adaptadores de integración externa (`footballmarket.integrations.*`) deben ser completamente independientes y estar libres de ciclos.
* **Condición ArchUnit:**
  ```java
  slices()
      .matching("footballmarket.integrations.(*)..")
      .should()
      .beFreeOfCycles();
  ```

### RULE-C03: Ausencia de ciclos entre paquetes comunes
* **Descripción:** Los módulos transversales comunes (`footballmarket.common.*`) deben estar libres de dependencias circulares entre sí.
* **Condición ArchUnit:**
  ```java
  slices()
      .matching("footballmarket.common.(*)..")
      .should()
      .beFreeOfCycles();
  ```

---

## 3. Reglas de Nomenclatura y Anotaciones (`NamingAndAnnotationRulesTest`)

### RULE-N01: Controladores con anotación y sufijo Controller
* **Descripción:** Las clases en paquetes `..presentation..` que estén anotadas con `@RestController` deben terminar con el sufijo `Controller`. A la inversa, toda clase con sufijo `Controller` debe residir en `..presentation..` y tener `@RestController`.
* **Condición ArchUnit:**
  ```java
  classes()
      .that().areAnnotatedWith(RestController.class)
      .should().haveSimpleNameEndingWith("Controller")
      .andShould().resideInAPackage("..presentation..");
  ```

### RULE-N02: DTOs implementados como records
* **Descripción:** Las clases que residen en paquetes `..dtos..` deben ser `record` de Java.
* **Condición ArchUnit:**
  ```java
  classes()
      .that().resideInAPackage("..dtos..")
      .should().beRecords();
  ```

### RULE-N03: Anotación @Repository restringida a interfaces Spring Data
* **Descripción:** La anotación `@Repository` solo puede colocarse en interfaces (repositorios Spring Data), nunca en clases concretas.
* **Condición ArchUnit:**
  ```java
  classes()
      .that().areAnnotatedWith(Repository.class)
      .should().beInterfaces();
  ```

---

## 4. Reglas de Excepciones y Mappers (`ExceptionAndMapperRulesTest`)

### RULE-E01: Jerarquía de excepciones propias
* **Descripción:** Toda excepción creada bajo el paquete raíz `footballmarket` debe heredar de `footballmarket.common.exceptions.FootballMarketException`.
* **Condición ArchUnit:**
  ```java
  classes()
      .that().areAssignableTo(Throwable.class)
      .and().resideInAPackage("footballmarket..")
      .should().beAssignableTo(FootballMarketException.class);
  ```

### RULE-M01: Mappers estáticos y finales
* **Descripción:** Las clases que residen en `..mappers..` o terminan en `Mapper` (excluyendo interfaces o mapeadores de terceros) deben ser `final`, no ser componentes Spring (`@Component`, `@Service`) y tener métodos estáticos.
* **Condición ArchUnit:**
  ```java
  classes()
      .that().haveSimpleNameEndingWith("Mapper")
      .and().resideInAPackage("footballmarket..")
      .should().beFinal()
      .andShould().notBeAnnotatedWith(Component.class);
  ```
