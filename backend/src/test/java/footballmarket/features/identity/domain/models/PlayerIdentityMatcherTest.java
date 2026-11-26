package footballmarket.features.identity.domain.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.identity.domain.models.PlayerIdentityMatcher.Candidate;
import footballmarket.features.identity.domain.records.PlayerIdentityAliases;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
class PlayerIdentityMatcherTest {
  private final PlayerIdentityMatcher matcher =
      new PlayerIdentityMatcher(PlayerIdentityAliases.controlled());

  @Nested
  @DisplayName("Coincidencia conservadora de identidad")
  class Matching {
    @Test
    @DisplayName("Admite nombre alternativo y sufijo de club cuando la fecha es opcional")
    void acceptsAlternateName() {
      League league = new League("League");
      Team team = new Team("Atlético FC", league, true);
      Player player = new Player("José-Pérez", team, "Forward");
      Candidate candidate = new Candidate("Other", "JOSE PEREZ", "Atletico", "Soccer", null, null);
      assertThat(matcher.matches(player, candidate)).isTrue();
    }

    @Test
    @DisplayName("Descarta deporte ausente o distinto de Soccer")
    void requiresSoccer() {
      League league = new League("League");
      Team team = new Team("Team", league, true);
      Player player = new Player("Name", team, "Forward");
      Candidate noSport = new Candidate("Name", null, "Team", null, null, null);
      Candidate otherSport = new Candidate("Name", null, "Team", "Basketball", null, null);
      assertThat(matcher.matches(player, noSport)).isFalse();
      assertThat(matcher.matches(player, otherSport)).isFalse();
    }

    @Test
    @DisplayName("Rechaza fecha contradictoria aun cuando coincide el club")
    void rejectsContradictoryDate() {
      League league = new League("League");
      Team team = new Team("Team", league, true);
      Player player = new Player("Name", team, "Forward");
      player.updateOptionalDetails(LocalDate.of(1990, 1, 1), "Spain");
      Candidate candidate =
          new Candidate("Name", null, "Team", "Soccer", LocalDate.of(1990, 1, 2), "España");
      assertThat(matcher.matches(player, candidate)).isFalse();
    }

    @Test
    @DisplayName("Equipo distinto exige fecha y nacionalidad exactas, con alias controlados")
    void acceptsDifferentTeamOnlyWithStrongIdentity() {
      League league = new League("League");
      Team team = new Team("Old Club", league, true);
      Player player = new Player("Name", team, "Forward");
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
      League league = new League("League");
      Team team = new Team("Team", league, true);
      Player player = new Player("Name", team, "Forward");
      Candidate first = new Candidate("Name", null, "Team", "Soccer", null, null);
      Candidate second = new Candidate("Name", null, "Team", "Soccer", null, null);
      assertThat(matcher.uniqueMatch(player, List.of())).isEmpty();
      assertThat(matcher.uniqueMatch(player, List.of(first))).contains(first);
      assertThat(matcher.uniqueMatch(player, List.of(first, second))).isEmpty();
    }

    @Test
    @DisplayName("Admite nombre compatible cuando club y fecha de nacimiento coinciden exactamente")
    void acceptsCompatibleNameWhenClubAndDateMatch() {
      League league = new League("Premier League");
      Team team = new Team("Arsenal FC", league, true);
      Player player = new Player("Gabriel Magalhães", team, "Defender");
      player.updateOptionalDetails(LocalDate.of(1997, 12, 19), "Brazil");

      Candidate candidate =
          new Candidate("Gabriel", null, "Arsenal", "Soccer", LocalDate.of(1997, 12, 19), "Brazil");
      assertThat(matcher.matches(player, candidate)).isTrue();
    }

    @Test
    @DisplayName("Admite equivalencia de nacionalidad The Netherlands con Netherlands")
    void acceptsTheNetherlandsAlias() {
      League league = new League("Premier League");
      Team team = new Team("Arsenal FC", league, true);
      Player player = new Player("Jurriën Timber", team, "Defender");
      player.updateOptionalDetails(LocalDate.of(2001, 6, 17), "Netherlands");

      Candidate candidate =
          new Candidate(
              "Jurrien Timber",
              null,
              "Arsenal",
              "Soccer",
              LocalDate.of(2001, 6, 17),
              "The Netherlands");
      assertThat(matcher.matches(player, candidate)).isTrue();
    }

