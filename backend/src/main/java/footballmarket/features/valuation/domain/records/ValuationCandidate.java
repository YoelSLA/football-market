package footballmarket.features.valuation.domain.records;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.statistics.domain.models.PlayerStatistics;
import footballmarket.features.valuation.domain.enums.GeneralPosition;

/** Filas de campaña y contexto necesarias para preparar un jugador. */
public record ValuationCandidate(
    Player player,
    PlayerStatistics jsonStatistics,
    PlayerStatistics scrapingStatistics,
    GeneralPosition position,
    ValuationPopulationKey populationKey) {}
