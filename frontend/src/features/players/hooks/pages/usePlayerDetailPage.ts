import { useCallback, useEffect, useState } from "react";
import { getErrorCode, getErrorMessage } from "@/infrastructure/http";
import {
	usePlayerDetailQuery,
	usePlayerSampleSeasonQuery,
	usePlayerTokenQuoteQuery,
} from "../queries";

export type PlayerDetailState =
	| "loading"
	| "invalid-id"
	| "not-found"
	| "error"
	| "ready";

function parsePlayerId(rawId: string | undefined): number | null {
	if (!rawId || !/^\d+$/.test(rawId)) return null;

	const parsed = Number(rawId);

	return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

export function usePlayerDetailPage(rawId: string | undefined) {
	const [playerId, setPlayerId] = useState(parsePlayerId(rawId));
	const [invalid, setInvalid] = useState<boolean>(
		parsePlayerId(rawId) === null,
	);

	useEffect(() => {
		const parsed = parsePlayerId(rawId);

		setPlayerId(parsed);
		setInvalid(parsed === null);
	}, [rawId]);

	const detail = usePlayerDetailQuery(playerId ?? 0, playerId !== null);
	const sampleSeason = usePlayerSampleSeasonQuery(
		detail.data?.id,
		detail.isSuccess,
	);
	const tokenQuote = usePlayerTokenQuoteQuery(
		detail.data?.id,
		detail.isSuccess,
	);

	// Descarta cualquier dato que corresponda a una consulta anterior: solo se presenta
	// la identidad del identificador vigente.
	const visibleId = playerId;
	const isCurrent = detail.data?.id === visibleId;
	const player = isCurrent ? detail.data : undefined;
	const retry = useCallback(() => void detail.refetch(), [detail]);

	const state: PlayerDetailState = invalid
		? "invalid-id"
		: player
			? "ready"
			: detail.isPending
				? "loading"
				: getErrorCode(detail.error) === "PLAYER_NOT_FOUND"
					? "not-found"
					: "error";

	const errorMessage =
		state === "error"
			? getErrorMessage(
					detail.error,
					"No pudimos consultar el jugador. Vuelve a intentarlo.",
				)
			: null;

	return {
		state,
		player,
		sampleSeason: sampleSeason.data,
		tokenQuote: tokenQuote.data,
		errorMessage,
		retry,
	};
}
