package footballmarket.features.market.domain.models;

import footballmarket.features.market.domain.exceptions.InsufficientUserCreditsException;
import footballmarket.features.market.domain.exceptions.InvalidCreditAmountException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Saldo de créditos de un usuario en el mercado.
 *
 * <p>El saldo se expresa en centésimas de crédito conforme a CL-01: {@code 100} centésimas
 * equivalen a 1 crédito. La invariante de no negatividad se sostiene en el modelo y además en la
 * base de datos, de modo que ningún camino de aplicación pueda dejar un saldo negativo.
 */
@Entity
@Table(name = "user_credits")
@Getter
@NoArgsConstructor
public class UserCredit {

  /** Saldo inicial de un usuario normal: 100 créditos. */
  public static final long INITIAL_USER_CREDITS = 10_000L;

  /** Saldo inicial del superusuario: es el vendedor del mercado y no compra. */
  public static final long INITIAL_SUPERUSER_CREDITS = 0L;

  @Id
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "available_credits", nullable = false)
  private long availableCredits;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  private UserCredit(Long userId, long availableCredits) {
    this.userId = userId;
    this.availableCredits = requireNotNegative(availableCredits);
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  /**
   * Crea la cuenta de un usuario normal con su saldo inicial.
   *
   * @param userId usuario propietario de la cuenta
   * @return cuenta con 100 créditos
   */
  public static UserCredit forUser(Long userId) {
    return new UserCredit(userId, INITIAL_USER_CREDITS);
  }

  /**
   * Crea la cuenta del superusuario, cuyo saldo inicial es cero.
   *
   * @param userId usuario propietario de la cuenta
   * @return cuenta con 0 créditos
   */
  public static UserCredit forSuperuser(Long userId) {
    return new UserCredit(userId, INITIAL_SUPERUSER_CREDITS);
  }

  /**
   * Restaura una cuenta con un saldo ya persistido.
   *
   * <p>Se usa al leer una fila existente. No crea saldos nuevos durante una compra: una cuenta
   * ausente produce rechazo por créditos insuficientes (FR-010).
   *
   * @param userId usuario propietario de la cuenta
   * @param availableCredits saldo persistido en centésimas
   * @return cuenta con el saldo indicado
   */
  public static UserCredit restore(Long userId, long availableCredits) {
    return new UserCredit(userId, availableCredits);
  }

  /**
   * Restaura una cuenta con su marca de tiempo original.
   *
   * @param userId usuario propietario de la cuenta
   * @param availableCredits saldo persistido en centésimas
   * @param updatedAt marca de tiempo persistida
   * @return cuenta con el saldo y la marca de tiempo indicados
   */
  public static UserCredit restore(Long userId, long availableCredits, OffsetDateTime updatedAt) {
    UserCredit credit = new UserCredit(userId, availableCredits);
    if (updatedAt != null) {
      credit.updatedAt = updatedAt;
    }
    return credit;
  }

  /**
   * Debita el saldo de esta cuenta.
   *
   * @param amount cantidad en centésimas a descontar
   * @throws InvalidCreditAmountException si el importe a debitar es negativo
   * @throws InsufficientUserCreditsException si el saldo no alcanza
   */
  public void debit(long amount) {
    requirePositiveOperation(amount);
    if (amount > this.availableCredits) {
      throw new InsufficientUserCreditsException();
    }
    this.availableCredits -= amount;
    this.touch();
  }

  /**
   * Acredita saldo a esta cuenta.
   *
   * @param amount cantidad en centésimas a sumar
   * @throws InvalidCreditAmountException si el importe a acreditar es negativo
   */
  public void credit(long amount) {
    requirePositiveOperation(amount);
    this.availableCredits = Math.addExact(this.availableCredits, amount);
    this.touch();
  }

  /**
   * Indica si el saldo permite cubrir el importe indicado sin quedar negativo.
   *
   * @param amount importe en centésimas
   * @return {@code true} cuando el saldo alcanza
   */
  public boolean covers(long amount) {
    return amount <= this.availableCredits;
  }

  private void touch() {
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  private static void requirePositiveOperation(long amount) {
    if (amount < 0) {
      throw new InvalidCreditAmountException("El importe de la operación no puede ser negativo.");
    }
  }

  private static long requireNotNegative(long value) {
    if (value < 0) {
      throw new InvalidCreditAmountException("El saldo no puede ser negativo.");
    }
    return value;
  }
}
