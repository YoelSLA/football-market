package footballmarket.features.valuation.application.internal;

import footballmarket.features.valuation.domain.models.AttributeValuation;
import footballmarket.features.valuation.domain.models.PositionalValuation;
import footballmarket.features.valuation.infraestructure.AttributeValuationRepository;
import footballmarket.features.valuation.infraestructure.PositionalValuationRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Publica o retira el par de estrategias dentro de una única transacción por jugador. */
@Component
public class ValuationResultPublisher {
  private final PositionalValuationRepository positional;
  private final AttributeValuationRepository attributes;

  public ValuationResultPublisher(
      PositionalValuationRepository positional, AttributeValuationRepository attributes) {
    this.positional = positional;
    this.attributes = attributes;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void publish(
      PositionalValuation positionalValuation, AttributeValuation attributeValuation) {
    Long playerId = positionalValuation.getPlayer().getId();
    LocalDate date = positionalValuation.getValuationDate();
    this.positional.deleteByPlayerIdAndValuationDate(playerId, date);
    this.attributes.deleteByPlayerIdAndValuationDate(playerId, date);
    // El borrado se materializa antes del insert: Hibernate ejecuta los inserts antes que los
    // deletes del mismo flush y la unicidad por jugador, fecha y estrategia se violaría.
    this.positional.flush();
    this.attributes.flush();
    this.positional.save(positionalValuation);
    this.attributes.save(attributeValuation);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void remove(Long playerId, LocalDate date) {
    this.positional.deleteByPlayerIdAndValuationDate(playerId, date);
    this.attributes.deleteByPlayerIdAndValuationDate(playerId, date);
  }
}
