export enum AuthStatus {
  UNKNOWN = "unknown",
  CHECKING = "checking",
  AUTHENTICATED = "authenticated",
  ANONYMOUS = "anonymous",
}

export type AuthSession = {
  token: string;
  expiresAt: number;
};

export type AuthStore = {
  status: AuthStatus;
  verificationError: string | null;
  session: AuthSession | null;
  revision: number;
  restoreSession: () => void;
  startSession: (session: AuthSession) => void;
  endSession: () => void;
  confirmSession: (session: AuthSession) => void;
  checkSession: (session: AuthSession) => void;
  failVerification: (session: AuthSession, message: string) => void;
};
