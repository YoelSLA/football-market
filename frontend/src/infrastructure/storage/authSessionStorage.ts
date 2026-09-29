import { browserStorage } from "./browserStorage";
import type { AuthSession } from "./types";
import { getJwtExpiration, isSessionValid } from "./utils";

const SESSION_KEY = "football-market.auth.token";

let currentSession: AuthSession | null = null;

export const authSessionStorage = {
  getSession(): AuthSession | null {
    return currentSession;
  },

  restore(): AuthSession | null {
    try {
      const token = browserStorage.get(SESSION_KEY);

      if (!token) {
        return null;
      }

      const session: AuthSession = {
        token,
        expiresAt: getJwtExpiration(token),
      };

      if (!isSessionValid(session)) {
        this.clear();
        return null;
      }

      currentSession = session;

      return session;
    } catch {
      this.clear();
      return null;
    }
  },

  save(session: AuthSession): void {
    if (!isSessionValid(session)) {
      throw new Error("Invalid auth session");
    }

    browserStorage.set(SESSION_KEY, session.token);
    currentSession = session;
  },

  clear(): void {
    currentSession = null;

    try {
      browserStorage.remove(SESSION_KEY);
    } catch {
      // La sesión en memoria queda invalidada aunque falle localStorage.
    }
  },
};