import { browserStorage } from "@/infrastructure/storage";
import { configureHttpCredentials } from "@/shared/http";
import { toAuthSession } from "../mappers/authMapper";
import { type AuthSession, isSessionValid } from "../models/AuthSession";

const sessionKey = "football-market.auth.token";
let currentSession: AuthSession | null = null;

export const authSessionStorage = {
  restore(): AuthSession | null {
    try {
      const token = browserStorage.get(sessionKey);
      const session = token ? toAuthSession({ token }) : null;
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
    if (!isSessionValid(session)) throw new Error("La sesión recibida no es válida. Inicia sesión de nuevo.");
    try {
      browserStorage.set(sessionKey, session.token);
    } catch {
      throw new Error("No se pudo guardar la sesión. Permite el almacenamiento del navegador y vuelve a intentarlo.");
    }
    currentSession = session;
  },
  clear(): void {
    currentSession = null;
    try { browserStorage.remove(sessionKey); } catch {
      // La sesión en memoria se invalida también si el navegador bloquea el almacenamiento.
    }
  },
  connect(onUnauthorized: () => void): () => void {
    return configureHttpCredentials({
      getToken: () => currentSession && isSessionValid(currentSession) ? currentSession.token : null,
      onUnauthorized,
    });
  },
};
