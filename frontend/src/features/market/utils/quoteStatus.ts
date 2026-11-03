import type { MarketQuoteStatus } from "@/features/market/types";

export function getMarketQuoteMessage(
	status: MarketQuoteStatus,
	httpStatus?: number,
): string {
	switch (status) {
		case "available":
			return "";
		case "loading":
			return "Consultando cotización…";
		case "paused":
			return "La consulta está en pausa por falta de conexión.";
		case "missing":
			return "No hay cotización disponible para este jugador.";
		case "authentication-error":
			return "La sesión no está disponible o venció. Volvé a iniciar sesión.";
		case "forbidden":
			return "No tenés permiso para consultar esta cotización.";
		case "http-error":
			return `No se pudo consultar la cotización${httpStatus ? ` (HTTP ${httpStatus})` : ""}.`;
		case "network-error":
			return "No se pudo conectar con el servidor de cotizaciones.";
		case "invalid-response":
			return "El servidor devolvió una cotización inválida. No se puede comprar.";
	}
}
