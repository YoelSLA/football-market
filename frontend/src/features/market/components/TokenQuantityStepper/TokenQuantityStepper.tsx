import { Field } from "@/shared/components";
import { Minus, Plus } from "@/shared/icons";
import styles from "./TokenQuantityStepper.module.scss";

export interface TokenQuantityPreset {
	label: string;
	value: number;
}

export interface TokenQuantityStepperProps {
	id: string;
	label: string;
	quantity: number;
	min?: number;
	max?: number;
	disabled?: boolean;
	error?: string;
	inputRef?: (element: HTMLInputElement | null) => void;
	inputProps?: Record<string, unknown>;
	presets?: (number | TokenQuantityPreset)[];
	presetsLabel?: string;
	onQuantityChange: (quantity: number) => void;
	className?: string;
}

export default function TokenQuantityStepper({
	id,
	label,
	quantity,
	min = 1,
	max = 100,
	disabled = false,
	error,
	inputRef,
	inputProps,
	presets,
	presetsLabel = "Cantidades rápidas",
	onQuantityChange,
	className,
}: TokenQuantityStepperProps) {
	const safeQuantity = Number.isFinite(quantity) ? quantity : min;
	const canDecrement = !disabled && safeQuantity > min;
	const canIncrement = !disabled && safeQuantity < max;

	const handleDecrement = () => {
		onQuantityChange(Math.max(min, safeQuantity - 1));
	};

	const handleIncrement = () => {
		onQuantityChange(Math.min(max, safeQuantity + 1));
	};

	return (
		<div className={`${styles.stepper} ${className ?? ""}`.trim()}>
			<div className={styles.stepper__controls}>
				<button
					type="button"
					className={styles.stepper__button}
					aria-label="Disminuir cantidad"
					disabled={!canDecrement}
					onClick={handleDecrement}
				>
					<Minus aria-hidden="true" />
				</button>

				<div className={styles.stepper__field}>
					<Field
						id={id}
						label={label}
						inputMode="numeric"
						autoComplete="off"
						disabled={disabled}
						error={error}
						ref={inputRef}
						{...inputProps}
					/>
				</div>

				<button
					type="button"
					className={styles.stepper__button}
					aria-label="Aumentar cantidad"
					disabled={!canIncrement}
					onClick={handleIncrement}
				>
					<Plus aria-hidden="true" />
				</button>
			</div>

			{presets && presets.length > 0 && (
				<fieldset className={styles.stepper__presets}>
					<legend className={styles.stepper__presetsLabel}>
						{presetsLabel}
					</legend>
					{presets.map((preset) => {
						const isObject = typeof preset === "object";
						const label = isObject ? preset.label : String(preset);
						const value = isObject ? preset.value : preset;
						return (
							<button
								key={label}
								type="button"
								className={styles.stepper__presetButton}
								disabled={disabled}
								onClick={() =>
									onQuantityChange(Math.min(max, Math.max(min, value)))
								}
							>
								{label}
							</button>
						);
					})}
				</fieldset>
			)}
		</div>
	);
}
