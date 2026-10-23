import { type ReactNode, useEffect } from "react";
import { useCurrentUserQuery } from "@/features/auth";
import { getErrorMessage } from "@/infrastructure/http";
import {
	authSessionStorage,
	isSessionValid,
	useAuthStore,
} from "@/infrastructure/storage";

interface Props {
	children: ReactNode;
}

export default function SessionLifecycle({ children }: Props) {
	const session = useAuthStore((state) => state.session);
	const revision = useAuthStore((state) => state.revision);
	const restoreSession = useAuthStore((state) => state.restoreSession);
	const endSession = useAuthStore((state) => state.endSession);
	const confirmSession = useAuthStore((state) => state.confirmSession);
	const checkSession = useAuthStore((state) => state.checkSession);
	const failVerification = useAuthStore((state) => state.failVerification);
	const status = useAuthStore((state) => state.status);
	const currentUser = useCurrentUserQuery(session !== null, revision);

	useEffect(() => {
		restoreSession();
	}, [restoreSession]);

	useEffect(() => {
		if (!session) return;
		let timer: ReturnType<typeof setTimeout>;
		const checkExpiry = () => {
			clearTimeout(timer);
			if (!isSessionValid(session)) endSession();
			else
				timer = setTimeout(
					checkExpiry,
					Math.min(session.expiresAt - Date.now(), 2_147_483_647),
				);
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

		if (!isSessionValid(session)) {
			endSession();
			return;
		}

		// Only check session if not already authenticated (prevents flash on navigation)
		if (currentUser.isFetching) {
			if (status !== "authenticated") {
				checkSession(session);
			}
			return;
		}

		if (currentUser.isSuccess) {
			confirmSession(session);
		} else if (currentUser.isError) {
			failVerification(
				session,
				getErrorMessage(
					currentUser.error,
					"No pudimos comprobar tu sesión. Vuelve a intentarlo.",
				),
			);
		}
	}, [
		session,
		currentUser.isFetching,
		currentUser.isSuccess,
		currentUser.isError,
		currentUser.error,
		checkSession,
		confirmSession,
		failVerification,
		endSession,
		status,
	]);

	return children;
}
