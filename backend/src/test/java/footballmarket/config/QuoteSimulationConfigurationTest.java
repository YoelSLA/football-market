package footballmarket.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.integrations.QuoteSource;
import footballmarket.integrations.SimulatedQuoteSource;
import footballmarket.integrations.exceptions.MissingQuoteSourceException;
import footballmarket.integrations.exceptions.QuoteBatchUnavailableException;
import footballmarket.models.records.CurrentQuote;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * Selección de la fuente de cotización en el arranque.
 *
 * <p>Contexto reducido con {@link ApplicationContextRunner}: no usa Spring completo, ni base de
 * datos, ni el perfil {@code test}, porque son propiedades explícitas conforme a {@code
 * docs/backend/testing.md} §2 y §11.
 *
 * <p>Las pruebas originales registran la fuente alternativa con {@code withBean}. La matriz H07
 * utiliza configuraciones {@code @TestConfiguration} de usuario y carga el simulador en la fase de
 * auto-configuración, incorporándolos al runner en ambos órdenes. Las configuraciones auxiliares
 * quedan excluidas del component scanning de las demás pruebas.
 *
 * <p>Un último grupo comprueba el cableado de la propiedad en la configuración base, que es lo que
 * decide si un entorno de ejecución dispone o no de una fuente y, por tanto, si la aplicación
 * arranca.
 */
@org.junit.jupiter.api.Tag("lightweight")
class QuoteSimulationConfigurationTest {

  /**
   * Resolvedor de los marcadores de posición de la configuración base.
   *
   * <p>El carácter de escape es {@code null} a propósito: en esa forma el resolvedor no procesa
   * secuencias de escape y devuelve el valor por defecto cuando la variable no está definida, que
   * es justo lo que se verifica aquí. Los valores leídos son marcadores de entorno sin escapes.
   */
  private static final PropertyPlaceholderHelper PLACEHOLDER_HELPER =
      new PropertyPlaceholderHelper("${", "}", ":", null, true);

