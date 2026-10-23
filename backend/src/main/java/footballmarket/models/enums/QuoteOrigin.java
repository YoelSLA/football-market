package footballmarket.models.enums;

/** Origen de una fila del historial de cotizaciones. */
public enum QuoteOrigin {
  /** Fila de ancla de 1 crédito, sin estrategia ni score. */
  INITIAL,
  /** Fila cuyo precio surge de aplicar el multiplicador a un score disponible. */
  RECOMPUTED,
  /** Fila sin valuación para la estrategia elegida: el precio vigente se repite. */
  CARRIED_FORWARD
}
