import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { getErrorCode, getErrorMessage } from "@/infrastructure/http";
import { AUTH_ERROR_CODES } from "../../constants";
import { useLoginForm } from "../../form";
import { useLoginMutation } from "../mutations";
import { useAuthStore } from "@/infrastructure/storage";

export function useLoginPage() {
	const form = useLoginForm();
	const navigate = useNavigate();
	const location = useLocation();
  const startSession = useAuthStore((state) => state.startSession);
	const [error, setError] = useState<string | null>(null);
	const mutation = useLoginMutation();
	const state: unknown = location.state;
	const registrationComplete =
		typeof state === "object" &&
		state !== null &&
		"registrationComplete" in state &&
		state.registrationComplete === true;
	const onSubmit = form.submit(async (values) => {
		setError(null);
		try {
			const session = await mutation.mutateAsync(values);
			startSession(session);
			form.reset();
			navigate("/home", { replace: true });
		} catch (failure) {
			const message = getErrorMessage(
				failure,
				"No pudimos iniciar sesión. Vuelve a intentarlo.",
			);
			if (getErrorCode(failure) === AUTH_ERROR_CODES.INVALID_CREDENTIALS) {
				form.setError("password", { message });
			} else {
				setError(message);
			}
		} finally {
			mutation.reset();
		}
	});
	return {
		form,
		onSubmit,
		error,
		registrationComplete,
		isPending: form.formState.isSubmitting,
	};
}
