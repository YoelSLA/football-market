import { type ButtonHTMLAttributes, forwardRef } from "react";
import styles from "./Button.module.scss";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
	unstyled?: boolean;
	variant?: "primary" | "secondary";
}

const BUTTON_VARIANT_CLASSES = {
	primary: styles.button_primary,
	secondary: styles.button_secondary,
} as const;

const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
	{
		className,
		type = "button",
		unstyled = false,
		variant = "primary",
		...props
	},
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
					: [styles.button, BUTTON_VARIANT_CLASSES[variant], className]
							.filter(Boolean)
							.join(" ")
			}
		/>
	);
});

export default Button;
