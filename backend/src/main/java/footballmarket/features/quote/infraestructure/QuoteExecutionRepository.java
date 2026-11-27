package footballmarket.features.quote.infraestructure;

import footballmarket.features.quote.domain.models.QuoteExecution;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Contextos de corrida y consultas de sus resúmenes finalizados. */
public interface QuoteExecutionRepository extends JpaRepository<QuoteExecution, Long> {

  /**
   * Última ejecución persistida según su fecha de cotización.
   *
   * @return última ejecución, o vacío cuando todavía no existe ninguna
   */
  Optional<QuoteExecution> findTopByOrderByQuoteDateDesc();

  /** Última ejecución finalizada, ordenada por inicio e identificador descendentes. */
  Optional<QuoteExecution> findTopByFinishedAtIsNotNullOrderByStartedAtDescIdDesc();

  /** Resúmenes finalizados de una fecha, con orden total incluso ante inicios iguales. */
  List<QuoteExecution> findAllByQuoteDateAndFinishedAtIsNotNullOrderByStartedAtDescIdDesc(
      LocalDate quoteDate);
}
