package footballmarket.features.market.infraestructure.config;

import footballmarket.features.market.application.exceptions.InvalidOfficialBuybackConfigurationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de la comisión aplicada a la venta oficial inmediata.
 *
 * <p>El porcentaje es un entero entre 10 y 99 inclusive. Se descuenta de la cotización vigente para
 * obtener el precio neto que recibe el vendedor. Un valor fuera del rango impide el arranque, de
 * modo que ninguna venta oficial se ejecute con una configuración inválida.
 *
 * @param discountPercentage comisión aplicada, en puntos porcentuales enteros; se conserva el
 *     nombre de propiedad por compatibilidad
 */
@ConfigurationProperties(prefix = "market.buyback")
public record OfficialBuybackProperties(int discountPercentage) {

  /**
   * Valida el rango inclusivo de la comisión configurada.
   *
   * @throws InvalidOfficialBuybackConfigurationException si la comisión está fuera de 10–99
   */
  public OfficialBuybackProperties {
    if (discountPercentage < 10 || discountPercentage > 99) {
      throw new InvalidOfficialBuybackConfigurationException(
          "La comisión de la venta oficial debe estar entre 10 y 99.");
    }
  }
}
