package footballmarket.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

@Tag("unit")
@AnalyzeClasses(packages = "footballmarket", importOptions = ImportOption.DoNotIncludeTests.class)
@DisplayName("Reglas de nomenclatura y uso de anotaciones")
public class NamingAndAnnotationRulesTest {

  @ArchTest
  static final ArchRule controladoresDebenTenerAnotacionYSufijo =
      classes()
          .that()
          .areAnnotatedWith(RestController.class)
          .should()
          .haveSimpleNameEndingWith("Controller")
          .andShould()
          .resideInAPackage("..presentation..")
          .as(
              "Las clases con @RestController deben residir en paquetes de presentación y terminar"
                  + " con el sufijo Controller")
          .because("Asegura la coherencia y predictibilidad de los endpoints HTTP");

  @ArchTest
  static final ArchRule clasesConSufijoControllerDebenEstarAnotadasConRestController =
      classes()
          .that()
          .haveSimpleNameEndingWith("Controller")
          .and()
          .resideInAPackage("footballmarket..")
          .should()
          .beAnnotatedWith(RestController.class)
          .as("Las clases con sufijo Controller deben estar anotadas con @RestController")
          .because("Todos los controladores del backend exponen endpoints REST");

  @ArchTest
  static final ArchRule dtosDebenSerRecords =
      classes()
          .that()
          .haveSimpleNameEndingWith("DTO")
          .and()
          .resideInAPackage("footballmarket..")
          .should()
          .beRecords()
          .as("Los objetos de transferencia de datos (DTO) deben implementarse como records")
          .because(
              "Las convenciones del backend exigen que los DTOs de contrato HTTP sean inmutables y"
                  + " modelados como record");

  @ArchTest
  static final ArchRule anotacionRepositorySoloEnInterfaces =
      classes()
          .that()
          .areAnnotatedWith(Repository.class)
          .should()
          .beInterfaces()
          .as(
              "La anotación @Repository solo puede colocarse en interfaces de persistencia Spring"
                  + " Data")
          .because(
              "La arquitectura prohíbe @Repository en clases de servicio, puertos o repositorios"
                  + " manuales");
}
