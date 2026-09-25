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
  startSession: (session: AuthSession) => void;
  endSession: () => void;
};