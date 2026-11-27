import { Search } from "@/shared/icons";
import FilterField from "../FilterField";
import styles from "./FilterSearch.module.scss";

interface Props {
	id: string;
	label: string;
	value: string;
	onChange: (value: string) => void;
	className?: string;
	placeholder?: string;
	accessibleLabel?: string;
	disabled?: boolean;
	maxLength?: number;
}

export default function FilterSearch({
	id,
	label,
	value,
	onChange,
	className,
	placeholder,
	accessibleLabel,
	disabled = false,
	maxLength,
}: Props) {
	return (
		<FilterField id={id} label={label} className={className}>
			<span className={styles.filterSearch__control}>
				<Search className={styles.filterSearch__icon} aria-hidden="true" />
				<input
					id={id}
					className={styles.filterSearch__input}
					type="search"
					aria-label={accessibleLabel ?? label}
					placeholder={placeholder}
					value={value}
					disabled={disabled}
					maxLength={maxLength}
					onChange={(event) => onChange(event.target.value)}
				/>
			</span>
		</FilterField>
	);
}
