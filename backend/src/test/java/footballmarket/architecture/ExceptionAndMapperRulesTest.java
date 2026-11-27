package footballmarket.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import footballmarket.common.exceptions.FootballMarketException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Tag("unit")
@AnalyzeClasses(packages = "footballmarket", importOptions = ImportOption.DoNotIncludeTests.class)
@DisplayName("Reglas de jerarquía de excepciones y diseño de mappers")
public class ExceptionAndMapperRulesTest {

  @ArchTest
  static final ArchRule excepcionesPropiasDebenHeredarDeFootballMarketException =
      classes()
          .that()
          .areAssignableTo(Throwable.class)
          .and()
          .resideInAPackage("footballmarket..")
          .should()
          .beAssignableTo(FootballMarketException.class)
          .as(
              "Todas las excepciones propias del backend deben heredar directa o indirectamente de"
                  + " FootballMarketException")
          .because(
              "Permite clasificar los errores uniformemente en DomainException,"
                  + " ApplicationException, PersistenceException o IntegrationException");

  @ArchTest
  static final ArchRule mappersDebenSerFinalesYNoBeansSpring =
      classes()
          .that()
          .haveSimpleNameEndingWith("Mapper")
          .and()
          .resideInAPackage("footballmarket..")
          .should()
          .haveModifier(JavaModifier.FINAL)
          .andShould()
          .notBeAnnotatedWith(Component.class)
          .andShould()
          .notBeAnnotatedWith(Service.class)
          .as(
              "Las clases Mapper deben ser finales y no deben ser administradas como componentes de"
                  + " Spring")
          .because(
              "Las convenciones del backend establecen que los mappers son clases utilitarias"
                  + " estáticas sin estado mutable ni instancias inyectadas");
}
