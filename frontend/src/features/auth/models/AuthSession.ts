export interface AuthSession {
  token: string;
  expiresAt: number;
}

export type AuthStatus = "unknown" | "checking" | "authenticated" | "anonymous";

export function isSessionValid(session: AuthSession, now = Date.now()): boolean {
  return Number.isFinite(session.expiresAt) && session.expiresAt > now;
}
