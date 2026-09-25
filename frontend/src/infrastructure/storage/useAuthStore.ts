import { create } from "zustand";
import type { QueryClient } from "@tanstack/react-query";
import { authSessionStorage } from "./authSessionStorage";
import { type AuthSession, type AuthStore, AuthStatus } from "./types";

let queryClient: QueryClient | null = null;

export function configureAuthQueryClient(client: QueryClient) {
  queryClient = client;
}

function clearRemoteState() {
  if (!queryClient) throw new Error("Auth query client is not configured");
  void queryClient.cancelQueries();
  queryClient.clear();
}

export const useAuthStore = create<AuthStore>((set, get) => ({
  status: AuthStatus.UNKNOWN,
  verificationError: null,
  session: null,
  revision: 0,

  restoreSession: () => {
    if (get().status !== AuthStatus.UNKNOWN) return;
    const session = authSessionStorage.restore();
    if (session) clearRemoteState();
    set({ session, status: session ? AuthStatus.CHECKING : AuthStatus.ANONYMOUS,
      verificationError: null, revision: get().revision + (session ? 1 : 0) });
  },

  startSession: (session) => {
    authSessionStorage.save(session);
    clearRemoteState();
    set({
      session,
      revision: get().revision + 1,
      status: AuthStatus.CHECKING,
      verificationError: null,
    });
  },

  endSession: () => {
    authSessionStorage.clear();
    clearRemoteState();
    set({
      session: null,
      revision: get().revision + 1,
      status: AuthStatus.ANONYMOUS,
      verificationError: null,
    });
  },

  confirmSession: (session: AuthSession) => {
    if (get().session === session && get().status === AuthStatus.CHECKING)
      set({ status: AuthStatus.AUTHENTICATED, verificationError: null });
  },
  checkSession: (session: AuthSession) => {
    if (get().session === session && get().status === AuthStatus.AUTHENTICATED)
      set({ status: AuthStatus.CHECKING, verificationError: null });
  },
  failVerification: (session: AuthSession, message: string) => {
    if (get().session === session && get().status === AuthStatus.CHECKING)
      set({ verificationError: message });
  },
}));
