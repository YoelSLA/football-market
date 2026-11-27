package footballmarket.features.market.domain.models;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.enums.TokenListingStatus;
import footballmarket.features.market.domain.exceptions.InvalidListingPriceException;
import footballmarket.features.market.domain.exceptions.InvalidTokenListingException;
import footballmarket.features.market.domain.records.TokenListingView;
import footballmarket.features.quote.domain.records.CurrentQuote;
import jakarta.persistence.*;
import java.math.BigInteger;
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
    return TokenListing.publish(seller, player, quantity, null, quote, createdAt, expiresAt);
  }

  /**
   * Construye una publicación con precio acordado explícito o con la cotización vigente.
   *
   * @param seller propietario de los tokens reservados
   * @param player jugador publicado
   * @param quantity cantidad de tokens reservada
   * @param requestedUnitPrice precio opcional solicitado en centésimas
   * @param quote cotización vigente usada como referencia
   * @param createdAt instante de publicación
   * @param expiresAt instante de vencimiento
   * @return publicación con precio e importe congelados
   * @throws InvalidTokenListingException si los datos estructurales de la publicación son inválidos
   * @throws InvalidListingPriceException si el precio solicitado queda fuera de la banda permitida
   */
  public static TokenListing publish(
      User seller,
      Player player,
      int quantity,
      Long requestedUnitPrice,
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
    long unitPrice = requestedUnitPrice == null ? quote.price() : requestedUnitPrice;
    if (!TokenListing.isWithinAllowedPrice(unitPrice, quote.price())) {
      throw new InvalidListingPriceException();
    }
    TokenListing listing = new TokenListing();
    listing.seller = seller;
    listing.player = player;
    listing.quantity = quantity;
    listing.unitPrice = unitPrice;
    listing.totalAmount = Math.multiplyExact((long) quantity, unitPrice);
    listing.quoteCalculatedAt = quote.calculatedAt();
    listing.quoteStrategyVersion = quote.strategyVersion();
    listing.status = TokenListingStatus.ACTIVE;
    listing.createdAt = createdAt;
    listing.expiresAt = expiresAt;
    return listing;
  }

  private static boolean isWithinAllowedPrice(long unitPrice, long quotePrice) {
    if (unitPrice <= 0) {
      return false;
    }
    BigInteger price = BigInteger.valueOf(unitPrice).multiply(BigInteger.valueOf(100));
    BigInteger quote = BigInteger.valueOf(quotePrice);
    return price.compareTo(quote.multiply(BigInteger.valueOf(80))) >= 0
        && price.compareTo(quote.multiply(BigInteger.valueOf(120))) <= 0;
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
        this.closedAt,
        this.player.getName(),
        this.player.getTeam() != null ? this.player.getTeam().getId() : null,
        this.player.getTeam() != null ? this.player.getTeam().getName() : null,
        this.player.getTeam() != null && this.player.getTeam().getLeague() != null
            ? this.player.getTeam().getLeague().getId()
            : null,
        this.player.getTeam() != null && this.player.getTeam().getLeague() != null
            ? this.player.getTeam().getLeague().getName()
            : null,
        this.player.getPosition(),
        this.player.getImageUrl(),
        this.player.getFallbackImageUrl());
  }
}
