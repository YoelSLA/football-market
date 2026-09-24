import type { AuthSession } from "../types";

export function isSessionValid(
	session: AuthSession,
	now = Date.now(),
): boolean {
	return Number.isFinite(session.expiresAt) && session.expiresAt > now;
}
