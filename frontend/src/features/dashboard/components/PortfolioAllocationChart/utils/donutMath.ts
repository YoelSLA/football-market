export interface DonutSegment {
	key: string;
	label: string;
	value: number;
	percentage: number;
	tokenCount: number;
	startAngle: number;
	endAngle: number;
	path: string;
	color: string;
}

export function degreesToRadians(degrees: number): number {
	return (degrees * Math.PI) / 180;
}

export function polarToCartesian(
	centerX: number,
	centerY: number,
	radius: number,
	angleInRadians: number,
): { x: number; y: number } {
	return {
		x: centerX + radius * Math.cos(angleInRadians),
		y: centerY + radius * Math.sin(angleInRadians),
	};
}

export function createDonutArc(
	cx: number,
	cy: number,
	innerRadius: number,
	outerRadius: number,
	startAngle: number,
	endAngle: number,
): string {
	// Truncar si es casi 360 para evitar degeneración en SVG
	const angleDiff = endAngle - startAngle;
	const effectiveEndAngle =
		angleDiff >= 2 * Math.PI ? startAngle + 2 * Math.PI - 0.0001 : endAngle;

	const p1 = polarToCartesian(cx, cy, outerRadius, startAngle);
	const p2 = polarToCartesian(cx, cy, outerRadius, effectiveEndAngle);
	const p3 = polarToCartesian(cx, cy, innerRadius, effectiveEndAngle);
	const p4 = polarToCartesian(cx, cy, innerRadius, startAngle);

	const largeArcFlag = angleDiff > Math.PI ? 1 : 0;

	return [
		`M ${p1.x} ${p1.y}`,
		`A ${outerRadius} ${outerRadius} 0 ${largeArcFlag} 1 ${p2.x} ${p2.y}`,
		`L ${p3.x} ${p3.y}`,
		`A ${innerRadius} ${innerRadius} 0 ${largeArcFlag} 0 ${p4.x} ${p4.y}`,
		"Z",
	].join(" ");
}

const PALETTE = [
	"#00dfa0", // accent-gain
	"#38bdf8", // cyan
	"#818cf8", // indigo
	"#fbbf24", // amber
	"#f472b6", // rose
	"#a78bfa", // purple
	"#34d399", // emerald
	"#94a3b8", // slate
];

const SEMANTIC_COLORS: Record<string, string> = {
	// Posiciones
	DELANTERO: "#f43f5e",
	Delanteros: "#f43f5e",
	MEDIOCAMPISTA: "#818cf8",
	Mediocampistas: "#818cf8",
	DEFENSOR: "#34d399",
	Defensores: "#34d399",
	ARQUERO: "#fbbf24",
	Arqueros: "#fbbf24",

	// Las 5 grandes ligas
	PREMIER_LEAGUE: "#8b5cf6",
	"Premier League": "#8b5cf6",
	LALIGA: "#ea580c",
	LaLiga: "#ea580c",
	"La Liga": "#ea580c",
	"Primera Division": "#ea580c",
	"Primera División": "#ea580c",
	SERIE_A: "#0284c7",
	"Serie A": "#0284c7",
	BUNDESLIGA: "#dc2626",
	Bundesliga: "#dc2626",
	LIGUE_1: "#10b981",
	"Ligue 1": "#10b981",
	OTRAS_LIGAS: "#94a3b8",
	"Otras Ligas": "#94a3b8",
};

export function calculateDonutSegments(
	items: Array<{
		key: string;
		label: string;
		totalValue: number;
		percentage: number;
		tokenCount: number;
	}>,
	cx: number,
	cy: number,
	innerRadius: number,
	outerRadius: number,
): DonutSegment[] {
	if (!items || items.length === 0) return [];

	let currentAngle = -Math.PI / 2; // Empezar en las 12 en punto (-90 deg)
	const total = items.reduce((sum, item) => sum + item.totalValue, 0);

	return items.map((item, index) => {
		const sliceAngle =
			total > 0 ? (item.totalValue / total) * (2 * Math.PI) : 0;
		const startAngle = currentAngle;
		const endAngle = currentAngle + sliceAngle;
		currentAngle = endAngle;

		const path =
			sliceAngle > 0
				? createDonutArc(cx, cy, innerRadius, outerRadius, startAngle, endAngle)
				: "";

		const color =
			SEMANTIC_COLORS[item.key] ??
			SEMANTIC_COLORS[item.label] ??
			PALETTE[index % PALETTE.length];

		return {
			key: item.key,
			label: item.label,
			value: item.totalValue,
			percentage: item.percentage,
			tokenCount: item.tokenCount,
			startAngle,
			endAngle,
			path,
			color,
		};
	});
}
