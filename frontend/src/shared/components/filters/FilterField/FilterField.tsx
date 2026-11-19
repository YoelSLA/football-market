import type { ReactNode } from "react";
import styles from "./FilterField.module.scss";

interface Props {
	id: string;
	label: string;
	icon?: ReactNode;
	className?: string;
	children: ReactNode;
}

export default function FilterField({
	id,
	label,
	icon,
	className,
	children,
}: Props) {
	const classNames = [styles.filterField, className].filter(Boolean).join(" ");

	return (
		<div className={classNames}>
			<label className={styles.filterField__label} htmlFor={id}>
				{icon}
				<span>{label}</span>
			</label>
			{children}
		</div>
	);
}
