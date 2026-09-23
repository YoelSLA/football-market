import { useQueryClient } from "@tanstack/react-query";
import { createContext, type ReactNode, useCallback, useContext, useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import { type AuthSession, type AuthStatus, isSessionValid } from "../models/AuthSession";
import { authSessionStorage } from "../services/AuthSessionStorage";
import { authQueryKeys } from "./authQueryKeys";
import { useCurrentUser } from "./useCurrentUser";

interface AuthContextValue {
  status: AuthStatus;
  verificationError: string | null;
  retry: () => void;
  startSession: (session: AuthSession) => void;
  endSession: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const location = useLocation();
  const [initialized, setInitialized] = useState(false);
  const [session, setSession] = useState<AuthSession | null>(null);
  const [revision, setRevision] = useState(0);
  const retrying = useRef(false);
  const currentUser = useCurrentUser(initialized && session !== null, revision, location.key);

  const endSession = useCallback(() => {
    authSessionStorage.clear();
    setSession(null);
    setRevision((value) => value + 1);
    void queryClient.cancelQueries();
    queryClient.clear();
  }, [queryClient]);

  useEffect(() => {
    const disconnect = authSessionStorage.connect(endSession);
    setSession(authSessionStorage.restore());
    setInitialized(true);
    return disconnect;
  }, [endSession]);

  useEffect(() => {
    if (!session) return;
    let timer: ReturnType<typeof setTimeout>;
    const checkExpiry = () => {
      clearTimeout(timer);
      if (!isSessionValid(session)) endSession();
      else timer = setTimeout(checkExpiry, Math.min(session.expiresAt - Date.now(), 2_147_483_647));
    };
    checkExpiry();
    window.addEventListener("focus", checkExpiry);
    document.addEventListener("visibilitychange", checkExpiry);
    return () => {
      clearTimeout(timer);
      window.removeEventListener("focus", checkExpiry);
      document.removeEventListener("visibilitychange", checkExpiry);
    };
  }, [session, endSession]);

  const startSession = useCallback((nextSession: AuthSession) => {
    authSessionStorage.save(nextSession);
    void queryClient.cancelQueries({ queryKey: authQueryKeys.all });
    queryClient.removeQueries({ queryKey: authQueryKeys.all });
    setRevision((value) => value + 1);
    setSession(nextSession);
  }, [queryClient]);

  const retry = () => {
    if (!session || retrying.current || currentUser.isFetching) return;
    if (!isSessionValid(session)) { endSession(); return; }
    retrying.current = true;
    void currentUser.refetch().finally(() => { retrying.current = false; });
  };

  const status: AuthStatus = !initialized ? "unknown"
    : !session || !isSessionValid(session) ? "anonymous"
    : currentUser.isSuccess && !currentUser.isFetching ? "authenticated" : "checking";

  return (
    <AuthContext.Provider value={{
      status,
      verificationError: currentUser.isError && !currentUser.isFetching ? currentUser.error.message : null,
      retry,
      startSession,
      endSession,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth requires AuthProvider");
  return context;
}
