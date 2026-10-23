package footballmarket.models.records;

import footballmarket.models.enums.GeneralPosition;

/** Identidad de una población deportiva comparable. */
public record ValuationPopulationKey(GeneralPosition position, Long leagueId, String seasonKey) {}
