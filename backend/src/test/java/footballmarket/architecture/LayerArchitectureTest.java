package footballmarket.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

@Tag("unit")
@AnalyzeClasses(packages = "footballmarket", importOptions = ImportOption.DoNotIncludeTests.class)
@DisplayName("Reglas de arquitectura de capas y dependencias de roles")
public class LayerArchitectureTest {

  @ArchTest
  static final ArchRule controladoresNoDebenAccederARepositoriosNiInfraestructura =
      noClasses()
          .that()
          .resideInAPackage("..presentation..")
          .and()
          .haveSimpleNameEndingWith("Controller")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..persistence..", "..infrastructure..", "..infraestructure..")
          .as(
              "Los controladores no deben acceder directamente a clases de persistencia o"
                  + " infraestructura")
          .because(
              "Los controladores deben delegar la lógica de negocio y el acceso a datos en la capa"
                  + " de aplicación (Services/Orchestrators)");

  @ArchTest
  static final ArchRule presentacionNoDebeAccederARepositorios =
      noClasses()
          .that()
          .resideInAPackage("..presentation..")
          .should()
          .dependOnClassesThat()
          .haveSimpleNameEndingWith("Repository")
          .as(
              "Ninguna clase del paquete de presentación debe depender directamente de repositorios")
          .because("El acceso a persistencia corresponde exclusivamente a la capa de aplicación");

  @ArchTest
  static final ArchRule dominioNoDebeDependerDePresentacionNiWeb =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..presentation..", "org.springframework.web..")
          .as("El dominio no debe depender de la capa de presentación ni de frameworks web")
          .because(
              "El modelo de dominio contiene conceptos, estado e invariantes de negocio y debe"
                  + " permanecer desacoplado de la tecnología web");

  @ArchTest
  static final ArchRule orquestadoresNoDebenAccederARepositoriosIntegracionesNiOtrosOrquestadores =
      noClasses()
          .that()
          .resideInAPackage("footballmarket.orchestrators..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..persistence..",
              "..infrastructure..",
              "..infraestructure..",
              "footballmarket.integrations..",
              "footballmarket.orchestrators..")
          .allowEmptyShould(true)
          .as(
              "Los orquestadores no deben depender de repositorios, adaptadores de integración ni de"
                  + " otros orquestadores")
          .because(
              "Los orquestadores se limitan exclusivamente a coordinar servicios y transportar"
                  + " modelos opacos");
}
