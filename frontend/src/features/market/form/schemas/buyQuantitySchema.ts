import { z } from "zod";

/**
 * Esquema de validación para la cantidad de tokens a comprar (SPEC-019).
 * El campo inicia en "1". Acepta cadenas de dígitos que representen un entero 1–100 (ej. "10").
 * Rechaza cadenas vacías, valores no numéricos, números con coma/punto y fuera de rango.
 */
export const buyQuantitySchema = z.object({
	quantity: z
		.string()
		.min(1, "Cantidad entre 1 y 100")
		.regex(/^\d+$/, "Cantidad entre 1 y 100")
		.refine(
			(val) => {
				const num = Number.parseInt(val, 10);
				return !Number.isNaN(num) && num >= 1 && num <= 100;
			},
			{ message: "Cantidad entre 1 y 100" },
		),
});

export type BuyQuantitySchemaType = z.infer<typeof buyQuantitySchema>;
