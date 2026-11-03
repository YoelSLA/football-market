import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { buyQuantitySchema } from "@/features/market/form/schemas";
import type { BuyQuantityForm } from "@/features/market/types";

/**
 * Hook de formulario para la cantidad de tokens a comprar (SPEC-019).
 * Inicia en "1". Valida cadenas de dígitos entre 1 y 100 y expone
 * el valor convertido a number sin invocar el Mutation Hook.
 */
export function useBuyQuantityForm() {
	const form = useForm<BuyQuantityForm>({
		resolver: zodResolver(buyQuantitySchema),
		mode: "all",
		defaultValues: { quantity: "1" },
	});

	const rawQuantity = form.watch("quantity");
	const parsed = Number.parseInt(rawQuantity, 10);
	const isValid =
		/^\d+$/.test(rawQuantity) &&
		!Number.isNaN(parsed) &&
		parsed >= 1 &&
		parsed <= 100;
	const validatedQuantity = isValid ? parsed : null;

	return {
		...form,
		validatedQuantity,
		resetQuantity: () => form.reset({ quantity: "1" }),
	};
}
