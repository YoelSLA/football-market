import { z } from "zod";

const email = z
	.email("Introduce un email válido.")
	.min(1, "Introduce tu email.");
const password = z.string().min(1, "Introduce tu contraseña.");

export const loginSchema = z.object({ email, password });
