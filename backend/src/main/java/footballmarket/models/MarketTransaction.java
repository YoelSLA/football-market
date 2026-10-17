package footballmarket.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Registro financiero de una compra confirmada.
 *
 * <p>Es append-only desde la aplicación: solo existe para compras confirmadas y no expone
 * operaciones de modificación. Conserva los estados de créditos y tenencias anteriores y
 * posteriores de ambas partes, además del precio aplicado y su procedencia, de modo que la
 * operación pueda reconstruirse sin recalcular nada.
 */
@Entity
@Table(name = "market_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "buyer_id", nullable = false)
  private User buyer;

  @ManyToOne(optional = false)
  @JoinColumn(name = "seller_id", nullable = false)
  private User seller;

  @ManyToOne(optional = false)
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "unit_price", nullable = false)
  private long unitPrice;

  @Column(name = "total_amount", nullable = false)
  private long totalAmount;

  @Column(name = "buyer_credits_before", nullable = false)
  private long buyerCreditsBefore;

  @Column(name = "buyer_credits_after", nullable = false)
  private long buyerCreditsAfter;

  @Column(name = "seller_credits_before", nullable = false)
  private long sellerCreditsBefore;

  @Column(name = "seller_credits_after", nullable = false)
  private long sellerCreditsAfter;

  @Column(name = "buyer_tokens_before", nullable = false)
  private int buyerTokensBefore;

  @Column(name = "buyer_tokens_after", nullable = false)
  private int buyerTokensAfter;

  @Column(name = "seller_tokens_before", nullable = false)
  private int sellerTokensBefore;

  @Column(name = "seller_tokens_after", nullable = false)
  private int sellerTokensAfter;

  @Column(name = "quote_calculated_at", nullable = false)
  private OffsetDateTime quoteCalculatedAt;

  @Column(name = "quote_strategy_version")
  private String quoteStrategyVersion;

  @Column(name = "executed_at", nullable = false)
  private OffsetDateTime executedAt;

  private MarketTransaction(
      User buyer,
      User seller,
      Player player,
      int quantity,
      long unitPrice,
      long totalAmount,
      long buyerCreditsBefore,
      long buyerCreditsAfter,
      long sellerCreditsBefore,
      long sellerCreditsAfter,
      int buyerTokensBefore,
      int buyerTokensAfter,
      int sellerTokensBefore,
      int sellerTokensAfter,
      OffsetDateTime quoteCalculatedAt,
      String quoteStrategyVersion,
      OffsetDateTime executedAt) {
    this.buyer = buyer;
    this.seller = seller;
    this.player = player;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.totalAmount = totalAmount;
    this.buyerCreditsBefore = buyerCreditsBefore;
    this.buyerCreditsAfter = buyerCreditsAfter;
    this.sellerCreditsBefore = sellerCreditsBefore;
    this.sellerCreditsAfter = sellerCreditsAfter;
    this.buyerTokensBefore = buyerTokensBefore;
    this.buyerTokensAfter = buyerTokensAfter;
    this.sellerTokensBefore = sellerTokensBefore;
    this.sellerTokensAfter = sellerTokensAfter;
    this.quoteCalculatedAt = quoteCalculatedAt;
    this.quoteStrategyVersion = quoteStrategyVersion;
    this.executedAt = executedAt;
  }

  /**
   * Registra una compra confirmada con los estados observados bajo bloqueo.
   *
   * @param buyer comprador autenticado
   * @param seller superusuario vendedor
   * @param player jugador comprado
   * @param quantity cantidad transferida
   * @param unitPrice precio por token en centésimas
   * @param totalAmount importe total en centésimas
   * @param buyerCreditsBefore saldo del comprador antes de la transferencia
   * @param buyerCreditsAfter saldo del comprador después de la transferencia
   * @param sellerCreditsBefore saldo del vendedor antes de la transferencia
   * @param sellerCreditsAfter saldo del vendedor después de la transferencia
   * @param buyerTokensBefore tenencia del comprador antes de la transferencia
   * @param buyerTokensAfter tenencia del comprador después de la transferencia
   * @param sellerTokensBefore tenencia del vendedor antes de la transferencia
   * @param sellerTokensAfter tenencia del vendedor después de la transferencia
   * @param quoteCalculatedAt instante de cálculo de la cotización aplicada
   * @param quoteStrategyVersion estrategia que produjo la cotización, o {@code null} si la fuente
   *     no la proporcionó
   * @param executedAt instante de confirmación de la operación
   * @return registro financiero de la operación
   */
  public static MarketTransaction record(
      User buyer,
      User seller,
      Player player,
      int quantity,
      long unitPrice,
      long totalAmount,
      long buyerCreditsBefore,
      long buyerCreditsAfter,
      long sellerCreditsBefore,
      long sellerCreditsAfter,
      int buyerTokensBefore,
      int buyerTokensAfter,
      int sellerTokensBefore,
      int sellerTokensAfter,
      OffsetDateTime quoteCalculatedAt,
      String quoteStrategyVersion,
      OffsetDateTime executedAt) {
    return new MarketTransaction(
        buyer,
        seller,
        player,
        quantity,
        unitPrice,
        totalAmount,
        buyerCreditsBefore,
        buyerCreditsAfter,
        sellerCreditsBefore,
        sellerCreditsAfter,
        buyerTokensBefore,
        buyerTokensAfter,
        sellerTokensBefore,
        sellerTokensAfter,
        quoteCalculatedAt,
        quoteStrategyVersion,
        executedAt);
  }
}
