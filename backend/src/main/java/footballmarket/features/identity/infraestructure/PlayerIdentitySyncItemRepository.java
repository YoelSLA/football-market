package footballmarket.features.identity.infraestructure;

import footballmarket.features.identity.domain.enums.PlayerIdentityItemResult;
import footballmarket.features.identity.domain.models.PlayerIdentitySyncItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Snapshots y agregados exclusivos de resultados por jugador. */
public interface PlayerIdentitySyncItemRepository
    extends JpaRepository<PlayerIdentitySyncItem, Long> {
  List<PlayerIdentitySyncItem> findByRunIdOrderById(Long runId);

  /** IDs históricos del origen; la existencia y las referencias actuales se filtran al capturar. */
  @Query(
      "select i.playerId from PlayerIdentitySyncItem i where i.runId = :sourceRunId and i.result in (footballmarket.features.identity.domain.enums.PlayerIdentityItemResult.NO_MATCH, footballmarket.features.identity.domain.enums.PlayerIdentityItemResult.AMBIGUOUS) order by i.playerId")
  List<Long> findHistoricalPendingPlayerIds(Long sourceRunId);

  /** Resultado agrupado por estado; no mezcla filas externas ni ámbitos fallidos. */
  interface ResultCount {
    PlayerIdentityItemResult getResult();

    long getAmount();
  }

  @Query(
      "select i.result as result, count(i) as amount from PlayerIdentitySyncItem i where i.runId = :runId group by i.result")
  List<ResultCount> countResults(Long runId);
}