  private final ApplicationContextRunner context =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(QuoteSimulationConfiguration.class));

  /** Construye la fuente real hipotética que representa la integración definitiva de REQ-S2-03. */
  private static QuoteSource realQuoteSource() {
    return playerId -> Optional.of(new CurrentQuote(playerId, 500L, null, "real"));
  }

  /** Fuente sintética de caracterización: no es una integración productiva ni usa credenciales. */
  static final class AlternativeQuoteSource implements QuoteSource {
    @Override
    public Optional<CurrentQuote> currentQuote(Long playerId) {
      return Optional.of(
          new CurrentQuote(
              playerId, 500L, OffsetDateTime.parse("2026-10-14T10:30:00Z"), "alternative-test"));
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class AlternativeSourceConfiguration {
    @Bean
    AlternativeQuoteSource alternativeQuoteSource() {
      return new AlternativeQuoteSource();
    }
  }

  /** Consumidor con inyección obligatoria: no selecciona fuentes ni usa ObjectProvider. */
  record QuoteSourceConsumer(QuoteSource source) {}

  @TestConfiguration(proxyBeanMethods = false)
  static class ConsumerConfiguration {
    @Bean
    @Lazy
    QuoteSourceConsumer quoteSourceConsumer(QuoteSource source) {
      return new QuoteSourceConsumer(source);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  static class DiscoveryConfiguration {}

  @Nested
  @DisplayName("Compatibilidad de la extensión masiva")
  class BatchCompatibility {
    @Test
    @DisplayName(
        "Una fuente que solo implementa la lectura individual rechaza el lote explícitamente")
    void fuenteSinSoporteMasivoRechazaElLote() {
      // Arrange
      QuoteSource alternative = new AlternativeQuoteSource();
      // Act / Assert
      assertThat(alternative.currentQuote(1L)).isPresent();
      assertThatThrownBy(() -> alternative.currentQuotes(java.util.Set.of(1L)))
          .isInstanceOf(QuoteBatchUnavailableException.class);
    }

    @Test
    @DisplayName("El simulador resuelve el lote sin delegar en la lectura individual")
    void elSimuladorResuelveElLote() {
      // Arrange
      QuoteSource simulator = new SimulatedQuoteSource();
      // Act
      java.util.Map<Long, CurrentQuote> batch = simulator.currentQuotes(java.util.Set.of(1L, 2L));
      // Assert
      assertThat(batch).containsOnlyKeys(1L, 2L);
    }
  }

  @Nested
  @DisplayName("H07: precedencia contractual con ambos órdenes de configuraciones")
  class ConfigurationRegistrationOrder {
    @ParameterizedTest(name = "Alternativa primero: {0}; simulador habilitado: {1}")
    @CsvSource({"true, true", "true, false", "false, true", "false, false"})
    @DisplayName("La alternativa debe ser la única fuente con independencia del orden de registro")
    void alternativaExcluyeSimuladorEnAmbosOrdenes(
        boolean alternativeFirst, boolean simulatorEnabled) {
      // Arrange: ambos órdenes de incorporación al runner usan las fases productivas correctas.
      // Boot debe procesar configuraciones de usuario antes de evaluar la auto-configuración.
      ApplicationContextRunner ordered =
          alternativeFirst
              ? new ApplicationContextRunner()
                  .withUserConfiguration(AlternativeSourceConfiguration.class)
                  .withConfiguration(AutoConfigurations.of(QuoteSimulationConfiguration.class))
              : new ApplicationContextRunner()
                  .withConfiguration(AutoConfigurations.of(QuoteSimulationConfiguration.class))
                  .withUserConfiguration(AlternativeSourceConfiguration.class);

      // Act / Assert: las mismas expectativas contractuales deben cumplirse en los cuatro casos.
      ordered
          .withUserConfiguration(ConsumerConfiguration.class)
          .withPropertyValues(
              QuoteSimulationConfiguration.ENABLED_PROPERTY + "=" + simulatorEnabled)
          .run(
              started -> {
                assertThat(started).hasNotFailed();
                assertThat(started.getBeansOfType(QuoteSource.class)).hasSize(1);
                assertThat(started).doesNotHaveBean(SimulatedQuoteSource.class);
                QuoteSource selected = started.getBean(QuoteSource.class);
                assertThat(selected).isExactlyInstanceOf(AlternativeQuoteSource.class);
                assertThat(selected).isSameAs(started.getBean("alternativeQuoteSource"));

                // La creación diferida solo permite inspeccionar primero los candidatos. Esta
                // llamada resuelve realmente la dependencia obligatoria del consumidor.
                QuoteSourceConsumer consumer = started.getBean(QuoteSourceConsumer.class);
                assertThat(consumer.source()).isSameAs(selected);
              });
    }

    @ParameterizedTest(name = "Simulador habilitado: {0}")
    @ValueSource(booleans = {true, false})
    @DisplayName(
        "Sin alternativa el simulador explícito resuelve al consumidor o el arranque falla")
    void sinAlternativaRespetaLaHabilitacionExplicita(boolean simulatorEnabled) {
      // Act / Assert: ambos escenarios deben conservar el comportamiento ya contratado.
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(QuoteSimulationConfiguration.class))
          .withUserConfiguration(ConsumerConfiguration.class)
          .withPropertyValues(
              QuoteSimulationConfiguration.ENABLED_PROPERTY + "=" + simulatorEnabled)
          .run(
              started -> {
                if (!simulatorEnabled) {
                  assertThat(started).hasFailed();
                  assertThat(started.getStartupFailure())
                      .isInstanceOf(MissingQuoteSourceException.class);
                  return;
                }
                assertThat(started).hasNotFailed();
                assertThat(started.getBeansOfType(QuoteSource.class)).hasSize(1);
                assertThat(started).doesNotHaveBean(AlternativeQuoteSource.class);
                QuoteSource selected = started.getBean(QuoteSource.class);
                assertThat(selected).isExactlyInstanceOf(SimulatedQuoteSource.class);
                assertThat(started.getBean(QuoteSourceConsumer.class).source()).isSameAs(selected);
              });
    }
  }

  @Nested
  @DisplayName("H07: discovery Boot y exclusión del escaneo ordinario")
  class AutoConfigurationDiscovery {
    @ParameterizedTest(name = "Fuente alternativa presente: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName(
        "EnableAutoConfiguration descubre el simulador mediante imports y respeta la alternativa")
    void descubreLaConfiguracionDesdeImports(boolean alternativePresent) {
      // Arrange: se usa el cargador de candidatos de Boot y su selector real, sin proporcionar
      // QuoteSimulationConfiguration al runner. Se excluyen las demás auto-configuraciones para
      // no iniciar infraestructura de DB, web o proveedores ajena a esta prueba de configuración.
      List<String> candidates =
          ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader())
              .getCandidates();
      String expected = QuoteSimulationConfiguration.class.getName();
      assertThat(candidates).contains(expected);
      String excluded =
          String.join(",", candidates.stream().filter(name -> !name.equals(expected)).toList());
      ApplicationContextRunner discovered =
          new ApplicationContextRunner()
              .withUserConfiguration(DiscoveryConfiguration.class, ConsumerConfiguration.class)
              .withPropertyValues(
                  "spring.autoconfigure.exclude=" + excluded,
                  QuoteSimulationConfiguration.ENABLED_PROPERTY + "=true");
      if (alternativePresent) {
        discovered = discovered.withUserConfiguration(AlternativeSourceConfiguration.class);
      }

      // Act / Assert
      discovered.run(
          started -> {
            assertThat(started).hasNotFailed();
            assertThat(started).hasSingleBean(QuoteSimulationConfiguration.class);
            assertThat(started.getBeansOfType(QuoteSource.class)).hasSize(1);
            QuoteSource selected = started.getBean(QuoteSource.class);
            if (alternativePresent) {
              assertThat(selected).isExactlyInstanceOf(AlternativeQuoteSource.class);
              assertThat(selected).isSameAs(started.getBean("alternativeQuoteSource"));
              assertThat(started).doesNotHaveBean(SimulatedQuoteSource.class);
            } else {
              assertThat(selected).isExactlyInstanceOf(SimulatedQuoteSource.class);
            }
            assertThat(started.getBean(QuoteSourceConsumer.class).source()).isSameAs(selected);
          });
    }

    @Test
    @DisplayName("El filtro de Spring Boot excluye la auto-configuración del escaneo ordinario")
    void excluyeLaConfiguracionDelComponentScanning() {
      // Arrange: mismo filtro de exclusión utilizado por SpringBootApplication.
      ClassPathScanningCandidateComponentProvider scanner =
          new ClassPathScanningCandidateComponentProvider(false);
      AutoConfigurationExcludeFilter autoConfigurationFilter = new AutoConfigurationExcludeFilter();
      autoConfigurationFilter.setBeanClassLoader(getClass().getClassLoader());
      scanner.addIncludeFilter(new AnnotationTypeFilter(Configuration.class));
      scanner.addExcludeFilter(autoConfigurationFilter);

      // Act / Assert: la clase solo debe entrar por discovery de auto-configuración.
      assertThat(QuoteSimulationConfiguration.class.isAnnotationPresent(AutoConfiguration.class))
          .isTrue();
      assertThat(scanner.findCandidateComponents("footballmarket.config"))
          .isNotEmpty()
          .extracting(definition -> definition.getBeanClassName())
          .doesNotContain(QuoteSimulationConfiguration.class.getName());
    }
  }

  @Nested
  @DisplayName("Cableado de la propiedad en la configuración base")
  class BaseConfigurationWiring {
    @Test
    @DisplayName("Sin la variable de entorno el simulador queda deshabilitado por defecto")
    void sinVariableDeEntornoElSimuladorQuedaDeshabilitado() throws IOException {
      // Arrange
      String declared = declaredSimulatorEnabled();

      // Assert: el valor por defecto es deshabilitado, de modo que ningún entorno arranca con
      // precios simulados sin haberlo pedido de forma explícita.
      assertThat(resolve(declared, new Properties())).isEqualTo("false");
    }

    @Test
    @DisplayName("La variable de entorno habilita el simulador y con ella llega a ser la fuente")
    void laVariableDeEntornoHabilitaElSimulador() throws IOException {
      // Arrange
      Properties environment = new Properties();
      environment.setProperty("MARKET_QUOTE_SIMULATOR_ENABLED", "true");

      // Act / Assert
      assertThat(resolve(declaredSimulatorEnabled(), environment)).isEqualTo("true");

      // La misma propiedad, ya resuelta, es la que la configuración lee para registrar el bean.
      context
          .withPropertyValues(QuoteSimulationConfiguration.ENABLED_PROPERTY + "=true")
          .run(
              started -> {
                assertThat(started).hasNotFailed();
                assertThat(started).hasSingleBean(QuoteSource.class);
                assertThat(started.getBean(QuoteSource.class))
                    .isInstanceOf(SimulatedQuoteSource.class);
              });
    }

    @Test
    @DisplayName("El precio por defecto es una centésima y la variable de entorno lo sustituye")
    void elPrecioPorDefectoEsUnaCentesima() throws IOException {
      // Assert
      assertThat(resolve(declaredSimulatorPrice(), new Properties())).isEqualTo("100");

      Properties environment = new Properties();
      environment.setProperty("MARKET_QUOTE_SIMULATOR_PRICE", "250");
      assertThat(resolve(declaredSimulatorPrice(), environment)).isEqualTo("250");
    }

    /**
     * Lee de `application.yml` el valor declarado, todavía sin resolver, para la propiedad dada.
     */
    private String declared(String property) throws IOException {
      List<PropertySource<?>> sources =
          new YamlPropertySourceLoader()
              .load("application", new ClassPathResource("application.yml"));
      assertThat(sources).as("application.yml debe seguir siendo legible").isNotEmpty();
      Object declared = sources.get(0).getProperty(property);
      assertThat(declared).as("application.yml debe declarar " + property).isNotNull();
      return String.valueOf(declared);
    }

    private String declaredSimulatorEnabled() throws IOException {
      return declared(QuoteSimulationConfiguration.ENABLED_PROPERTY);
    }

    private String declaredSimulatorPrice() throws IOException {
      return declared(QuoteSimulationConfiguration.PRICE_PROPERTY);
    }

    /** Resuelve el valor declarado aplicando las variables de entorno simuladas. */
    private String resolve(String declared, Properties environment) {
      return PLACEHOLDER_HELPER.replacePlaceholders(declared, environment);
    }
  }

  @Nested
  @DisplayName("Selección de fuente")
  class SourceSelection {
    @Test
    @DisplayName(
        "Con fuente real disponible la fuente real prevalece aunque el simulador esté habilitado")
    void laFuenteRealPrevaleceSobreElSimulador() {
      // Arrange / Act
      context
          .withBean(QuoteSource.class, QuoteSimulationConfigurationTest::realQuoteSource)
          .withPropertyValues(QuoteSimulationConfiguration.ENABLED_PROPERTY + "=true")
          .run(
              started -> {
                // Assert
                assertThat(started).hasNotFailed();
                assertThat(started).hasSingleBean(QuoteSource.class);
                assertThat(started.getBean(QuoteSource.class))
                    .isNotInstanceOf(SimulatedQuoteSource.class);
              });
    }

    @Test
    @DisplayName("Con fuente real el simulador no se registra aunque su propiedad esté habilitada")
    void elSimuladorNoSeRegistraConFuenteReal() {
      // Arrange / Act
      context
          .withBean(QuoteSource.class, QuoteSimulationConfigurationTest::realQuoteSource)
          .withPropertyValues(QuoteSimulationConfiguration.ENABLED_PROPERTY + "=true")
          .run(
              started -> {
                // Assert
                assertThat(started).doesNotHaveBean(SimulatedQuoteSource.class);
                assertThat(started.getBeansOfType(SimulatedQuoteSource.class)).isEmpty();
              });
    }
  }

  @Nested
  @DisplayName("Arranque sin fuente de cotización")
  class StartupWithoutSource {
    @Test
    @DisplayName(
        "Sin ninguna fuente y con el simulador deshabilitado el arranque falla de forma explícita")
    void fallaElArranqueSinFuenteNiSimulador() {
      // Arrange / Act
      context
          .withPropertyValues(QuoteSimulationConfiguration.ENABLED_PROPERTY + "=false")
          .run(
              started -> {
                // Assert
                assertThat(started).hasFailed();
                // La guardia lanza la excepción directamente, sin envolverla en otra causa. Por eso
                // se afirma el tipo exacto del fallo y no una causa anidada:
                // `hasRootCauseInstanceOf` exige una cadena de causas y no puede satisfacerse
                // cuando
                // la excepción no tiene causa, con independencia del tipo que se le pase. Esta
                // forma
                // además es más estricta, porque fija el tipo del propio fallo de arranque.
                assertThat(started.getStartupFailure())
                    .isInstanceOf(MissingQuoteSourceException.class);
              });
    }

    @Test
    @DisplayName("Sin ninguna fuente y sin propiedad explícita el arranque también falla")
    void fallaElArranqueSinPropiedadSimulador() {
      // Arrange / Act
      context.run(
          started -> {
            // Assert
            assertThat(started).hasFailed();
            // La guardia lanza la excepción directamente, sin envolverla en otra causa. Por eso
            // se afirma el tipo exacto del fallo y no una causa anidada: `hasRootCauseInstanceOf`
            // exige una cadena de causas y no puede satisfacerse cuando la excepción no tiene
            // causa,
            // con independencia del tipo que se le pase. Esta forma además es más estricta, porque
            // fija el tipo del propio fallo de arranque.
            assertThat(started.getStartupFailure()).isInstanceOf(MissingQuoteSourceException.class);
          });
    }
  }

  @Nested
  @DisplayName("Simulador habilitado")
  class SimulatorEnabled {
    @Test
    @DisplayName("Sin fuente real y con el simulador habilitado se registra como fuente activa")
    void registraElSimuladorComoFuenteActiva() {
      // Arrange / Act
      context
          .withPropertyValues(QuoteSimulationConfiguration.ENABLED_PROPERTY + "=true")
          .run(
              started -> {
                // Assert
                assertThat(started).hasNotFailed();
                assertThat(started).hasSingleBean(QuoteSource.class);
                assertThat(started.getBean(QuoteSource.class))
                    .isInstanceOf(SimulatedQuoteSource.class);
              });
    }

    @Test
    @DisplayName("El precio del simulador queda bound desde su propiedad")
    void elPrecioQuedaBoundDesdeLaPropiedad() {
      // Arrange / Act
      context
          .withPropertyValues(
              QuoteSimulationConfiguration.ENABLED_PROPERTY + "=true",
              QuoteSimulationConfiguration.PRICE_PROPERTY + "=350")
          .run(
              started -> {
                // Assert
                assertThat(started).hasNotFailed();
                assertThat(
                        started.getBean(QuoteSource.class).currentQuote(1L).orElseThrow().price())
                    .isEqualTo(350L);
              });
    }

    @Test
    @DisplayName(
        "Sin fuente real y con el simulador deshabilitado el arranque falla, sin registrar fuente")
    void sinFuenteNiSimuladorHabilitadoNoArranca() {
      // Arrange / Act
      context
          .withPropertyValues(QuoteSimulationConfiguration.ENABLED_PROPERTY + "=false")
          .run(
              started -> {
                // Assert
                // Sin fuente real y con el simulador deshabilitado, el contrato exige que la
                // aplicación no inicie: un contexto que llegara a arrancar en este estado sería
                // una configuración inválida aceptada. La falla por ausencia de fuente es lo que
                // demuestra además que el simulador no llegó a registrarse.
                assertThat(started).hasFailed();
                assertThat(started.getStartupFailure())
                    .isInstanceOf(MissingQuoteSourceException.class);
              });
    }
  }
}
