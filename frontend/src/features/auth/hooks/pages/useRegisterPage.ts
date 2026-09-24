import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { getErrorCode, getErrorMessage } from "@/infrastructure/http";
import { AUTH_ERROR_CODES } from "../../constants";
import { useRegisterForm } from "../../form";
import { useRegisterMutation } from "../mutations";

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
			if (getErrorCode(failure) === AUTH_ERROR_CODES.EMAIL_ALREADY_REGISTERED) {
				form.setError("email", { message });
			} else {
				setError(message);
			}
		} finally {
			mutation.reset();
		}
	});
	return { form, onSubmit, error, isPending: form.formState.isSubmitting };
}
