import { useQueryClient } from "@tanstack/react-query";
import {
  type ReactNode,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { useLocation } from "react-router-dom";
import { getErrorMessage } from "@/infrastructure/http";
import { AuthContext } from "../context";
import { useCurrentUserQuery } from "../hooks/queries";
import { authSessionStorage } from "../services";
import type { AuthSession, AuthStatus } from "../types";
import { isSessionValid } from "../utils";

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const location = useLocation();
  const [initialized, setInitialized] = useState(false);
  const [session, setSession] = useState<AuthSession | null>(null);
  const [revision, setRevision] = useState(0);
  const retrying = useRef(false);
  const currentUser = useCurrentUserQuery(
    initialized && session !== null,
    revision,
    location.key,
  );

  const endSession = useCallback(() => {
    authSessionStorage.clear();
    setSession(null);
    setRevision((value) => value + 1);
    void queryClient.cancelQueries();
    queryClient.clear();
  }, [queryClient]);

  useEffect(() => {
    setSession(authSessionStorage.restore());
    setInitialized(true);
  }, []);

  useEffect(() => {
    if (!session) return;
    let timer: ReturnType<typeof setTimeout>;
    const checkExpiry = () => {
      clearTimeout(timer);
      if (!isSessionValid(session)) endSession();
      else
        timer = setTimeout(
          checkExpiry,
          Math.min(session.expiresAt - Date.now(), 2_147_483_647),
        );
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

  const startSession = useCallback(
    (nextSession: AuthSession) => {
      authSessionStorage.save(nextSession);
      void queryClient.cancelQueries();
      queryClient.clear();
      setRevision((value) => value + 1);
      setSession(nextSession);
    },
    [queryClient],
  );

  const retry = () => {
    if (!session || retrying.current || currentUser.isFetching) return;
    if (!isSessionValid(session)) {
      endSession();
      return;
    }
    retrying.current = true;
    void currentUser.refetch().finally(() => {
      retrying.current = false;
    });
  };

  const status: AuthStatus = !initialized
    ? "unknown"
    : !session || !isSessionValid(session)
      ? "anonymous"
      : currentUser.isSuccess && !currentUser.isFetching
        ? "authenticated"
        : "checking";

  return (
    <AuthContext.Provider
      value={{
        status,
        verificationError:
          currentUser.isError && !currentUser.isFetching
            ? getErrorMessage(
              currentUser.error,
              "No pudimos comprobar tu sesión. Vuelve a intentarlo.",
            )
            : null,
        retry,
        startSession,
        endSession,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
