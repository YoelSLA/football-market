package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.models.PlayerIdentityMatcher.Candidate;
import footballmarket.models.records.PlayerIdentityAliases;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerIdentityMatcherTest {
  private final PlayerIdentityMatcher matcher =
      new PlayerIdentityMatcher(PlayerIdentityAliases.controlled());

  @Nested
  @DisplayName("Coincidencia conservadora de identidad")
  class Matching {
    @Test
    @DisplayName("Admite nombre alternativo y sufijo de club cuando la fecha es opcional")
    void acceptsAlternateName() {
      Player player = new Player("José-Pérez", "Atlético FC", "League", "Forward");
      Candidate candidate = new Candidate("Other", "JOSE PEREZ", "Atletico", "Soccer", null, null);
      assertThat(matcher.matches(player, candidate)).isTrue();
    }

    @Test
    @DisplayName("Descarta deporte ausente o distinto de Soccer")
    void requiresSoccer() {
      Player player = new Player("Name", "Team", "League", "Forward");
      Candidate noSport = new Candidate("Name", null, "Team", null, null, null);
      Candidate otherSport = new Candidate("Name", null, "Team", "Basketball", null, null);
      assertThat(matcher.matches(player, noSport)).isFalse();
      assertThat(matcher.matches(player, otherSport)).isFalse();
    }

    @Test
    @DisplayName("Rechaza fecha contradictoria aun cuando coincide el club")
    void rejectsContradictoryDate() {
      Player player = new Player("Name", "Team", "League", "Forward");
      player.updateOptionalDetails(LocalDate.of(1990, 1, 1), "Spain");
      Candidate candidate =
          new Candidate("Name", null, "Team", "Soccer", LocalDate.of(1990, 1, 2), "España");
      assertThat(matcher.matches(player, candidate)).isFalse();
    }

    @Test
    @DisplayName("Equipo distinto exige fecha y nacionalidad exactas, con alias controlados")
    void acceptsDifferentTeamOnlyWithStrongIdentity() {
      Player player = new Player("Name", "Old Club", "League", "Forward");
      player.updateOptionalDetails(LocalDate.of(1990, 1, 1), "USA");
      Candidate valid =
          new Candidate(
              "Name", null, "New Club", "Soccer", LocalDate.of(1990, 1, 1), "United States");
      Candidate missing = new Candidate("Name", null, "New Club", "Soccer", null, "United States");
      assertThat(matcher.matches(player, valid)).isTrue();
      assertThat(matcher.matches(player, missing)).isFalse();
    }

    @Test
    @DisplayName("No decide por primer candidato ni por relevancia cuando hay varios válidos")
    void requiresExactlyOneMatch() {
      Player player = new Player("Name", "Team", "League", "Forward");
      Candidate first = new Candidate("Name", null, "Team", "Soccer", null, null);
      Candidate second = new Candidate("Name", null, "Team", "Soccer", null, null);
      assertThat(matcher.uniqueMatch(player, List.of())).isEmpty();
      assertThat(matcher.uniqueMatch(player, List.of(first))).contains(first);
      assertThat(matcher.uniqueMatch(player, List.of(first, second))).isEmpty();
    }
  }
}
