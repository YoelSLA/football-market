import { zodResolver } from "@hookform/resolvers/zod";
import { useMemo } from "react";
import { useForm } from "react-hook-form";
import { sellQuantitySchema } from "@/features/market/form/schemas";
import type { SellQuantityForm } from "@/features/market/types";

export function useSellQuantityForm(availableQuantity: number) {
	const form = useForm<SellQuantityForm>({
		resolver: zodResolver(sellQuantitySchema),
		mode: "all",
		defaultValues: { quantity: "1" },
	});
	const rawQuantity = form.watch("quantity");
	const parsed = Number(rawQuantity);
	const validatedQuantity =
		/^\d+$/.test(rawQuantity) &&
		Number.isSafeInteger(parsed) &&
		parsed >= 1 &&
		parsed <= 100 &&
		parsed <= availableQuantity
			? parsed
			: null;
	const maximum = Math.min(100, Math.max(0, availableQuantity));
	const presets = useMemo(
		() =>
			[1, 2, 5, maximum]
				.map((value) => Math.min(value, maximum))
				.filter(
					(value, index, values) =>
						value >= 1 && values.indexOf(value) === index,
				),
		[maximum],
	);

	return {
		...form,
		validatedQuantity,
		maximum,
		presets,
		setQuantity: (quantity: number) =>
			form.setValue("quantity", String(quantity), { shouldValidate: true }),
		resetQuantity: () => form.reset({ quantity: "1" }),
	};
}
