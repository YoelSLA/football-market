import type { InputHTMLAttributes, Ref } from "react";

import styles from "./Field.module.scss";

interface Props extends InputHTMLAttributes<HTMLInputElement> {
	id: string;
	label: string;
	error?: string;
	ref?: Ref<HTMLInputElement>;
}

export default function Field({ label, error, ...input }: Props) {
	return (
		<div className={styles.field}>
			<label className={styles.field__label} htmlFor={input.id}>
				{label}
			</label>

			<input
				{...input}
				className={
					input.className
						? `${styles.field__input} ${input.className}`
						: styles.field__input
				}
				aria-invalid={Boolean(error)}
				aria-describedby={error ? `${input.id}-error` : undefined}
			/>

			<span id={`${input.id}-error`} className={styles.field__error}>
				{error}
			</span>
		</div>
	);
}
