import type { ButtonHTMLAttributes } from "react";
import styles from "./Button.module.scss";

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {}

export default function Button({
	className,
	type = "button",
	...props
}: Props) {
	return (
		<button
			{...props}
			type={type}
			className={className ? `${styles.button} ${className}` : styles.button}
		/>
	);
}
