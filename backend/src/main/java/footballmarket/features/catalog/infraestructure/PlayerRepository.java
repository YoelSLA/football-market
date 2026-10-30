package footballmarket.features.catalog.infraestructure;

import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Consulta local del catálogo vigente con asociaciones disponibles al terminar la lectura. */
@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
  /** Mismo predicado de visibilidad y filtros para contenido y conteo, antes de paginar. */
  @EntityGraph(attributePaths = {"team", "team.league"})
  @Query(
      "select p from Player p join p.team t join t.league l"
          + " where p.active = true and t.current = true"
          + " and (:leagueId is null or l.id = :leagueId)"
          + " and (:teamId is null or t.id = :teamId)"
          + " and (:position is null or p.position = :position)"
          + " and (:nationality is null or p.nationality = :nationality)"
          + " and (cast(:name as string) is null or cast(function('catalog_normalize_name', cast(:name as string)) as string) = ''"
          + " or cast(function('catalog_normalize_name', p.name) as string) like"
          + " concat('%', replace(replace(replace(cast(function('catalog_normalize_name', cast(:name as string)) as string),"
          + " '!', '!!'), '%', '!%'), '_', '!_'), '%') escape '!')")
  Page<Player> findCatalog(
      @Param("name") String name,
      @Param("leagueId") Long leagueId,
      @Param("teamId") Long teamId,
      @Param("position") String position,
      @Param("nationality") String nationality,
      Pageable pageable);

  @Query(
      "select distinct l from Player p join p.team t join t.league l where p.active=true and t.current=true order by l.name, l.id")
  List<League> findCatalogLeagues();

  @Query(
      "select distinct t from Player p join p.team t join t.league l where p.active=true and t.current=true order by t.name, t.id")
  List<Team> findCatalogTeams();

  @Query(
      "select distinct p.position from Player p join p.team t join t.league l where p.active=true and t.current=true and trim(p.position) <> '' order by p.position")
  List<String> findCatalogPositions();

  @Query(
      "select distinct p.nationality from Player p join p.team t join t.league l where p.active=true and t.current=true and p.nationality is not null and trim(p.nationality) <> '' order by p.nationality")
  List<String> findCatalogNationalities();

  @EntityGraph(attributePaths = {"team", "team.league"})
  @Query(
      "select p from Player p join p.team t join t.league l where p.active = true and t.current = true")
  Page<Player> findByActiveTrue(Pageable pageable);

  /**
   * Consulta individual con el mismo predicado de visibilidad del catálogo.
   *
   * <p>Replica exactamente el filtro de {@link #findByActiveTrue(Pageable)} (jugador activo con
   * equipo vigente) y carga las asociaciones necesarias para la respuesta. Un jugador existente
   * pero no visible no produce coincidencias.
   *
   * @param id identificador interno del jugador
   * @return jugador visible con su equipo y liga cargados, o vacío si no cumple la visibilidad
   */
  @EntityGraph(attributePaths = {"team", "team.league"})
  @Query(
      "select p from Player p join p.team t join t.league l"
          + " where p.id = :id and p.active = true and t.current = true")
  Optional<Player> findVisibleById(Long id);

  /**
   * Lectura por lote de los jugadores indicados, con su equipo disponible para la respuesta.
   *
   * <p>Una sola consulta para toda la página: el equipo se carga en la misma lectura, de modo que
   * la presentación de las posiciones no abre una consulta por jugador. No filtra por visibilidad:
   * un jugador inactivo permanece en el portfolio con sus datos descriptivos.
   *
   * @param playerIds identificadores de los jugadores de la página
   * @return jugadores indicados con su equipo cargado
   */
  @Query("select p from Player p join fetch p.team t where p.id in :playerIds")
  List<Player> findAllByIdInWithTeam(@Param("playerIds") List<Long> playerIds);

  Optional<Player> findFirstByIdGreaterThanOrderByIdAsc(Long id);

  /** Materializa el snapshot de las ligas seleccionadas, sin filtros de vigencia adicionales. */
  @EntityGraph(attributePaths = {"team", "team.league", "externalReferences"})
  @Query("select distinct p from Player p where p.team.league.id in :leagueIds order by p.id")
  List<Player> findIdentitySnapshot(List<Long> leagueIds);

  /** Materializa únicamente pendientes todavía existentes y sin ninguna identidad WhoScored. */
  @EntityGraph(attributePaths = {"team", "team.league", "externalReferences"})
  @Query(
      "select distinct p from Player p where p.id in :playerIds and not exists (select r.id from PlayerExternalReference r where r.player = p and r.provider = footballmarket.features.catalog.domain.enums.ExternalProvider.WHOSCORED) order by p.id")
  List<Player> findHistoricalIdentitySnapshot(List<Long> playerIds);

  /** Bloqueo breve de la identidad local, exclusivamente durante confirmación. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Player p where p.id = :id")
  Optional<Player> lockIdentityById(Long id);
}
