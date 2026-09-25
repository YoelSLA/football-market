import { type ReactNode, useEffect } from "react";
import { configureHttpCredentials } from "@/infrastructure/http";
import { authSessionStorage, isSessionValid, useAuthStore } from "@/infrastructure/storage";

interface Props {
  children: ReactNode;
}

export default function HttpCredentials({ children }: Props) {
  const endSession = useAuthStore((state) => state.endSession);

  useEffect(
    () =>
      configureHttpCredentials({
        getToken: () => {
          const session = authSessionStorage.getSession();

          return session && isSessionValid(session) ? session.token : null;
        },
        onUnauthorized: endSession,
      }),
    [endSession],
  );

  return children;
}