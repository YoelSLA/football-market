package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.exceptions.InsufficientUserCreditsException;
import footballmarket.models.exceptions.InvalidCreditAmountException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Reglas de dominio del saldo de créditos: centésimas, no negatividad y transferencias. */
@org.junit.jupiter.api.Tag("unit")
class UserCreditTest {

  private static final long BUYER_ID = 42L;

  @Nested
  @DisplayName("Saldo inicial")
  class InitialBalance {
    @Test
    @DisplayName("Un usuario normal arranca con 100 créditos y el superusuario con 0")
    void asignaSaldosInicialesSegunElRol() {
      // Arrange / Act
      UserCredit user = UserCredit.forUser(BUYER_ID);
      UserCredit superuser = UserCredit.forSuperuser(1L);

      // Assert
      assertThat(user.getAvailableCredits()).isEqualTo(10_000L);
      assertThat(superuser.getAvailableCredits()).isZero();
      assertThat(user.getUserId()).isEqualTo(BUYER_ID);
    }

    @Test
    @DisplayName("Una cuenta restaurada conserva el saldo persistido sin recalcularlo")
    void restauraElSaldoPersistido() {
      // Arrange / Act
      UserCredit restored = UserCredit.restore(BUYER_ID, 3_400L);

      // Assert
      assertThat(restored.getAvailableCredits()).isEqualTo(3_400L);
    }

    @Test
    @DisplayName("Una cuenta restaurada conserva su marca de tiempo original")
    void restauraLaMarcaDeTiempoPersistida() {
      // Arrange
      OffsetDateTime emitted = OffsetDateTime.parse("2026-10-14T10:30:00Z");

      // Act
      UserCredit restored = UserCredit.restore(BUYER_ID, 500L, emitted);

      // Assert
      assertThat(restored.getUpdatedAt()).isEqualTo(emitted);
    }

    @Test
    @DisplayName("Un saldo negativo no puedeuir una cuenta válida")
    void rechazaConstruirUnaCuentaNegativa() {
      // Assert
      assertThatThrownBy(() -> UserCredit.restore(BUYER_ID, -1L))
          .isInstanceOf(InvalidCreditAmountException.class);
    }
  }

  @Nested
  @DisplayName("Débito")
  class Debit {
    @Test
    @DisplayName("Un débito mayor al saldo disponible es rechazado")
    void rechazaDebitarMasQueElSaldo() {
      // Arrange
      UserCredit credit = UserCredit.restore(BUYER_ID, 500L);

      // Act / Assert
      assertThatThrownBy(() -> credit.debit(501L))
          .isInstanceOf(InsufficientUserCreditsException.class);
      assertThat(credit.getAvailableCredits()).isEqualTo(500L);
    }

    @Test
    @DisplayName("Un débito válido descuenta exactamente el importe y nunca deja saldo negativo")
    void descuentaElImporteSolicitado() {
      // Arrange
      UserCredit credit = UserCredit.restore(BUYER_ID, 1_000L);

      // Act
      credit.debit(250L);

      // Assert
      assertThat(credit.getAvailableCredits()).isEqualTo(750L);
      assertThat(credit.getAvailableCredits()).isNotNegative();
    }

    @Test
    @DisplayName("Un débito que deja el saldo exactamente en cero es válido")
    void permiteDejarElSaldoEnCero() {
      // Arrange
      UserCredit credit = UserCredit.restore(BUYER_ID, 1_000L);

      // Act
      credit.debit(1_000L);

      // Assert
      assertThat(credit.getAvailableCredits()).isZero();
    }

    @Test
    @DisplayName("Un importe de débito negativo no representa una operación válida")
    void rechazaDebitarUnImporteNegativo() {
      // Arrange
      UserCredit credit = UserCredit.restore(BUYER_ID, 1_000L);

      // Act / Assert
      assertThatThrownBy(() -> credit.debit(-1L)).isInstanceOf(InvalidCreditAmountException.class);
    }
  }

  @Nested
  @DisplayName("Crédito")
  class Credit {
    @Test
    @DisplayName("Un crédito que desborda conserva el saldo y su marca de tiempo")
    void rechazaOverflowSinModificarLaCuenta() {
      // Arrange
      OffsetDateTime updatedAt = OffsetDateTime.parse("2026-10-14T10:30:00Z");
      UserCredit credit = UserCredit.restore(BUYER_ID, Long.MAX_VALUE - 50L, updatedAt);

      // Act / Assert
      assertThatThrownBy(() -> credit.credit(100L)).isInstanceOf(ArithmeticException.class);
      assertThat(credit.getAvailableCredits()).isEqualTo(Long.MAX_VALUE - 50L);
      assertThat(credit.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("Una acreditación que alcanza exactamente Long.MAX_VALUE es válida")
    void permiteAcreditarHastaElLimite() {
      // Arrange
      UserCredit credit = UserCredit.restore(BUYER_ID, Long.MAX_VALUE - 100L);

      // Act
      credit.credit(100L);

      // Assert
      assertThat(credit.getAvailableCredits()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @DisplayName("Un crédito suma el importe sin que el saldo pueda quedar negativo")
    void sumaElImporteRecibido() {
      // Arrange
      UserCredit credit = UserCredit.restore(BUYER_ID, 0L);

      // Act
      credit.credit(4_200L);

      // Assert
      assertThat(credit.getAvailableCredits()).isEqualTo(4_200L);
      assertThat(credit.getAvailableCredits()).isNotNegative();
    }

    @Test
    @DisplayName("Un importe de crédito negativo no representa una operación válida")
    void rechazaAcreditarUnImporteNegativo() {
      // Arrange
      UserCredit credit = UserCredit.restore(BUYER_ID, 1_000L);

      // Act / Assert
      assertThatThrownBy(() -> credit.credit(-1L)).isInstanceOf(InvalidCreditAmountException.class);
    }
  }

  @Nested
  @DisplayName("Cobertura del saldo")
  class Coverage {
    @Test
    @DisplayName("El saldo cubre exactamente el importe disponible y no uno mayor")
    void informaCoberturaExactaDelSaldo() {
      // Arrange
      UserCredit credit = UserCredit.restore(BUYER_ID, 1_000L);

      // Act / Assert
      assertThat(credit.covers(1_000L)).isTrue();
      assertThat(credit.covers(1_001L)).isFalse();
      assertThat(credit.covers(0L)).isTrue();
    }
  }
}
