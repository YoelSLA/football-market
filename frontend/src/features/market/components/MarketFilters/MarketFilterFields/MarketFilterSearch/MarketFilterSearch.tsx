import { Search } from "@/shared/icons";
import styles from "./MarketFilterSearch.module.scss";

interface Props {
	id?: string;
	className?: string;
	value?: string;
	disabled?: boolean;
	active?: boolean;
	onChange?: (value: string) => void;
	label?: string;
	accessibleLabel?: string;
	placeholder?: string;
	showLabel?: boolean;
}

export default function MarketFilterSearch({
	id,
	className,
	value,
	disabled = false,
	active = true,
	onChange,
	label = "Buscar jugador o club",
	accessibleLabel,
	placeholder = "Buscar jugador o club...",
	showLabel = false,
}: Props) {
	return (
		<label
			className={
				className
					? `${styles.marketFilterSearch} ${className}`
					: styles.marketFilterSearch
			}
			htmlFor={id}
		>
			{showLabel && (
				<span className={styles.marketFilterSearch__label}>{label}</span>
			)}
			<span className={styles.marketFilterSearch__control}>
				<Search
					className={styles.marketFilterSearch__icon}
					aria-hidden="true"
				/>
				<input
					id={id}
					className={styles.marketFilterSearch__input}
					aria-label={
						active
							? (accessibleLabel ?? label)
							: `${accessibleLabel ?? label} (pendiente)`
					}
					placeholder={placeholder}
					value={active ? (value ?? "") : undefined}
					disabled={disabled}
					onChange={
						active && onChange
							? (event) => onChange(event.target.value)
							: undefined
					}
				/>
			</span>
		</label>
	);
}
