import type { PositionFilter } from "@/features/portfolio/types";

export const portfolioLabels = {
	error: "No pudimos cargar tu portfolio. Vuelve a intentarlo.",
	loading: "Cargando tu portfolio…",
	missing: "No disponible",
	empty: "Todavía no tienes posiciones en tu cartera.",
	filters: [
		{ value: "ALL", label: "Todas" },
		{ value: "OPEN", label: "Abiertas" },
		{ value: "CLOSED", label: "Cerradas" },
	] satisfies { value: PositionFilter; label: string }[],
};
