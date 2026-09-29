import { zodResolver } from "@hookform/resolvers/zod";
import { useRef } from "react";
import { useForm } from "react-hook-form";
import type { RegisterForm } from "../../types";
import { registerSchema } from "../schemas";

export function useRegisterForm() {
	const form = useForm<RegisterForm>({
		resolver: zodResolver(registerSchema),
		defaultValues: { email: "", password: "", passwordConfirmation: "" },
	});
	const submitting = useRef(false);
	return {
		...form,
		submit: (action: (values: RegisterForm) => Promise<void>) =>
			form.handleSubmit(async (values) => {
				if (submitting.current) return;
				submitting.current = true;
				try {
					await action(values);
				} finally {
					submitting.current = false;
				}
			}),
	};
}