    @Test
    @DisplayName("Un jugador sin equipo resuelto no existe como Player y por tanto no coincide")
    void refusesPlayerWithoutTeam() {
      // Arrange / Act / Assert: el Player definitivo no admite equipo nulo, de modo que un jugador
      // sin equipo resuelto no puede construirse y el matcher solo recibe jugadores asociados.
      assertThatThrownBy(() -> new Player("Name", (Team) null, "Forward"))
          .isInstanceOf(InvalidPlayerException.class);
    }

    @Test
    @DisplayName(
        "matchesRoster admite nombre exacto con discrepancia menor de fecha en el mismo club")
    void matchesRosterAcceptsMinorDateDiscrepancyForExactName() {
      League league = new League("Premier League");
      Team team = new Team("Arsenal FC", league, true);
      Player player = new Player("Ben White", team, "Defender");
      player.updateOptionalDetails(LocalDate.of(1997, 10, 8), "England");

      Candidate candidate =
          new Candidate(
              "Ben White", null, "Arsenal", "Soccer", LocalDate.of(1997, 11, 8), "England");
      assertThat(matcher.matchesRoster(player, candidate)).isTrue();
    }

    @Test
    @DisplayName("matchesRoster admite nombre exacto con nacionalidad secundaria en el mismo club")
    void matchesRosterAcceptsSecondaryNationalityForExactName() {
      League league = new League("Premier League");
      Team team = new Team("Chelsea FC", league, true);
      Player player = new Player("Geovany Quenda", team, "Forward");
      player.updateOptionalDetails(LocalDate.of(2007, 4, 30), "Guinea-Bissau");

      Candidate candidate =
          new Candidate(
              "Geovany Quenda", null, "Chelsea", "Soccer", LocalDate.of(2007, 4, 30), "Portugal");
      assertThat(matcher.matchesRoster(player, candidate)).isTrue();
    }

    @Test
    @DisplayName("matchesRoster descarta homónimo de diferente generación aun con nombre exacto")
    void matchesRosterRejectsDifferentGenerationEvenWithExactName() {
      League league = new League("League");
      Team team = new Team("Team", league, true);
      Player player = new Player("Name", team, "Forward");
      player.updateOptionalDetails(LocalDate.of(2005, 1, 1), "Spain");

      Candidate candidate =
          new Candidate("Name", null, "Team", "Soccer", LocalDate.of(1975, 1, 1), "Spain");
      assertThat(matcher.matchesRoster(player, candidate)).isFalse();
    }

    @Test
    @DisplayName("matchesRoster exige fecha exacta para nombres compatibles no exactos")
    void matchesRosterRequiresExactDateForCompatibleNames() {
      League league = new League("Premier League");
      Team team = new Team("Arsenal FC", league, true);
      Player player = new Player("Gabriel Magalhães", team, "Defender");
      player.updateOptionalDetails(LocalDate.of(1997, 12, 19), "Brazil");

      Candidate differentDateCandidate =
          new Candidate("Gabriel", null, "Arsenal", "Soccer", LocalDate.of(1997, 4, 3), "Brazil");
      Candidate exactDateCandidate =
          new Candidate("Gabriel", null, "Arsenal", "Soccer", LocalDate.of(1997, 12, 19), "Brazil");

      assertThat(matcher.matchesRoster(player, differentDateCandidate)).isFalse();
      assertThat(matcher.matchesRoster(player, exactDateCandidate)).isTrue();
    }

