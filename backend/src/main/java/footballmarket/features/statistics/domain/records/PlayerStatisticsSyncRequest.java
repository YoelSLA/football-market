package footballmarket.features.statistics.domain.records;

import java.time.LocalDate;

/** Opciones del lote de scraping; el canal lo fija exclusivamente el caso de uso. */
public record PlayerStatisticsSyncRequest(LocalDate extractionDate, Integer maxPlayers) {}
