import type { ButtonHTMLAttributes } from "react";
import styles from "./Button.module.scss";

export function Button({
	className,
	type = "button",
	...props
}: ButtonHTMLAttributes<HTMLButtonElement>) {
	return (
		<button
			{...props}
			type={type}
			className={className ? `${styles.button} ${className}` : styles.button}
		/>
	);
}
