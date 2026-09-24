import { createContext } from "react";
import type { AuthSession, AuthStatus } from "../types";

export interface AuthContextValue {
	status: AuthStatus;
	verificationError: string | null;
	retry: () => void;
	startSession: (session: AuthSession) => void;
	endSession: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
