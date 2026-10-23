import { zodResolver } from "@hookform/resolvers/zod";
import { useRef } from "react";
import { useForm } from "react-hook-form";
import { registerSchema } from "@/features/auth/form/schemas";
import type { RegisterForm } from "@/features/auth/types";

export function useRegisterForm() {
	const form = useForm<RegisterForm>({
		resolver: zodResolver(registerSchema),
		defaultValues: {
			username: "",
			email: "",
			password: "",
			passwordConfirmation: "",
		},
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
