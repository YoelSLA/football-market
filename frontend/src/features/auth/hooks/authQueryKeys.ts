export const authQueryKeys = {
  all: ["auth"] as const,
  currentUser: (revision: number, navigation: string) => ["auth", "currentUser", revision, navigation] as const,
};
