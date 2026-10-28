package footballmarket.features.statistics.infraestructure;

import footballmarket.features.statistics.domain.models.ExtractionIncident;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso al historial de incidencias saneadas de un intento. */
public interface ExtractionIncidentRepository extends JpaRepository<ExtractionIncident, Long> {}
