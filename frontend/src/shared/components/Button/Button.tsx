import { type ButtonHTMLAttributes, forwardRef } from "react";
import styles from "./Button.module.scss";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
	unstyled?: boolean;
}

const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
	{ className, type = "button", unstyled = false, ...props },
	ref,
) {
	return (
		<button
			ref={ref}
			{...props}
			type={type}
			className={
				unstyled
					? className
					: className
						? `${styles.button} ${className}`
						: styles.button
			}
		/>
	);
});

export default Button;
