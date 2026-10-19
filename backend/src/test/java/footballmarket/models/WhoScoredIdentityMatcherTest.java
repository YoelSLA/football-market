package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.enums.PlayerIdentityItemResult;
import footballmarket.models.exceptions.InvalidPlayerException;
import footballmarket.models.records.WhoScoredIdentityContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@org.junit.jupiter.api.Tag("unit")
class WhoScoredIdentityMatcherTest {
  @Nested
  @DisplayName("Normalización exacta de identidad")
  class Normalization {
    @ParameterizedTest
    @CsvSource({
      "José—PÉREZ,jose perez",
      "Øzil,øzil",
      "Straße,straße",
      "O'Neil,o'neil",
      "J. Pérez,j. perez"
    })
    @DisplayName("Elimina diacríticos latinos y guiones sin inferir transliteraciones")
    void normalizesOnlySpecifiedCharacters(String input, String expected) {
      // Act / Assert
      assertThat(WhoScoredIdentityNormalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Conserva marcas no latinas y colapsa White Space Unicode")
    void preservesNonLatinMarks() {
      // Act / Assert
      assertThat(WhoScoredIdentityNormalizer.normalize("\u00a0A\t—\nB\u0085")).isEqualTo("a b");
      assertThat(WhoScoredIdentityNormalizer.normalize("क\u093f")).isEqualTo("क\u093f");
      assertThat(WhoScoredIdentityNormalizer.validName("---")).isFalse();
    }
  }

  @Nested
  @DisplayName("Coincidencia global por contexto completo")
  class Matching {
    @Test
    @DisplayName(
        "Una referencia inicial prevalece sobre atributos inválidos y varias referencias generan conflicto")
    void prioritizesInitialReferences() {
      // Arrange
      PlayerIdentitySyncItem referenced =
          new PlayerIdentitySyncItem(1L, 1L, null, null, null, 1L, "League", List.of("10"));
      PlayerIdentitySyncItem inconsistent =
          new PlayerIdentitySyncItem(1L, 2L, null, null, null, 1L, "League", List.of("10", "11"));
      PlayerIdentitySyncItem invalid =
          new PlayerIdentitySyncItem(1L, 3L, "---", null, "Team", 1L, "League", List.of());
      // Act
      Map<Long, WhoScoredIdentityMatcher.Decision> decisions =
          matcher.match(List.of(referenced, inconsistent, invalid), List.of(), Set.of(1L));
      // Assert
      assertThat(decisions.get(1L).result()).isEqualTo(PlayerIdentityItemResult.ALREADY_REFERENCED);
      assertThat(decisions.get(2L).reasonCode()).isEqualTo("MULTIPLE_INITIAL_REFERENCES");
      assertThat(decisions.get(3L).result()).isEqualTo(PlayerIdentityItemResult.LOCAL_DATA_ERROR);
    }

    @Test
    @DisplayName("Distingue homónimos por equipo y deduplica contextos del mismo identificador")
    void distinguishesHomonymsAndDeduplicatesContexts() {
      // Arrange
      PlayerIdentitySyncItem madrid = snapshot(1L, "Jose Perez", "Real Madrid CF");
      PlayerIdentitySyncItem barcelona = snapshot(2L, "Jose Perez", "FC Barcelona");
      WhoScoredIdentityCandidate candidate = candidate("101", "Jose Perez", "Real Madrid", 1L);
      candidate.observe(candidate.getContexts().getFirst());
      // Act
      Map<Long, WhoScoredIdentityMatcher.Decision> decisions =
          matcher.match(List.of(madrid, barcelona), List.of(candidate), Set.of(1L));
      // Assert
      assertThat(decisions.get(1L).result()).isEqualTo(PlayerIdentityItemResult.RESOLVED);
      assertThat(decisions.get(2L).result()).isEqualTo(PlayerIdentityItemResult.NO_MATCH);
      assertThat(candidate.getContexts()).hasSize(1);
    }

    @Test
    @DisplayName("Una variante de nombre puede coincidir sin combinar campos entre contextos")
    void matchesAnObservedNameVariant() {
      // Arrange
      PlayerIdentitySyncItem item = snapshot(1L, "Jose Perez", "Real Madrid CF");
      WhoScoredIdentityCandidate candidate = candidate("101", "Other Name", "Barcelona", 1L);
      candidate.observe(
          new WhoScoredIdentityContext(
              "Jose Perez", null, "Real Madrid", 4, null, 1L, "League", null, null));
      // Act
      WhoScoredIdentityMatcher.Decision decision =
          matcher.match(List.of(item), List.of(candidate), Set.of(1L)).get(1L);
      // Assert
      assertThat(decision.result()).isEqualTo(PlayerIdentityItemResult.RESOLVED);
      assertThat(decision.externalId()).isEqualTo("101");
    }

    private final WhoScoredIdentityMatcher matcher =
        new WhoScoredIdentityMatcher(WhoScoredTeamAliases.normative());

    @Test
    @DisplayName(
        "Dos identificadores en ligas diferentes producen ambigüedad sin desempate por orden")
    void rejectsGlobalAmbiguity() {
      // Arrange
      PlayerIdentitySyncItem item = snapshot(1L, "José Pérez", "Real Madrid CF");
      WhoScoredIdentityCandidate first = candidate("101", "Jose Perez", "Real Madrid", 1L);
      WhoScoredIdentityCandidate second = candidate("102", "José Pérez", "Real Madrid", 2L);
      // Act
      Map<Long, WhoScoredIdentityMatcher.Decision> decisions =
          matcher.match(List.of(item), List.of(first, second), Set.of(1L, 2L));
      Map<Long, WhoScoredIdentityMatcher.Decision> reversed =
          matcher.match(List.of(item), List.of(second, first), Set.of(1L, 2L));
      // Assert
      assertThat(decisions.get(1L).result()).isEqualTo(PlayerIdentityItemResult.AMBIGUOUS);
      assertThat(reversed).isEqualTo(decisions);
    }

    @Test
    @DisplayName(
        "No combina el nombre de un contexto con el equipo de otro ni asocia solo por nombre")
    void requiresOneCompleteContext() {
      // Arrange
      PlayerIdentitySyncItem item = snapshot(1L, "José Pérez", "Real Madrid CF");
      WhoScoredIdentityCandidate candidate = candidate("101", "Jose Perez", "Barcelona", 1L);
      candidate.observe(
          new WhoScoredIdentityContext(
              "Other", null, "Real Madrid", 4, null, 1L, "League", null, null));
      // Act
      WhoScoredIdentityMatcher.Decision decision =
          matcher.match(List.of(item), List.of(candidate), Set.of(1L)).get(1L);
      // Assert
      assertThat(decision.result()).isEqualTo(PlayerIdentityItemResult.NO_MATCH);
      assertThat(decision.candidates()).containsExactly(candidate);
    }

    @Test
    @DisplayName("Un candidato para varios jugadores locales genera conflicto para todos")
    void rejectsMultipleLocalOwners() {
      // Arrange
      PlayerIdentitySyncItem first = snapshot(1L, "Jose Perez", "Real Madrid CF");
      PlayerIdentitySyncItem second = snapshot(2L, "Jose Perez", "Real Madrid CF");
      WhoScoredIdentityCandidate candidate = candidate("101", "Jose Perez", "Real Madrid", 1L);
      // Act
      Map<Long, WhoScoredIdentityMatcher.Decision> decisions =
          matcher.match(List.of(first, second), List.of(candidate), Set.of(1L));
      // Assert
      assertThat(decisions.values())
          .allSatisfy(
              decision ->
                  assertThat(decision.result()).isEqualTo(PlayerIdentityItemResult.CONFLICT));
    }

    @Test
    @DisplayName("Rechaza colisiones inversas normalizadas del catálogo cerrado")
    void rejectsAliasCollisions() {
      // Arrange
      List<WhoScoredTeamAliases.Alias> aliases =
          List.of(
              new WhoScoredTeamAliases.Alias("Local A", "José"),
              new WhoScoredTeamAliases.Alias("Local B", "JOSE"));
      // Act / Assert
      assertThatThrownBy(() -> new WhoScoredTeamAliases(aliases))
          .isInstanceOf(InvalidPlayerException.class);
    }
  }

  private static PlayerIdentitySyncItem snapshot(Long playerId, String name, String team) {
    return new PlayerIdentitySyncItem(1L, playerId, name, 1L, team, 1L, "League", List.of());
  }

  private static WhoScoredIdentityCandidate candidate(
      String id, String name, String team, Long leagueId) {
    WhoScoredIdentityCandidate candidate = new WhoScoredIdentityCandidate(1L, id);
    candidate.observe(
        new WhoScoredIdentityContext(name, null, team, 4, null, leagueId, "League", null, null));
    return candidate;
  }
}
