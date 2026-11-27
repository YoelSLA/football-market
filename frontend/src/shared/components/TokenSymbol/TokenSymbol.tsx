import { TOKEN_SYMBOL } from "@/shared/constants";
import styles from "./TokenSymbol.module.scss";

export interface TokenSymbolProps {
	className?: string;
	size?: "xs" | "sm" | "md" | "lg";
	ariaHidden?: boolean;
}

export default function TokenSymbol({
	className,
	size,
	ariaHidden = false,
}: TokenSymbolProps) {
	const sizeClass = size ? styles[`tokenSymbol_${size}`] : "";
	return (
		<span
			className={`${styles.tokenSymbol} ${sizeClass} ${className ?? ""}`.trim()}
			aria-hidden={ariaHidden ? true : undefined}
			title="Tokens"
		>
			{TOKEN_SYMBOL}
		</span>
	);
}
