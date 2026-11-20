/**
 * Valida y parsea el identificador del jugador desde los parámetros de ruta.
 * Retorna un entero positivo válido o null si es inválido.
 */
export function parsePlayerId(rawId: string | undefined): number | null {
	if (!rawId || !/^\d+$/.test(rawId)) return null;

	const parsed = Number(rawId);

	return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}
