import { Pagination } from "@/shared/components";
import styles from "./MarketPagination.module.scss";

interface Props {
	page: number;
	totalPages: number;
	totalPlayers?: number;
	visiblePlayers: number;
	pageSize?: number;
	onPageChange: (page: number) => void;
	showSummary?: boolean;
	variant?: "default" | "official";
	className?: string;
	itemLabel?: string;
}

export default function MarketPagination({
	page,
	totalPages,
	totalPlayers,
	visiblePlayers,
	onPageChange,
	showSummary = true,
	variant = "default",
	className,
	itemLabel = "jugadores",
}: Props) {
	const rootClassName = className
		? `${styles.marketPagination} ${className}`
		: styles.marketPagination;

	return (
		<footer className={rootClassName} data-variant={variant}>
			{showSummary && (
				<p>
					Mostrando {visiblePlayers} de{" "}
					{totalPlayers?.toLocaleString("es-AR") ?? "—"} {itemLabel}
				</p>
			)}
			<Pagination
				page={page}
				totalPages={totalPages}
				onChange={onPageChange}
				className={styles.marketPagination__pages}
			/>
		</footer>
	);
}
