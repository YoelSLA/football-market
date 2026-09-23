import { z } from "zod";

const email = z.email("Introduce un email válido.").min(1, "Introduce tu email.");
const password = z.string().min(1, "Introduce tu contraseña.");

export const loginSchema = z.object({ email, password });
export const registerSchema = z.object({
  email,
  password: password.min(8, "La contraseña debe tener al menos 8 caracteres."),
  passwordConfirmation: z.string().min(1, "Confirma tu contraseña."),
}).refine((values) => values.password === values.passwordConfirmation, {
  message: "Las contraseñas deben coincidir.",
  path: ["passwordConfirmation"],
});
