/** Constantes de la fuente de datos de demostración del detalle y del catálogo. */

/** Temporada deportiva fija de demostración. No hay selector ni cambio automático. */
export const DEMO_SEASON = "2025/2026";

/**
 * Inicio del período financiero de demostración, independiente del reloj del navegador
 * y del inicio efectivo de la temporada deportiva.
 */
export const DEMO_QUOTE_PERIOD_START = "2026-02-01";

/** Cantidad de cotizaciones diarias del caso normal. */
export const DEMO_QUOTE_DAYS = 30;

/** Etiqueta visible de procedencia de todo bloque simulado. */
export const DEMO_DATA_LABEL = "Datos de demostración";

/** Unidad de cotización. Nunca valor de transferencia ni precio operativo. */
export const DEMO_QUOTE_UNIT_LABEL = "Créditos";

/** Fechas consecutivas del período fijo, en formato yyyy-MM-dd. */
export const DEMO_QUOTE_DATES: readonly string[] = Array.from(
	{ length: DEMO_QUOTE_DAYS },
	(_, index) => {
		const start = Date.parse(`${DEMO_QUOTE_PERIOD_START}T00:00:00Z`);

		return new Date(start + index * 86_400_000).toISOString().slice(0, 10);
	},
);

export const DEMO_MESSAGES = {
	notFound: "Jugador no encontrado",
	invalidId: "Identificador de jugador inválido",
	emptySeries: "No hay historial de cotizaciones disponible",
	breadcrumbRoot: "Jugadores",
	singlePoint: "Solo hay una cotización disponible",
	percentageUnavailable: "Variación porcentual no disponible",
	retry: "Reintentar",
	backToCatalog: "Volver al catálogo",
	genericPortraitAlt: "Icono genérico del jugador",
	playerPortraitAlt: "Fotografía de {name}",
} as const;
