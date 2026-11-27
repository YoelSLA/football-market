package footballmarket.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

@Tag("unit")
@AnalyzeClasses(packages = "footballmarket", importOptions = ImportOption.DoNotIncludeTests.class)
@DisplayName("Reglas de modularidad y ciclos entre componentes modulares")
public class FeatureCyclesTest {

  @ArchTest
  static final ArchRule featuresLibresDeCiclos =
      slices()
          .matching("footballmarket.features.(*)..")
          .should()
          .beFreeOfCycles()
          // Acoplamientos cíclicos heredados documentados en TD-041 (deuda técnica de modularidad)
          .ignoreDependency(
              resideInAPackage("..features.auth.."), resideInAPackage("..features.market.."))
          .ignoreDependency(
              resideInAPackage("..features.businessmetrics.."),
              resideInAPackage("..features.market.."))
          .ignoreDependency(
              resideInAPackage("..features.catalog.."), resideInAPackage("..features.identity.."))
          .ignoreDependency(
              resideInAPackage("..features.catalog.."), resideInAPackage("..features.images.."))
          .ignoreDependency(
              resideInAPackage("..features.catalog.."), resideInAPackage("..features.market.."))
          .ignoreDependency(
              resideInAPackage("..features.catalog.."), resideInAPackage("..features.statistics.."))
          .ignoreDependency(
              resideInAPackage("..features.market.."), resideInAPackage("..features.portfolio.."))
          .ignoreDependency(
              resideInAPackage("..features.market.."), resideInAPackage("..features.quote.."))
          .ignoreDependency(
              resideInAPackage("..features.quote.."), resideInAPackage("..features.valuation.."))
          .ignoreDependency(
              resideInAPackage("..features.ranking.."), resideInAPackage("..features.valuation.."))
          .ignoreDependency(
              resideInAPackage("..features.statistics.."),
              resideInAPackage("..features.valuation.."))
          .as("Los paquetes de features modulares deben estar libres de dependencias cíclicas")
          .because(
              "Los ciclos entre features dificultan el mantenimiento y la modularidad (salvo acoplamientos legados en TD-041)");

  @ArchTest
  static final ArchRule integracionesLibresDeCiclos =
      slices()
          .matching("footballmarket.integrations.(*)..")
          .should()
          .beFreeOfCycles()
          .as("Los adaptadores de integración externa deben estar libres de dependencias cíclicas")
          .because("Cada adaptador externo debe ser independiente de los demás");

  @ArchTest
  static final ArchRule commonLibreDeCiclos =
      slices()
          .matching("footballmarket.common.(*)..")
          .should()
          .beFreeOfCycles()
          .as("Los paquetes transversales comunes deben estar libres de dependencias cíclicas")
          .because("Los módulos comunes no deben tener acoplamientos circulares");
}
