package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Lista de resúmenes correspondientes a la fecha consultada.
 *
 * @param executions ejecuciones de la fecha de cotización; como máximo una, y vacía si la fecha no
 *     tiene ejecución
 */
@Schema(
    name = "QuoteExecutionsResponse",
    description = "Ejecuciones de cotización de la fecha consultada")
public record QuoteExecutionsResponseDTO(
    @Schema(description = "Ejecuciones de la fecha de cotización consultada")
        List<QuoteExecutionResponseDTO> executions) {}
