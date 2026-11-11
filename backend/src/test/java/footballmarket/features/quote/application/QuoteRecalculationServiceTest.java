package footballmarket.features.quote.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.exceptions.MarketNotInitializedException;
import footballmarket.features.quote.application.exceptions.ExecutionAlreadyExistsException;
import footballmarket.features.quote.application.exceptions.InvalidAdjustmentFactorException;
import footballmarket.features.quote.application.exceptions.UnknownQuoteStrategyException;
import footballmarket.features.quote.application.internal.QuotePlayerPublisher;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.enums.QuoteExecutionResult;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.models.PlayerQuote;
import footballmarket.features.quote.domain.models.QuoteAdjustment;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.quote.infraestructure.config.QuoteProperties;
import footballmarket.features.valuation.application.ValuationEvaluationService;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import footballmarket.support.ValuationTestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * Categoría Service del recálculo manual de cotizaciones.
 *
 * <p>Afirma exclusivamente lo que devuelve o lanza la API pública de {@link
 * QuoteRecalculationService}. No accede a la persistencia: el estado durable se comprueba en {@code
 * QuoteExecutionPersistenceTest}.
 */
@SpringBootTest(
    properties = {
      "market.quote.strategy=GENERAL_ATTRIBUTES",
      "market.quote.adjustment-factor=0.25",
      "market.quote.variation-cap=0.20",
      "market.quote.min-price-floor=7"
    })
