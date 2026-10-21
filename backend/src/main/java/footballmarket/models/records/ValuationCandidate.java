package footballmarket.models.records;

import footballmarket.models.Player;
import footballmarket.models.PlayerStatistics;
import footballmarket.models.enums.GeneralPosition;

/** Filas de campaña y contexto necesarias para preparar un jugador. */
public record ValuationCandidate(
    Player player,
    PlayerStatistics jsonStatistics,
    PlayerStatistics scrapingStatistics,
    GeneralPosition position,
    ValuationPopulationKey populationKey) {}
