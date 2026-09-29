import { AuthSession } from "@/infrastructure/storage/types";

export function isSessionValid(
	session: AuthSession,
	now = Date.now(),
): boolean {
	return Number.isFinite(session.expiresAt) && session.expiresAt > now;
}

export function getJwtExpiration(token: string): number {
	const parts = token.split(".");
	if (parts.length !== 3 || parts.some((part) => !/^[\w-]+$/.test(part)))
		throw new Error("Invalid JWT structure");
	try {
		const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
		const decoded: unknown = JSON.parse(
			atob(payload.padEnd(Math.ceil(payload.length / 4) * 4, "=")),
		);
		if (
			typeof decoded !== "object" ||
			decoded === null ||
			!("exp" in decoded) ||
			typeof decoded.exp !== "number" ||
			!Number.isSafeInteger(decoded.exp) ||
			decoded.exp < 0 ||
			!Number.isSafeInteger(decoded.exp * 1000)
		)
			throw new Error("Invalid JWT expiration");
		return decoded.exp * 1000;
	} catch {
		throw new Error("Invalid JWT payload or expiration");
	}
}
