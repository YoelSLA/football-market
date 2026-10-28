import { z } from "zod";

export const sellQuantitySchema = z.object({
	quantity: z
		.string()
		.min(1, "Cantidad entre 1 y 100")
		.regex(/^\d+$/, "Cantidad entre 1 y 100")
		.refine((value) => {
			const quantity = Number(value);
			return Number.isSafeInteger(quantity) && quantity >= 1 && quantity <= 100;
		}, "Cantidad entre 1 y 100"),
});
