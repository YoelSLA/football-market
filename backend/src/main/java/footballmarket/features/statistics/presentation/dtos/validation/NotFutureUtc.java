package footballmarket.features.statistics.presentation.dtos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/** Valida una fecha opcional contra el día UTC, independientemente de la zona del servidor. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotFutureUtcValidator.class)
public @interface NotFutureUtc {
  String message() default "La fecha de extracción no puede ser futura";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
