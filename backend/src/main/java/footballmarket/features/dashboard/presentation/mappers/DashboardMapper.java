package footballmarket.features.dashboard.presentation.mappers;

import footballmarket.features.dashboard.infrastructure.projections.P2PDealProjection;
import footballmarket.features.dashboard.infrastructure.projections.PlayerMoverProjection;
import footballmarket.features.dashboard.infrastructure.projections.ValuePickProjection;
import footballmarket.features.dashboard.presentation.dtos.response.AllocationItemDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardAllocationDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardFinancialSummaryDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardP2PDealDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardPlayerMoverDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardRecentActivityDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardValuePickDTO;
import footballmarket.features.portfolio.domain.records.OperationHistoryView;
import footballmarket.features.portfolio.domain.records.PortfolioView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DashboardMapper {

  private DashboardMapper() {}

  public static DashboardFinancialSummaryDTO toFinancialSummary(
      long availableCredits, PortfolioView.PortfolioSummary summary) {
    if (summary == null) {
      return new DashboardFinancialSummaryDTO(
          availableCredits, 0L, availableCredits, 0L, 0L, 0L, 0L, BigDecimal.ZERO, 0, 0, 0, true);
    }

    Long netWorth =
        summary.totalCurrentValue() != null ? availableCredits + summary.totalCurrentValue() : null;

    return new DashboardFinancialSummaryDTO(
        availableCredits,
        summary.totalCurrentValue(),
        netWorth,
        summary.totalInvested(),
        summary.totalUnrealizedProfitLoss(),
        summary.totalRealizedProfitLoss(),
        summary.totalProfitLoss(),
        summary.totalProfitLossPercentage(),
        summary.totalTokens(),
        summary.totalAvailableTokens(),
        summary.totalReservedTokens(),
        summary.valuationComplete());
  }

  public static DashboardAllocationDTO toAllocation(
      List<PortfolioView.PortfolioPosition> positions) {
    if (positions == null || positions.isEmpty()) {
      return new DashboardAllocationDTO(List.of(), List.of(), BigDecimal.ZERO, "Sin posiciones");
    }

    long totalPortfolioValue = 0L;
    Map<String, Long> posValues = new HashMap<>();
    Map<String, Integer> posTokens = new HashMap<>();

    Map<String, Long> leagueValues = new HashMap<>();
    Map<String, Integer> leagueTokens = new HashMap<>();

    long maxHoldingValue = 0L;
    String topHoldingName = "Ninguno";

    for (PortfolioView.PortfolioPosition pos : positions) {
      if (!"OPEN".equalsIgnoreCase(pos.status())) {
        continue;
      }
      long val = pos.currentValue() != null ? pos.currentValue() : pos.acquisitionCost();
      totalPortfolioValue += val;

      if (val > maxHoldingValue) {
        maxHoldingValue = val;
        topHoldingName = pos.playerName() != null ? pos.playerName() : "Desconocido";
      }

      String normPos = normalizePosition(pos.position());
      posValues.put(normPos, posValues.getOrDefault(normPos, 0L) + val);
      posTokens.put(normPos, posTokens.getOrDefault(normPos, 0) + pos.quantity());

      String normLeague = normalizeLeague(pos.leagueName());
      leagueValues.put(normLeague, leagueValues.getOrDefault(normLeague, 0L) + val);
      leagueTokens.put(normLeague, leagueTokens.getOrDefault(normLeague, 0) + pos.quantity());
    }

    List<AllocationItemDTO> byPosition = new ArrayList<>();
    List<Map.Entry<String, Long>> sortedPositions =
        posValues.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .toList();
    for (Map.Entry<String, Long> entry : sortedPositions) {
      BigDecimal pct =
          totalPortfolioValue > 0
              ? BigDecimal.valueOf(entry.getValue())
                  .multiply(BigDecimal.valueOf(100))
                  .divide(BigDecimal.valueOf(totalPortfolioValue), 2, RoundingMode.HALF_UP)
              : BigDecimal.ZERO;
      byPosition.add(
          new AllocationItemDTO(
              entry.getKey(),
              getPositionLabel(entry.getKey()),
              entry.getValue(),
              pct,
              posTokens.getOrDefault(entry.getKey(), 0)));
    }

    List<AllocationItemDTO> byLeague = new ArrayList<>();
    List<Map.Entry<String, Long>> sortedLeagues =
        leagueValues.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .toList();
    for (Map.Entry<String, Long> entry : sortedLeagues) {
      BigDecimal pct =
          totalPortfolioValue > 0
              ? BigDecimal.valueOf(entry.getValue())
                  .multiply(BigDecimal.valueOf(100))
                  .divide(BigDecimal.valueOf(totalPortfolioValue), 2, RoundingMode.HALF_UP)
              : BigDecimal.ZERO;
      byLeague.add(
          new AllocationItemDTO(
              entry.getKey(),
              getLeagueLabel(entry.getKey()),
              entry.getValue(),
              pct,
              leagueTokens.getOrDefault(entry.getKey(), 0)));
    }

    BigDecimal topConcentration =
        totalPortfolioValue > 0
            ? BigDecimal.valueOf(maxHoldingValue)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalPortfolioValue), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    return new DashboardAllocationDTO(byPosition, byLeague, topConcentration, topHoldingName);
  }

  public static DashboardPlayerMoverDTO toPlayerMover(PlayerMoverProjection p) {
    String[] names = splitName(p.getName());
    return new DashboardPlayerMoverDTO(
        p.getPlayerId(),
        names[0],
        names[1],
        p.getTeamName() != null ? p.getTeamName() : "Club",
        p.getLeagueName() != null ? p.getLeagueName() : "Liga",
        normalizePosition(p.getPosition()),
        p.getImageUrl(),
        p.getCurrentPrice() != null ? p.getCurrentPrice() : 0L,
        p.getPreviousPrice(),
        p.getDeltaCredits(),
        p.getDeltaPercentage(),
        p.getScore());
  }

  public static DashboardValuePickDTO toValuePick(ValuePickProjection p) {
    String[] names = splitName(p.getName());
    return new DashboardValuePickDTO(
        p.getPlayerId(),
        names[0],
        names[1],
        p.getTeamName() != null ? p.getTeamName() : "Club",
        p.getLeagueName() != null ? p.getLeagueName() : "Liga",
        normalizePosition(p.getPosition()),
        p.getImageUrl(),
        p.getWhoScoredScore() != null ? p.getWhoScoredScore() : BigDecimal.ZERO,
        p.getCurrentPrice() != null ? p.getCurrentPrice() : 0L,
        p.getValueRatio() != null ? p.getValueRatio() : BigDecimal.ZERO);
  }

  public static DashboardP2PDealDTO toP2PDeal(P2PDealProjection p) {
    return new DashboardP2PDealDTO(
        p.getListingId(),
        p.getPlayerId(),
        p.getPlayerName() != null ? p.getPlayerName() : "Futbolista",
        p.getTeamName() != null ? p.getTeamName() : "Club",
        p.getLeagueName() != null ? p.getLeagueName() : "Liga",
        p.getImageUrl(),
        p.getAvailableTokens() != null ? p.getAvailableTokens() : 0,
        p.getP2pUnitPrice() != null ? p.getP2pUnitPrice() : 0L,
        p.getOfficialQuotePrice() != null ? p.getOfficialQuotePrice() : 0L,
        p.getDiscountPercentage() != null ? p.getDiscountPercentage() : BigDecimal.ZERO);
  }

  public static List<DashboardRecentActivityDTO> toRecentActivity(
      OperationHistoryView historyView) {
    if (historyView == null || historyView.transactions() == null) {
      return List.of();
    }
    return historyView.transactions().stream()
        .map(
            tx ->
                new DashboardRecentActivityDTO(
                    tx.transactionId(),
                    tx.type() != null ? tx.type().name() : "BUY",
                    "OFFICIAL",
                    tx.player() != null ? tx.player().id() : 0L,
                    tx.player() != null && tx.player().name() != null
                        ? tx.player().name()
                        : "Futbolista",
                    tx.quantity(),
                    tx.unitPrice(),
                    tx.totalAmount(),
                    tx.executedAt()))
        .toList();
  }

  private static String[] splitName(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      return new String[] {"Jugador", ""};
    }
    String trimmed = fullName.trim();
    int lastSpace = trimmed.lastIndexOf(' ');
    if (lastSpace > 0) {
      return new String[] {trimmed.substring(0, lastSpace), trimmed.substring(lastSpace + 1)};
    }
    return new String[] {trimmed, ""};
  }

  private static String normalizePosition(String pos) {
    if (pos == null) return "DELANTERO";
    String lower = pos.toLowerCase();
    if (lower.contains("delanter")
        || lower.contains("forward")
        || lower.contains("att")
        || lower.contains("st")) {
      return "DELANTERO";
    }
    if (lower.contains("medio") || lower.contains("midfield")) {
      return "MEDIOCAMPISTA";
    }
    if (lower.contains("defen")) {
      return "DEFENSOR";
    }
    if (lower.contains("arqu")
        || lower.contains("goal")
        || lower.contains("keeper")
        || lower.contains("port")) {
      return "ARQUERO";
    }
    return "DELANTERO";
  }

  private static String getPositionLabel(String key) {
    return switch (key) {
      case "DELANTERO" -> "Delanteros";
      case "MEDIOCAMPISTA" -> "Mediocampistas";
      case "DEFENSOR" -> "Defensores";
      case "ARQUERO" -> "Arqueros";
      default -> key;
    };
  }

  private static String normalizeLeague(String league) {
    if (league == null || league.isBlank()) {
      return "OTRAS_LIGAS";
    }
    String l = league.trim().toUpperCase();
    if (l.contains("BUNDESLIGA")) return "BUNDESLIGA";
    if (l.contains("PREMIER")) return "PREMIER_LEAGUE";
    if (l.contains("SERIE A") || l.contains("CALCIO")) return "SERIE_A";
    if (l.contains("LIGUE 1")) return "LIGUE_1";
    if (l.contains("PRIMERA") || l.contains("LIGA") || l.contains("SANTANDER")) return "LALIGA";
    return "OTRAS_LIGAS";
  }

  private static String getLeagueLabel(String key) {
    return switch (key) {
      case "PREMIER_LEAGUE" -> "Premier League";
      case "LALIGA" -> "LaLiga";
      case "SERIE_A" -> "Serie A";
      case "BUNDESLIGA" -> "Bundesliga";
      case "LIGUE_1" -> "Ligue 1";
      default -> "Otras Ligas";
    };
  }
}