    @Test
    @DisplayName(
        "matchesRoster y matches normalizan prefijos y sufijos de clubes (AFC, AS, RC, Racing Club)")
    void normalizesClubAffixesCorrectly() {
      League league = new League("League");

      Team bournemouth = new Team("AFC Bournemouth", league, true);
      Player kluivert = new Player("Justin Kluivert", bournemouth, "Forward");
      kluivert.updateOptionalDetails(LocalDate.of(1999, 5, 5), "Netherlands");
      Candidate candBournemouth =
          new Candidate(
              "Justin Kluivert",
              null,
              "Bournemouth",
              "Soccer",
              LocalDate.of(1999, 5, 5),
              "Netherlands");
      assertThat(matcher.matches(kluivert, candBournemouth)).isTrue();
      assertThat(matcher.matchesRoster(kluivert, candBournemouth)).isTrue();

      Team monaco = new Team("AS Monaco FC", league, true);
      Player zakaria = new Player("Denis Zakaria", monaco, "Midfielder");
      zakaria.updateOptionalDetails(LocalDate.of(1996, 11, 20), "Switzerland");
      Candidate candMonaco =
          new Candidate(
              "Denis Zakaria", null, "Monaco", "Soccer", LocalDate.of(1996, 11, 20), "Switzerland");
      assertThat(matcher.matches(zakaria, candMonaco)).isTrue();
      assertThat(matcher.matchesRoster(zakaria, candMonaco)).isTrue();

      Team lens = new Team("Racing Club de Lens", league, true);
      Player sotoca = new Player("Florian Sotoca", lens, "Forward");
      sotoca.updateOptionalDetails(LocalDate.of(1990, 10, 25), "France");
      Candidate candLens =
          new Candidate(
              "Florian Sotoca", null, "Lens", "Soccer", LocalDate.of(1990, 10, 25), "France");
      assertThat(matcher.matches(sotoca, candLens)).isTrue();
      assertThat(matcher.matchesRoster(sotoca, candLens)).isTrue();

      Team strasbourg = new Team("RC Strasbourg Alsace", league, true);
      Player gameiro = new Player("Kévin Gameiro", strasbourg, "Forward");
      gameiro.updateOptionalDetails(LocalDate.of(1987, 5, 9), "France");
      Candidate candStrasbourg =
          new Candidate(
              "Kevin Gameiro", null, "Strasbourg", "Soccer", LocalDate.of(1987, 5, 9), "France");
      assertThat(matcher.matches(gameiro, candStrasbourg)).isTrue();
      assertThat(matcher.matchesRoster(gameiro, candStrasbourg)).isTrue();

      Team hamburg = new Team("Hamburger SV", league, true);
      Player fernandes = new Player("Daniel Fernandes", hamburg, "Goalkeeper");
      fernandes.updateOptionalDetails(LocalDate.of(1992, 11, 13), "Portugal");
      Candidate candHamburg =
          new Candidate(
              "Daniel Fernandes",
              null,
              "Hamburg",
              "Soccer",
              LocalDate.of(1992, 11, 13),
              "Portugal");
      assertThat(matcher.matchesRoster(fernandes, candHamburg)).isTrue();

      Team marseille = new Team("Olympique de Marseille", league, true);
      Player harit = new Player("Amine Harit", marseille, "Midfielder");
      harit.updateOptionalDetails(LocalDate.of(1997, 6, 18), "Morocco");
      Candidate candMarseille =
          new Candidate(
              "Amine Harit", null, "Marseille", "Soccer", LocalDate.of(1997, 6, 18), "Morocco");
      assertThat(matcher.matchesRoster(harit, candMarseille)).isTrue();

      Team angers = new Team("Angers SCO", league, true);
      Player arcus = new Player("Carlens Arcus", angers, "Defender");
      arcus.updateOptionalDetails(LocalDate.of(1996, 6, 28), "Haiti");
      Candidate candAngers =
          new Candidate(
              "Carlens Arcus", null, "Angers", "Soccer", LocalDate.of(1996, 6, 28), "Haiti");
      assertThat(matcher.matchesRoster(arcus, candAngers)).isTrue();

      Team rennes = new Team("Stade Rennais FC 1901", league, true);
      Player seidu = new Player("Alidu Seidu", rennes, "Defender");
      seidu.updateOptionalDetails(LocalDate.of(2000, 6, 4), "Ghana");
      Candidate candRennes =
          new Candidate("Alidu Seidu", null, "Rennes", "Soccer", LocalDate.of(2000, 6, 4), "Ghana");
      assertThat(matcher.matchesRoster(seidu, candRennes)).isTrue();

      Team psg = new Team("Paris Saint-Germain FC", league, true);
      Player hakimi = new Player("Achraf Hakimi", psg, "Defender");
      hakimi.updateOptionalDetails(LocalDate.of(1998, 11, 4), "Morocco");
      Candidate candPsg =
          new Candidate(
              "Achraf Hakimi", null, "Paris SG", "Soccer", LocalDate.of(1998, 11, 4), "Morocco");
      assertThat(matcher.matchesRoster(hakimi, candPsg)).isTrue();
    }