@ActiveProfiles("test")
@Import({
  TestcontainersConfiguration.class,
  IdentityCatalogueFixture.class,
  ValuationTestData.class
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class QuoteRecalculationServiceTest {

  /** Fecha de mercado de las filas iniciales: dos días antes de la corrida. */
  private static final LocalDate MARKET_DATE = LocalDate.of(2026, 10, 17);

  /** Fecha de la campaña de métricas previa a la valuación. */
  private static final LocalDate CAMPAIGN_DATE = LocalDate.of(2026, 10, 18);

  /** Fecha de cotización de la ejecución: el día en que corre el recálculo. */
  private static final LocalDate QUOTE_DATE = LocalDate.of(2026, 10, 19);

  @Autowired private QuoteRecalculationService service;
  @Autowired private QuoteMarketInitializationService initialization;
  @Autowired private ValuationEvaluationService valuation;
  @Autowired private PlayerCatalogService playerCatalogue;
  @Autowired private QuotePlayerPublisher publisher;
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private ValuationTestData data;
  @Autowired private QuoteProperties properties;

  /**
   * Reloj de las campañas de estadísticas, fijado a la fecha de la corrida.
   *
   * <p>La valuación se sella con el día en que se evalúa y su fecha es la fecha de vigencia de la
   * fila recalculada, de modo que fijarla hace que el historial no dependa del día en que se
   * ejecuta la suite.
   */
  @TestBean(name = "statisticsClock", methodName = "valuationClock")
  private Clock statisticsClock;

  /**
   * Reloj de mercado, fijado a la fecha de cotización de la corrida.
   *
   * <p>La fecha de cotización de una ejecución es el día en que corre, de modo que fijarla mantiene
   * la corrida y su resumen estables entre ejecuciones de la suite.
   */
  @TestBean(name = "marketClock", methodName = "quoteClock")
  private Clock marketClock;

  static Clock valuationClock() {
    return Clock.fixed(Instant.parse("2026-10-19T09:00:00Z"), ZoneOffset.UTC);
  }

  static Clock quoteClock() {
    return Clock.fixed(Instant.parse("2026-10-19T09:00:00Z"), ZoneOffset.UTC);
  }

  @Nested
  @DisplayName("Publicación individual")
  class PlayerPublication {

    @Test
    @DisplayName("Publica una fila recalculada con score y estrategia")
    void publishesRecomputedQuote() {
      Player player = prepareValuedMarket();

      QuotePlayerPublisher.PublicationResult result =
          publisher.publish(
              player,
              QUOTE_DATE,
              ValuationStrategy.POSITIONAL_PERFORMANCE,
              new BigDecimal("0.10"),
              new BigDecimal("0.10"),
              1L,
              Instant.parse("2026-10-19T10:00:00Z"));

      PlayerQuote quote = result.quote().orElseThrow();
      assertThat(result.publication()).isEqualTo(QuotePlayerPublisher.Publication.RECOMPUTED);
      assertThat(quote.getOrigin()).isEqualTo(QuoteOrigin.RECOMPUTED);
      assertThat(quote.getScore()).isNotNull();
      assertThat(quote.getStrategyName()).isEqualTo(ValuationStrategy.POSITIONAL_PERFORMANCE);
      assertThat(quote.getStrategyVersion()).isEqualTo("v1");
    }

    @Test
    @DisplayName("Arrastra el precio sin score y hereda la estrategia vigente")
    void carriesForwardCurrentQuote() {
      Player player = prepareMarketWithoutValuations();

      QuotePlayerPublisher.PublicationResult result =
          publisher.publish(
              player,
              QUOTE_DATE,
              ValuationStrategy.POSITIONAL_PERFORMANCE,
              new BigDecimal("0.10"),
              new BigDecimal("0.10"),
              1L,
              Instant.parse("2026-10-19T10:00:00Z"));

      PlayerQuote quote = result.quote().orElseThrow();
      assertThat(result.publication()).isEqualTo(QuotePlayerPublisher.Publication.CARRIED_FORWARD);
      assertThat(quote.getScore()).isNull();
      assertThat(quote.getStrategyName()).isNull();
      assertThat(quote.getPrice()).isEqualTo(quote.getPreviousPrice());
    }

    @Test
    @DisplayName("No reprecia con una valuación no posterior a la referencia")
    void skipsOldValuation() {
      // Arrange: el mercado queda inicializado en la misma fecha que la valuación, de modo que la
      // valuación no es posterior a la fecha de vigencia de referencia del jugador.
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(CAMPAIGN_DATE);
      valuation.evaluate();
      initialization.initialize(QUOTE_DATE);
      Player player = firstPlayer();

      QuotePlayerPublisher.PublicationResult result =
          publisher.publish(
              player,
              QUOTE_DATE,
              ValuationStrategy.POSITIONAL_PERFORMANCE,
              new BigDecimal("0.10"),
              new BigDecimal("0.10"),
              1L,
              Instant.parse("2026-10-19T10:00:00Z"));

      assertThat(result.publication()).isEqualTo(QuotePlayerPublisher.Publication.NOT_REPRICED);
      assertThat(result.quote()).isEmpty();
    }

    @Test
    @DisplayName("Una fila arrastrada no impide publicar la recalculada de la misma fecha")
    void recomputedQuoteUpdatesCarriedForward() {
      Player player = prepareMarketWithoutValuations();
      QuotePlayerPublisher.PublicationResult carried =
          publisher.publish(
              player,
              QUOTE_DATE,
              ValuationStrategy.POSITIONAL_PERFORMANCE,
              new BigDecimal("0.10"),
              new BigDecimal("0.10"),
              1L,
              Instant.parse("2026-10-19T09:00:00Z"));
      valuation.evaluate();

      QuotePlayerPublisher.PublicationResult result =
          publisher.publish(
              player,
              QUOTE_DATE,
              ValuationStrategy.POSITIONAL_PERFORMANCE,
              new BigDecimal("0.10"),
              new BigDecimal("0.10"),
              1L,
              Instant.parse("2026-10-19T10:00:00Z"));

      assertThat(result.publication()).isEqualTo(QuotePlayerPublisher.Publication.RECOMPUTED);
      assertThat(result.quote().orElseThrow().getEffectiveDate()).isEqualTo(QUOTE_DATE);
      assertThat(result.quote().orElseThrow().getId())
          .isEqualTo(carried.quote().orElseThrow().getId());
      assertThat(result.quote().orElseThrow().getCalculatedAt())
          .isEqualTo(Instant.parse("2026-10-19T10:00:00Z"));
      assertThat(result.quote().orElseThrow().getStrategyName())
          .isEqualTo(ValuationStrategy.POSITIONAL_PERFORMANCE);
      assertThat(result.quote().orElseThrow().getScore()).isNotNull();
      PlayerQuote updated = result.quote().orElseThrow();
      QuoteAdjustment expected =
          QuoteAdjustment.calculate(
              100L, updated.getScore(), new BigDecimal("0.10"), new BigDecimal("0.10"), 1L);
      assertThat(updated.getPrice()).isEqualTo(expected.price());
      assertThat(updated.getPreviousPrice()).isEqualTo(100L);
      assertThat(updated.getMultiplier()).isEqualByComparingTo(expected.multiplier());
    }
  }

  @Nested
  @DisplayName("Ejecución poblacional")
  class PopulationExecution {

    @Test
    @DisplayName("La consulta relee origen y resultado derivados y devuelve vacío sin ejecución")
    void queriesPersistedOutcomeByDate() {
      // Arrange
      prepareValuedMarket();
      service.recalculateAutomatic();
      // Act
      List<QuoteExecution> saved = service.executions(QUOTE_DATE);
      List<QuoteExecution> absent = service.executions(QUOTE_DATE.minusDays(1));
      // Assert
      assertThat(saved)
          .singleElement()
          .satisfies(
              execution -> {
                assertThat(execution.getOrigin()).isEqualTo(QuoteExecutionOrigin.AUTOMATIC);
                assertThat(execution.result()).isEqualTo(QuoteExecutionResult.SUCCESS);
                assertThat(execution.failureReason()).isNull();
              });
      assertThat(absent).isEmpty();
    }

    @Test
    @DisplayName("La entrada automática persiste su origen y aplica la configuración del mercado")
    void automaticExecutionUsesMarketConfiguration() {
      // Arrange
      prepareValuedMarket();
      // Act
      QuoteExecution result = service.recalculateAutomatic();
      // Assert
      assertThat(result.getOrigin()).isEqualTo(QuoteExecutionOrigin.AUTOMATIC);
      assertThat(result.getStrategyName()).isEqualTo(ValuationStrategy.GENERAL_ATTRIBUTES);
      assertThat(result.getAdjustmentFactor()).isEqualByComparingTo("0.25");
      assertThat(result.getVariationCap()).isEqualByComparingTo("0.20");
      assertThat(result.getMinPriceFloor()).isEqualTo(7L);
      assertThat(result.getPricedPlayers()).isEqualTo(5);
      assertThat(result.getRecomputedRows()).isEqualTo(5);
      assertThat(result.getCarriedForwardRows()).isZero();
      assertThat(result.getNotRepricedPlayers()).isZero();
      assertThat(result.getFailedPlayers()).isZero();
      // Verify
      assertThat(service.executions(QUOTE_DATE))
          .singleElement()
          .satisfies(
              saved -> {
                assertThat(saved.getId()).isEqualTo(result.getId());
                assertThat(saved.getOrigin()).isEqualTo(QuoteExecutionOrigin.AUTOMATIC);
                assertThat(saved.getStrategyName()).isEqualTo(result.getStrategyName());
                assertThat(saved.getAdjustmentFactor())
                    .isEqualByComparingTo(result.getAdjustmentFactor());
                assertThat(saved.getVariationCap()).isEqualByComparingTo(result.getVariationCap());
                assertThat(saved.getMinPriceFloor()).isEqualTo(result.getMinPriceFloor());
              });
    }

    @Test
    @DisplayName("La entrada manual con los parámetros configurados conserva los mismos contadores")
    void manualExecutionUsesEquivalentInputs() {
      // Arrange
      prepareValuedMarket();
      // Act
      QuoteExecution result =
          service.recalculate(
              properties.strategy().name(),
              properties.adjustmentFactor(),
              properties.variationCap(),
              properties.minPriceFloor());
      // Assert
      assertThat(result.getOrigin()).isEqualTo(QuoteExecutionOrigin.MANUAL);
      assertThat(result.getStrategyName()).isEqualTo(ValuationStrategy.GENERAL_ATTRIBUTES);
      assertThat(result.getPricedPlayers()).isEqualTo(5);
      assertThat(result.getRecomputedRows()).isEqualTo(5);
      assertThat(result.getCarriedForwardRows()).isZero();
      assertThat(result.getNotRepricedPlayers()).isZero();
      assertThat(result.getFailedPlayers()).isZero();
    }

    @Test
    @DisplayName("Persiste independientemente varias corridas manuales del mismo día")
    void persistsRepeatedManualRuns() {
      prepareValuedMarket();

      QuoteExecution result = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);

      assertThat(result.getPricedPlayers()).isEqualTo(5);
      assertThat(result.getRecomputedRows()).isEqualTo(5);
      // El resumen se compara por identificador: la consulta lo relee de la base en otra
      // transacción y devuelve otra instancia de la misma fila.
      assertThat(service.executions(result.getQuoteDate()))
          .extracting(QuoteExecution::getId)
          .containsExactly(result.getId());
      QuoteExecution repeated = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      assertThat(repeated.getId()).isNotEqualTo(result.getId());
      assertThat(repeated.getOrigin()).isEqualTo(QuoteExecutionOrigin.MANUAL);
      assertThat(repeated.getFailedPlayers()).isZero();
      assertThat(service.executions(QUOTE_DATE))
          .extracting(QuoteExecution::getId)
          .containsExactlyInAnyOrder(result.getId(), repeated.getId());
    }

    @Test
    @DisplayName("Aplica la precedencia de estrategia, mercado y factor")
    void appliesRejectionPrecedence() {
      assertThatThrownBy(
              () -> service.recalculate("UNKNOWN", new BigDecimal("2.00"), BigDecimal.ZERO, 0L))
          .isInstanceOf(UnknownQuoteStrategyException.class);
      assertThatThrownBy(
              () ->
                  service.recalculate(
                      "POSITIONAL_PERFORMANCE", new BigDecimal("2.00"), BigDecimal.ZERO, 0L))
          .isInstanceOf(MarketNotInitializedException.class);

      prepareMarketWithoutValuations();

      assertThatThrownBy(
              () ->
                  service.recalculate(
                      "POSITIONAL_PERFORMANCE", new BigDecimal("2.00"), BigDecimal.ZERO, 0L))
          .isInstanceOf(InvalidAdjustmentFactorException.class);
    }

    @Test
    @DisplayName("Las manuales coexisten con una automática y otra automática se rechaza")
    void limitsOnlyAutomaticExecution() {
      // Arrange
      prepareValuedMarket();
      // Act
      QuoteExecution before = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      QuoteExecution automatic = service.recalculateAutomatic();
      QuoteExecution after = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      // Assert
      assertThatThrownBy(service::recalculateAutomatic)
          .isInstanceOf(ExecutionAlreadyExistsException.class);
      assertThat(service.executions(QUOTE_DATE))
          .extracting(QuoteExecution::getId)
          .containsExactlyInAnyOrder(before.getId(), automatic.getId(), after.getId());
      assertThat(service.executions(QUOTE_DATE))
          .extracting(QuoteExecution::getOrigin)
          .containsExactlyInAnyOrder(
              QuoteExecutionOrigin.MANUAL,
              QuoteExecutionOrigin.AUTOMATIC,
              QuoteExecutionOrigin.MANUAL);
    }
  }

  /**
   * Mercado con métricas valoradas y con el ancla inicial de dos días antes.
   *
   * <p>La valuación se sella en la fecha de la corrida y la fecha de vigencia de referencia del
   * jugador es la del ancla, de modo que la ejecución reprecia a toda la población.
   */
  private Player prepareValuedMarket() {
    catalogue.prepare();
    data.prepareConfirmedJsonCampaign(CAMPAIGN_DATE);
    valuation.evaluate();
    initialization.initialize(MARKET_DATE);
    return firstPlayer();
  }

  /** Mercado con métricas y con el ancla inicial, pero sin valuaciones publicadas. */
  private Player prepareMarketWithoutValuations() {
    catalogue.prepare();
    data.prepareConfirmedJsonCampaign(CAMPAIGN_DATE);
    initialization.initialize(MARKET_DATE);
    return firstPlayer();
  }

  private Player firstPlayer() {
    return playerCatalogue.getActivePlayers(0, 10).getContent().getFirst();
  }
}
