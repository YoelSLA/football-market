import { type ReactNode, useEffect } from "react";
import { useLocation } from "react-router-dom";
import { useCurrentUserQuery } from "@/features/auth";
import { getErrorMessage } from "@/infrastructure/http";
import { authSessionStorage, isSessionValid, useAuthStore } from "@/infrastructure/storage";

export default function SessionLifecycle({ children }: { children: ReactNode }) {
  const location = useLocation();
  const session = useAuthStore((state) => state.session);
  const revision = useAuthStore((state) => state.revision);
  const restoreSession = useAuthStore((state) => state.restoreSession);
  const endSession = useAuthStore((state) => state.endSession);
  const confirmSession = useAuthStore((state) => state.confirmSession);
  const checkSession = useAuthStore((state) => state.checkSession);
  const failVerification = useAuthStore((state) => state.failVerification);
  const currentUser = useCurrentUserQuery(session !== null, revision, location.key);

  useEffect(() => { restoreSession(); }, [restoreSession]);

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

  useEffect(() => {
    if (!session || authSessionStorage.getSession() !== session) return;
    if (!isSessionValid(session)) { endSession(); return; }
    if (currentUser.isFetching) { checkSession(session); return; }
    if (currentUser.isSuccess) confirmSession(session);
    else if (currentUser.isError)
      failVerification(session, getErrorMessage(currentUser.error, "No pudimos comprobar tu sesión. Vuelve a intentarlo."));
  }, [session, revision, location.key, currentUser.isFetching, currentUser.isSuccess, currentUser.isError, currentUser.error, checkSession, confirmSession, failVerification, endSession]);

  return children;
}
