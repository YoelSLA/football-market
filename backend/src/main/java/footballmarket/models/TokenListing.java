package footballmarket.models;

import footballmarket.models.enums.TokenListingStatus;
import footballmarket.models.exceptions.InvalidTokenListingException;
import footballmarket.models.records.CurrentQuote;
import footballmarket.models.records.TokenListingView;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Publicación con precio congelado y reserva derivada, sin transferencia de recursos al crearla.
 */
@Entity
@Table(name = "token_listings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenListing {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "seller_user_id", nullable = false)
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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private TokenListingStatus status;

  @Column(name = "quote_calculated_at", nullable = false)
  private OffsetDateTime quoteCalculatedAt;

  @Column(name = "quote_strategy_version", length = 64)
  private String quoteStrategyVersion;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Column(name = "closed_at")
  private OffsetDateTime closedAt;

  /**
   * Construye una oferta sin mover recursos; el llamador garantiza la disponibilidad bajo bloqueo.
   */
  public static TokenListing publish(
      User seller,
      Player player,
      int quantity,
      CurrentQuote quote,
      OffsetDateTime createdAt,
      OffsetDateTime expiresAt) {
    if (seller == null
        || player == null
        || quantity < 1
        || quantity > 100
        || quote == null
        || quote.price() <= 0
        || quote.calculatedAt() == null
        || createdAt == null
        || expiresAt == null
        || !expiresAt.isAfter(createdAt)) {
      throw new InvalidTokenListingException();
    }
    TokenListing listing = new TokenListing();
    listing.seller = seller;
    listing.player = player;
    listing.quantity = quantity;
    listing.unitPrice = quote.price();
    listing.totalAmount = Math.multiplyExact((long) quantity, quote.price());
    listing.quoteCalculatedAt = quote.calculatedAt();
    listing.quoteStrategyVersion = quote.strategyVersion();
    listing.status = TokenListingStatus.ACTIVE;
    listing.createdAt = createdAt;
    listing.expiresAt = expiresAt;
    return listing;
  }

  /** Usa el instante decisivo obtenido bajo bloqueo, no una segunda lectura del reloj al cerrar. */
  public void markSold(OffsetDateTime validatedAt) {
    this.close(TokenListingStatus.SOLD, validatedAt);
  }

  /** Cancela exclusivamente una publicación vigente en el instante recibido bajo bloqueo. */
  public void markCancelled(OffsetDateTime validatedAt) {
    this.close(TokenListingStatus.CANCELLED, validatedAt);
  }

  private void close(TokenListingStatus target, OffsetDateTime validatedAt) {
    if (!this.isActiveAndValid(validatedAt)) {
      throw new InvalidTokenListingException();
    }
    this.status = target;
    this.closedAt = validatedAt;
  }

  public TokenListingStatus effectiveStatus(OffsetDateTime now) {
    return this.status == TokenListingStatus.ACTIVE && !now.isBefore(this.expiresAt)
        ? TokenListingStatus.EXPIRED
        : this.status;
  }

  public boolean isActiveAndValid(OffsetDateTime now) {
    return this.effectiveStatus(now) == TokenListingStatus.ACTIVE;
  }

  public TokenListingView snapshot(OffsetDateTime now) {
    return new TokenListingView(
        this.id,
        this.seller.getId(),
        this.player.getId(),
        this.quantity,
        this.unitPrice,
        this.totalAmount,
        this.effectiveStatus(now),
        this.createdAt,
        this.expiresAt,
        this.closedAt);
  }
}
