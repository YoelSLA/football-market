package footballmarket.features.market.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.infraestructure.projections.OfficialMarketPlayerProjection;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.models.PlayerQuote;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.quote.infraestructure.PlayerQuoteRepository;
import footballmarket.features.quote.infraestructure.QuoteExecutionRepository;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import footballmarket.support.MarketTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
@Transactional
class OfficialMarketQuoteEquivalenceTest {

  @Autowired private OfficialMarketRepository officialMarketRepository;
  @Autowired private QuoteExecutionRepository quoteExecutionRepository;
  @Autowired private PlayerQuoteRepository playerQuoteRepository;
  @Autowired private MarketTestData testData;

  @Nested
  @DisplayName("Equivalencia canónica con PlayerQuoteRepository")
  class CanonicalEquivalence {

    @Test
    @DisplayName("Coincidencia exacta para cotización inicial única")
    void matchesInitialQuote() {
      // Arrange
      Player player = testData.createVisiblePlayer();
      testData.initializeMarket(player);

      // Act
      List<PlayerQuote> quoteDomainList =
          playerQuoteRepository.findLatestByPlayerId(player.getId());
      List<OfficialMarketPlayerProjection> marketProjections =
          officialMarketRepository.findOfficialMarketPlayers(
              player.getName(), null, null, null, null, null, null, null, "id", "asc", 10, 0);

      // Assert
      assertThat(quoteDomainList).isNotEmpty();
      PlayerQuote latestQuote = quoteDomainList.get(0);

      assertThat(marketProjections).hasSize(1);
      OfficialMarketPlayerProjection projection = marketProjections.get(0);

      assertThat(projection.getQuoteId()).isEqualTo(latestQuote.getId());
      assertThat(projection.getPrice()).isEqualTo(latestQuote.getPrice());
      assertThat(projection.getPreviousPrice()).isNull();
      assertThat(projection.getOrigin()).isEqualTo("INITIAL");
      assertThat(projection.getEffectiveDate()).isEqualTo(latestQuote.getEffectiveDate());
    }

    @Test
    @DisplayName("Prevalencia de cotización RECOMPUTED sobre arrastrada a igual fecha de vigencia")
    void matchesRecomputedOverCarriedForwardOnSameDate() {
      // Arrange
      Player player = testData.createVisiblePlayer();
      LocalDate date = LocalDate.of(2026, 11, 10);
      Instant instant1 = Instant.parse("2026-11-10T08:00:00Z");
      Instant instant2 = Instant.parse("2026-11-10T14:00:00Z");
      QuoteExecution execution1 = execution(date, instant1, 0, 1);
      QuoteExecution execution2 = execution(date, instant2, 1, 0);

      // Initial
      playerQuoteRepository.save(
          new PlayerQuote(
              player,
              LocalDate.of(2026, 11, 1),
              100L,
              null,
              BigDecimal.ONE,
              null,
              null,
              null,
              null,
              QuoteOrigin.INITIAL,
              Instant.parse("2026-11-01T08:00:00Z")));

      // Carried forward at date
      PlayerQuote carriedForward =
          playerQuoteRepository.save(
              new PlayerQuote(
                  player,
                  date,
                  100L,
                  100L,
                  BigDecimal.ONE,
                  null,
                  null,
                  null,
                  null,
                  QuoteOrigin.CARRIED_FORWARD,
                  instant1,
                  execution1));

      // Recomputed at date, replacing the carried-forward daily row
      PlayerQuote recomputed =
          new PlayerQuote(
              player,
              date,
              150L,
              100L,
              new BigDecimal("1.500000"),
              new BigDecimal("8.500000"),
              ValuationStrategy.POSITIONAL_PERFORMANCE,
              "v1",
              date,
              QuoteOrigin.RECOMPUTED,
              instant2,
              execution2);
      carriedForward.replaceCalculation(recomputed);
      recomputed = playerQuoteRepository.saveAndFlush(carriedForward);

      // Act
      List<PlayerQuote> quoteDomainList =
          playerQuoteRepository.findLatestByPlayerId(player.getId());
      List<OfficialMarketPlayerProjection> marketProjections =
          officialMarketRepository.findOfficialMarketPlayers(
              player.getName(), null, null, null, null, null, null, null, "id", "asc", 10, 0);

      // Assert
      assertThat(quoteDomainList).isNotEmpty();
      PlayerQuote canonicalLatest = quoteDomainList.get(0);
      assertThat(canonicalLatest.getId()).isEqualTo(recomputed.getId());

      assertThat(marketProjections).hasSize(1);
      OfficialMarketPlayerProjection projection = marketProjections.get(0);

      assertThat(projection.getQuoteId()).isEqualTo(canonicalLatest.getId());
      assertThat(projection.getPrice()).isEqualTo(150L);
      assertThat(projection.getPreviousPrice()).isEqualTo(100L);
      assertThat(projection.getOrigin()).isEqualTo("RECOMPUTED");
      assertThat(projection.getEffectiveDate()).isEqualTo(date);
    }

