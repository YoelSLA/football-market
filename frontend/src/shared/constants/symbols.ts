/**
 * Símbolos canónicos de unidades económicas de la plataforma.
 */

/** Símbolo de la moneda interna de la plataforma (Créditos) */
export const CREDIT_SYMBOL = "₵" as const;

/** Símbolo del activo deportivo transaccionable (Tokens de jugadores) */
export const TOKEN_SYMBOL = "Ⓣ" as const;

/**
 * Catálogo agrupado para desestructurar o mapear dinámicamente.
 */
export const APP_SYMBOLS = {
	CREDIT: CREDIT_SYMBOL,
	TOKEN: TOKEN_SYMBOL,
} as const;

/**
 * Textos y etiquetas de unidades con soporte singular / plural.
 */
export const UNIT_LABELS = {
	CREDIT: {
		symbol: CREDIT_SYMBOL,
		singular: "crédito",
		plural: "créditos",
	},
	TOKEN: {
		symbol: TOKEN_SYMBOL,
		singular: "token",
		plural: "tokens",
	},
} as const;
