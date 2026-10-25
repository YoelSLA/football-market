package footballmarket.support;

import footballmarket.models.MarketTransaction;
import footballmarket.models.Player;
import footballmarket.models.User;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/** Fixtures de usuarios, jugadores y operaciones confirmadas para el historial personal. */
@Component
public class OperationHistoryTestData {

  private final MarketTestData market;
  private final FinancialTestData financial;

  public OperationHistoryTestData(MarketTestData marketData, FinancialTestData financialData) {
    market = marketData;
    financial = financialData;
  }

  /** Crea un usuario normal persistido. */
  public User registerUser() {
    return market.registerBuyer();
  }

  /** Devuelve el superusuario persistido del mercado. */
  public User registerSuperuser() {
    return market.registerSuperuser();
  }

  /** Crea un jugador activo con equipo y liga persistidos. */
  public Player createPlayer() {
    return market.createVisiblePlayer();
  }

  /** Registra una transacción confirmada con sus valores históricos. */
  public MarketTransaction record(
      User buyer,
      User seller,
      Player player,
      int quantity,
      long unitPrice,
      OffsetDateTime executedAt) {
    return financial.record(buyer, seller, player, quantity, unitPrice, executedAt);
  }
}