    @Test
    @DisplayName("Coincidencia exacta para múltiples jugadores con historiales dispares")
    void matchesAcrossMultiplePlayersWithDifferentHistories() {
      // Arrange
      Player player1 = testData.createVisiblePlayer();
      testData.initializeMarket(player1);

      Player player2 = testData.createVisiblePlayer();
      LocalDate initialDate = LocalDate.of(2026, 11, 1);
      Instant initialInstant = Instant.parse("2026-11-01T08:00:00Z");
      playerQuoteRepository.save(
          new PlayerQuote(
              player2,
              initialDate,
              200L,
              null,
              BigDecimal.ONE,
              null,
              null,
              null,
              null,
              QuoteOrigin.INITIAL,
              initialInstant));
      LocalDate recomputationDate = LocalDate.of(2026, 11, 11);
      Instant recomputationInstant = Instant.parse("2026-11-11T10:00:00Z");
      QuoteExecution execution = execution(recomputationDate, recomputationInstant, 1, 0);
      PlayerQuote player2Latest =
          playerQuoteRepository.save(
              new PlayerQuote(
                  player2,
                  recomputationDate,
                  300L,
                  200L,
                  new BigDecimal("1.500000"),
                  new BigDecimal("9.000000"),
                  ValuationStrategy.POSITIONAL_PERFORMANCE,
                  "v1",
                  recomputationDate,
                  QuoteOrigin.RECOMPUTED,
                  recomputationInstant,
                  execution));

      // Act & Assert
      List<PlayerQuote> p1Canonical = playerQuoteRepository.findLatestByPlayerId(player1.getId());
      List<PlayerQuote> p2Canonical = playerQuoteRepository.findLatestByPlayerId(player2.getId());

      List<OfficialMarketPlayerProjection> marketP1 =
          officialMarketRepository.findOfficialMarketPlayers(
              player1.getName(), null, null, null, null, null, null, null, "id", "asc", 10, 0);
      List<OfficialMarketPlayerProjection> marketP2 =
          officialMarketRepository.findOfficialMarketPlayers(
              player2.getName(), null, null, null, null, null, null, null, "id", "asc", 10, 0);

      assertThat(marketP1.get(0).getQuoteId()).isEqualTo(p1Canonical.get(0).getId());
      assertThat(marketP2.get(0).getQuoteId()).isEqualTo(p2Canonical.get(0).getId());
      assertThat(marketP2.get(0).getQuoteId()).isEqualTo(player2Latest.getId());
    }

    private QuoteExecution execution(
        LocalDate date, Instant instant, int recomputedRows, int carriedForwardRows) {
      return quoteExecutionRepository.save(
          new QuoteExecution(
              date,
              QuoteExecutionOrigin.MANUAL,
              ValuationStrategy.POSITIONAL_PERFORMANCE,
              "v1",
              new BigDecimal("0.10"),
              new BigDecimal("0.10"),
              1L,
              recomputedRows + carriedForwardRows,
              recomputedRows,
              carriedForwardRows,
              0,
              0,
              instant,
              instant));
    }
  }
}
