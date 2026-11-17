import { Button } from "@/shared/components";
import styles from "./MarketCatalogState.module.scss";

interface Props {
	variant: "loading" | "error" | "empty";
	message: string;
	onRetry?: () => void;
}

export default function MarketCatalogState({
	variant,
	message,
	onRetry,
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
		</div>
	);
}
