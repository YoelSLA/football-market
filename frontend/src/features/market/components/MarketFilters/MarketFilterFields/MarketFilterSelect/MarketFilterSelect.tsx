import styles from "./MarketFilterSelect.module.scss";

interface Option {
	value: string;
	label: string;
}

interface Props {
	id?: string;
	label: string;
	value?: string;
	defaultValue?: string;
	disabled?: boolean;
	options: Option[];
	onChange?: (value: string) => void;
}

export default function MarketFilterSelect({
	id,
	label,
	value,
	defaultValue,
	disabled = false,
	options,
	onChange,
}: Props) {
	return (
		<label className={styles.marketFilterSelect} htmlFor={id}>
			{label}
			<select
				id={id}
				className={styles.marketFilterSelect__select}
				value={value}
				defaultValue={defaultValue}
				disabled={disabled}
				onChange={
					onChange ? (event) => onChange(event.target.value) : undefined
				}
			>
				{options.map((option) => (
					<option key={option.value} value={option.value}>
						{option.label}
					</option>
				))}
			</select>
		</label>
	);
}
