import { z } from "zod";
import { loginSchema } from "./loginSchema";

export const registerSchema = z
	.object({
		email: loginSchema.shape.email,
		password: loginSchema.shape.password.min(
			8,
			"La contraseña debe tener al menos 8 caracteres.",
		),
		passwordConfirmation: z.string().min(1, "Confirma tu contraseña."),
	})
	.refine((values) => values.password === values.passwordConfirmation, {
		message: "Las contraseñas deben coincidir.",
		path: ["passwordConfirmation"],
	});
