import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { AUTH_ERROR_CODES } from "@/features/auth/constants";
import { useRegisterForm } from "@/features/auth/form";
import { useRegisterMutation } from "@/features/auth/hooks/mutations";
import { getErrorCode, getErrorMessage } from "@/infrastructure/http";

export function useRegisterPage() {
	const form = useRegisterForm();
	const navigate = useNavigate();
	const [error, setError] = useState<string | null>(null);
	const mutation = useRegisterMutation();
	const onSubmit = form.submit(async (values) => {
		setError(null);
		try {
			await mutation.mutateAsync(values);
			form.reset();
			navigate("/login", {
				replace: true,
				state: { registrationComplete: true },
			});
		} catch (failure) {
			const message = getErrorMessage(
				failure,
				"No pudimos crear la cuenta. Vuelve a intentarlo.",
			);
			const code = getErrorCode(failure);
			if (code === AUTH_ERROR_CODES.EMAIL_ALREADY_REGISTERED) {
				form.setError("email", { message });
			} else if (code === AUTH_ERROR_CODES.USERNAME_ALREADY_TAKEN) {
				form.setError("username", { message });
			} else {
				setError(message);
			}
		} finally {
			mutation.reset();
		}
	});
	return { form, onSubmit, error, isPending: form.formState.isSubmitting };
}
