export const authQueryKeys = {
	all: ["auth"] as const,
	currentUser: (revision: number) => ["auth", "currentUser", revision] as const,
};
