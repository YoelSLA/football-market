package footballmarket.models.enums;

/** Estado de una publicación; EXPIRED solo se deriva en lectura y nunca se persiste. */
public enum TokenListingStatus {
  ACTIVE,
  SOLD,
  CANCELLED,
  EXPIRED
}
