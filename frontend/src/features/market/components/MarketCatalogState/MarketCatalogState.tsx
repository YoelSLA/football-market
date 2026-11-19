import type { ReactNode } from "react";
import { Button } from "@/shared/components";
import styles from "./MarketCatalogState.module.scss";

interface Props {
	variant: "loading" | "error" | "empty";
	message: string;
	onRetry?: () => void;
	children?: ReactNode;
}

export default function MarketCatalogState({
	variant,
	message,
	onRetry,
	children,
}: Props) {
	return (
		<div
			className={styles.state}
			role={variant === "error" ? "alert" : "status"}
		>
			<p>{message}</p>

			{variant === "error" && onRetry && (
				<Button onClick={onRetry}>Reintentar</Button>
			)}

			{children}
		</div>
	);
}