    @Test
    @DisplayName("Normaliza caracteres latinos no descomponibles (Ł, Ø, Đ) y alias de nacionalidad")
    void normalizesSpecialCharactersAndCountryAliases() {
      League league = new League("Serie A");

      Team bologna = new Team("Bologna FC 1909", league, true);
      Player skorupski = new Player("Łukasz Skorupski", bologna, "Goalkeeper");
      skorupski.updateOptionalDetails(LocalDate.of(1991, 5, 5), "Poland");
      Candidate candSkorupski =
          new Candidate(
              "Lukasz Skorupski", null, "Bologna", "Soccer", LocalDate.of(1991, 5, 5), "Poland");
      assertThat(matcher.matches(skorupski, candSkorupski)).isTrue();

      Team atalanta = new Team("Atalanta BC", league, true);
      Player kolasinac = new Player("Sead Kolašinac", atalanta, "Defender");
      kolasinac.updateOptionalDetails(LocalDate.of(1993, 6, 20), "Bosnia-Herzegovina");
      Candidate candKolasinac =
          new Candidate(
              "Sead Kolasinac",
              null,
              "Atalanta",
              "Soccer",
              LocalDate.of(1993, 6, 20),
              "Bosnia and Herzegovina");
      assertThat(matcher.matches(kolasinac, candKolasinac)).isTrue();

      League epl = new League("Premier League");
      Team fulham = new Team("Fulham FC", epl, true);
      Player cairney = new Player("Tom Cairney", fulham, "Midfielder");
      cairney.updateOptionalDetails(LocalDate.of(1991, 1, 20), "England");
      Candidate candCairney =
          new Candidate(
              "Tom Cairney", null, "Fulham", "Soccer", LocalDate.of(1991, 1, 20), "Scotland");
      assertThat(matcher.matches(cairney, candCairney)).isTrue();
    }

    @Test
    @DisplayName("Admite monónimos deportivos en el mismo club cuando fecha y país coinciden")
    void acceptsMononymsInSameClub() {
      League league = new League("La Liga");

      Team betis = new Team("Real Betis Balompié", league, true);
      Player bartra = new Player("Bartra", betis, "Defender");
      bartra.updateOptionalDetails(LocalDate.of(1991, 1, 15), "Spain");
      Candidate candBartra =
          new Candidate(
              "Marc Bartra", null, "Real Betis", "Soccer", LocalDate.of(1991, 1, 15), "Spain");
      assertThat(matcher.matches(bartra, candBartra)).isTrue();

      Team osasuna = new Team("CA Osasuna", league, true);
      Player catena = new Player("Catena", osasuna, "Defender");
      catena.updateOptionalDetails(LocalDate.of(1994, 10, 28), "Spain");
      Candidate candCatena =
          new Candidate(
              "Alejandro Catena", null, "Osasuna", "Soccer", LocalDate.of(1994, 10, 28), "Spain");
      assertThat(matcher.matches(catena, candCatena)).isTrue();
    }

    @Test
    @DisplayName(
        "Tolera discrepancias menores de fecha en matches cuando club, nombre y país coinciden")
    void acceptsMinorDateDiscrepancyInMatchesWhenClubAndNameMatch() {
      League league = new League("Premier League");

      Team palace = new Team("Crystal Palace FC", league, true);
      Player benitez = new Player("Walter Benítez", palace, "Goalkeeper");
      benitez.updateOptionalDetails(LocalDate.of(1993, 3, 19), "Argentina");
      Candidate candBenitez =
          new Candidate(
              "Walter Benítez",
              null,
              "Crystal Palace",
              "Soccer",
              LocalDate.of(1993, 1, 19),
              "Argentina");
      assertThat(matcher.matches(benitez, candBenitez)).isTrue();

      League bundesliga = new League("Bundesliga");
      Team hamburg = new Team("Hamburger SV", bundesliga, true);
      Player poulsen = new Player("Yussuf Poulsen", hamburg, "Forward");
      poulsen.updateOptionalDetails(LocalDate.of(1994, 6, 16), "Denmark");
      Candidate candPoulsen =
          new Candidate(
              "Yussuf Poulsen", null, "Hamburg", "Soccer", LocalDate.of(1994, 6, 15), "Denmark");
      assertThat(matcher.matches(poulsen, candPoulsen)).isTrue();
    }
  }
}
