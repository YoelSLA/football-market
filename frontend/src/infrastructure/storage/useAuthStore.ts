import { create } from "zustand";
import { authSessionStorage } from "./authSessionStorage";
import { AuthStore, AuthStatus } from "./types";

export const useAuthStore = create<AuthStore>((set) => ({
  status: AuthStatus.ANONYMOUS,
  verificationError: null,

  startSession: (session) => {
    authSessionStorage.save(session);

    set({
      status: AuthStatus.AUTHENTICATED,
      verificationError: null,
    });
  },

  endSession: () => {
    authSessionStorage.clear();

    set({
      status: AuthStatus.ANONYMOUS,
      verificationError: null,
    });
  },
}));