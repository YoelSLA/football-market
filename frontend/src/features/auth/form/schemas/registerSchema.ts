import { z } from "zod";
import { loginSchema } from "./loginSchema";

const username = z
	.string()
	.min(1, "Introduce tu nombre de usuario.")
	.min(3, "El nombre de usuario debe tener entre 3 y 30 caracteres.")
	.max(30, "El nombre de usuario debe tener entre 3 y 30 caracteres.")
	.regex(
		/^[A-Za-z0-9._-]+$/,
		"Usa solo letras, números, punto, guion y guion bajo.",
	);

export const registerSchema = z
	.object({
		username,
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
