/**
 * Modelos de dominio internos de la feature Market (SPEC-019).
 * No importa types de players ni portfolio.
 */

export interface MarketPlayer {
	id: number;
	name: string;
	teamName: string;
	leagueName: string;
	position: string;
	imageUrl: string | null;
	fallbackImageUrl: string | null;
}

export interface PlayerHolding extends MarketPlayer {
	totalQuantity: number;
	availableQuantity: number;
	reservedQuantity: number;
}

export interface MarketQuote {
	price: number; // en centésimas
	effectiveDate: string; // fecha de vigencia
	calculatedAt: string; // instante de cálculo
	previousPrice?: number | null; // precio anterior en centésimas, nulo en la cotización inicial
}

export type MarketQuoteStatus =
	| "loading"
	| "paused"
	| "available"
	| "missing"
	| "authentication-error"
	| "forbidden"
	| "http-error"
	| "network-error"
	| "invalid-response";

export type MarketQuoteFailure = Exclude<
	MarketQuoteStatus,
	"loading" | "paused" | "available" | "missing"
>;

/** Error de lectura seguro: no conserva credenciales ni cuerpos HTTP. */
export class MarketQuoteReadError extends Error {
	constructor(
		readonly reason: MarketQuoteFailure,
		readonly httpStatus?: number,
	) {
		super("No se pudo obtener una cotización válida.");
		this.name = "MarketQuoteReadError";
	}
}

export interface BuyResult {
	transactionId: number;
	playerId: number;
	quantity: number;
	unitPrice: number;
	totalAmount: number;
	executedAt: string;
	listingId?: number;
}

export interface BuyEstimation {
	quantity: number;
	unitPriceCentimes: number;
	totalCentimes: number;
}

export interface BuyQuantityForm {
	quantity: string;
}

export interface SellResult {
	listingId: number;
	playerId: number;
	quantity: number;
	unitPrice: number;
	totalAmount: number;
	status: string;
	createdAt: string;
	expiresAt: string;
}

export interface SellEstimation {
	quantity: number;
	unitPriceCentimes: number;
	totalCentimes: number;
}

export interface SellQuantityForm {
	quantity: string;
}

export type MarketTab = "official" | "community";

export interface MarketListing {
	listingId: number;
	sellerId: number;
	playerId: number;
	quantity: number;
	unitPrice: number;
	totalAmount: number;
	createdAt: string;
	expiresAt: string;
}

export interface CommunityMarketPlayer extends MarketPlayer {
	listings: MarketListing[];
	listingCount: number;
	availableQuantity: number;
	minimumUnitPrice: number;
}

export type CommunityMarketSort = "price" | "quantity" | "name";

export interface CommunityMarketFilters {
	search: string;
	league: string;
	position: string;
	sort: CommunityMarketSort;
}
