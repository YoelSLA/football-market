package footballmarket.models.records;

import java.time.OffsetDateTime;

/**
 * Instantánea de la cotización vigente de un jugador.
 *
 * <p>El precio se expresa en centésimas de crédito conforme a CL-01: {@code 100} centésimas
 * equivalen a 1 crédito. {@code calculatedAt} es el instante en que la cotización fue calculada, no
 * el de la lectura ni el de la respuesta. {@code strategyVersion} admite {@code null}: la fuente
 * real puede no proporcionarla y en ese caso se conserva la ausencia.
 *
 * @param playerId jugador al que corresponde la cotización
 * @param price precio de un token en centésimas de crédito; debe ser mayor que cero
 * @param calculatedAt instante de cálculo de la cotización, en UTC
 * @param strategyVersion estrategia que produjo la cotización, o {@code null} si no se proporciona
 */
public record CurrentQuote(
    Long playerId, long price, OffsetDateTime calculatedAt, String strategyVersion) {}
