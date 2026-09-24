import { browserStorage } from "@/infrastructure/storage";
import type { AuthSession } from "../types";
import { getJwtExpiration, isSessionValid } from "../utils";

const sessionKey = "football-market.auth.token";
let currentSession: AuthSession | null = null;

export const authSessionStorage = {
	getSession(): AuthSession | null {
		return currentSession;
	},
	restore(): AuthSession | null {
		try {
			const token = browserStorage.get(sessionKey);
			const session = token
				? { token, expiresAt: getJwtExpiration(token) }
				: null;
			if (session && isSessionValid(session)) {
				currentSession = session;
				return session;
			}
		} catch {
			// Un almacenamiento inaccesible no permite restaurar una sesión.
		}
		this.clear();
		return null;
	},
	save(session: AuthSession): void {
		if (!isSessionValid(session)) throw new Error("Invalid auth session");
		browserStorage.set(sessionKey, session.token);
		currentSession = session;
	},
	clear(): void {
		currentSession = null;
		try {
			browserStorage.remove(sessionKey);
		} catch {
			// La sesión en memoria se invalida también si el navegador bloquea el almacenamiento.
		}
	},
};
