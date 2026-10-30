package footballmarket.features.statistics.presentation.dtos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Usa el reloj de validación configurable y compara exclusivamente en UTC. */
public class NotFutureUtcValidator implements ConstraintValidator<NotFutureUtc, LocalDate> {
  @Override
  public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
    return value == null
        || !value.isAfter(
            LocalDate.now(context.getClockProvider().getClock().withZone(ZoneOffset.UTC)));
  }
}
