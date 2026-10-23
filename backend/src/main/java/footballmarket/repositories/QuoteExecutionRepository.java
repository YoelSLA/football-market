package footballmarket.repositories;

import footballmarket.models.QuoteExecution;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Resúmenes append-only de las corridas de cotización, uno por fecha de cotización. */
public interface QuoteExecutionRepository extends JpaRepository<QuoteExecution, Long> {

  /**
   * Resumen de la corrida de una fecha de cotización.
   *
   * @param quoteDate fecha de cotización de la corrida
   * @return resumen de la corrida, o vacío si la fecha no tiene ejecuciones
   */
  Optional<QuoteExecution> findByQuoteDate(LocalDate quoteDate);

  /**
   * Última ejecución persistida según su fecha de cotización.
   *
   * @return última ejecución, o vacío cuando todavía no existe ninguna
   */
  Optional<QuoteExecution> findTopByOrderByQuoteDateDesc();

  /** Resúmenes de una fecha, ordenados por inicio descendente. */
  List<QuoteExecution> findAllByQuoteDateOrderByStartedAtDesc(LocalDate quoteDate);
}
