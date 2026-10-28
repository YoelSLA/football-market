import { Search } from "@/shared/icons";
import styles from "./MarketFilterSearch.module.scss";

interface Props {
	id?: string;
	className?: string;
	value?: string;
	disabled?: boolean;
	active?: boolean;
	onChange?: (value: string) => void;
}

export default function MarketFilterSearch({
	id,
	className,
	value,
	disabled = false,
	active = true,
	onChange,
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
			<Search className={styles.marketFilterSearch__icon} aria-hidden="true" />
			<input
				id={id}
				className={styles.marketFilterSearch__input}
				aria-label={
					active ? "Buscar jugador o club" : "Buscar jugador o club (pendiente)"
				}
				placeholder="Buscar jugador o club..."
				value={active ? (value ?? "") : undefined}
				disabled={disabled}
				onChange={
					active && onChange
						? (event) => onChange(event.target.value)
						: undefined
				}
			/>
		</label>
	);
}
