package footballmarket.repositories;

import footballmarket.models.PlayerExternalReference;
import footballmarket.models.PlayerProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Resolución de identidades externas, independiente de la clave primaria del jugador. */
public interface PlayerExternalReferenceRepository
    extends JpaRepository<PlayerExternalReference, Long> {
  /** Proyección escalar para detectar cambios de propietario antes de mutar entidades. */
  interface Identity {
    String getExternalId();

    Long getPlayerId();
  }

  @Query(
      "select r.externalId as externalId, r.player.id as playerId from PlayerExternalReference r where r.provider = :provider")
  List<Identity> findIdentitiesByProvider(@Param("provider") PlayerProvider provider);

  @EntityGraph(attributePaths = "player")
  Optional<PlayerExternalReference> findByProviderAndExternalId(
      PlayerProvider provider, String externalId);

  @EntityGraph(attributePaths = "player")
  List<PlayerExternalReference> findByProvider(PlayerProvider provider);
}
