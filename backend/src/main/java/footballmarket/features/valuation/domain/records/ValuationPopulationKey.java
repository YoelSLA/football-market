package footballmarket.features.valuation.domain.records;

import footballmarket.features.valuation.domain.enums.GeneralPosition;

/** Identidad de una población deportiva comparable. */
public record ValuationPopulationKey(GeneralPosition position, Long leagueId, String seasonKey) {}
