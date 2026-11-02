package footballmarket.features.catalog.application.internal;

import footballmarket.features.identity.domain.enums.ReviewSubjectType;

/**
 * Identidad externa que se está escribiendo dentro del intento transaccional en curso.
 *
 * <p>Solo existe para acotar el replay ante una violación de unicidad: si la transacción se
 * revierte, el servicio coordinador necesita saber qué candidato la provocó. Se sobrevive al
 * rollback porque no forma parte del estado persistente.
 */
public final class SynchronizationAttempt {
  private ReviewSubjectType type;
  private String externalId;

  /** Marca el candidato cuya escritura sigue en curso. */
  public void set(ReviewSubjectType type, String externalId) {
    this.type = type;
    this.externalId = externalId;
  }

  /** Libera la marca cuando la escritura terminó correctamente. */
  public void clear() {
    this.type = null;
    this.externalId = null;
  }

  public ReviewSubjectType type() {
    return this.type;
  }

  public String externalId() {
    return this.externalId;
  }
}
