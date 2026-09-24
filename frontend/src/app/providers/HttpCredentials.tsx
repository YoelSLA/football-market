import { type ReactNode, useEffect } from "react";
import { authSessionStorage, isSessionValid, useAuth } from "@/features/auth";
import { configureHttpCredentials } from "@/infrastructure/http";

interface Props {
  children: ReactNode;
}

export default function HttpCredentials({ children }: Props) {
  const { endSession } = useAuth();

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