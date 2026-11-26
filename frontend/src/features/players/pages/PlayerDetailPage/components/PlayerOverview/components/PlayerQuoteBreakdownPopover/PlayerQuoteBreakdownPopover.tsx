import { useEffect, useRef, useState } from "react";
import type { QuoteBreakdownFactors } from "@/features/players/types";
import { TokenSymbol } from "@/shared/components";
import { Activity, Flame, Info, X } from "@/shared/icons";
import styles from "./PlayerQuoteBreakdownPopover.module.scss";

export interface PlayerQuoteBreakdownPopoverProps {
	breakdown: QuoteBreakdownFactors;
}

export default function PlayerQuoteBreakdownPopover({
	breakdown,
}: PlayerQuoteBreakdownPopoverProps) {
	const [isOpen, setIsOpen] = useState(false);
	const popoverRef = useRef<HTMLDivElement>(null);
	const triggerRef = useRef<HTMLButtonElement>(null);

	// Cerrar al pulsar Escape o hacer click fuera
	useEffect(() => {
		if (!isOpen) return;

		function handleClickOutside(event: MouseEvent) {
			if (
				popoverRef.current &&
				!popoverRef.current.contains(event.target as Node) &&
				triggerRef.current &&
				!triggerRef.current.contains(event.target as Node)
			) {
				setIsOpen(false);
			}
		}

		function handleKeyDown(event: KeyboardEvent) {
			if (event.key === "Escape") {
				setIsOpen(false);
				triggerRef.current?.focus();
			}
		}

		document.addEventListener("mousedown", handleClickOutside);
		document.addEventListener("keydown", handleKeyDown);
		return () => {
			document.removeEventListener("mousedown", handleClickOutside);
			document.removeEventListener("keydown", handleKeyDown);
		};
	}, [isOpen]);

	const sportSign = breakdown.sportContributionPercentage >= 0 ? "+" : "";
	const marketSign = breakdown.marketContributionPercentage >= 0 ? "+" : "";
	const totalSign = breakdown.totalVariationPercentage >= 0 ? "+" : "";

	return (
		<div className={styles.wrapper}>
			<button
				ref={triggerRef}
				type="button"
				className={styles.triggerButton}
				onClick={() => setIsOpen((prev) => !prev)}
				aria-expanded={isOpen}
				aria-haspopup="dialog"
				aria-label="Ver desglose de factores de cotización"
				title="Ver desglose de factores de cotización"
			>
				<Info className={styles.triggerIcon} aria-hidden="true" />
				<span>Desglose</span>
			</button>

			{isOpen && (
				<div
					ref={popoverRef}
					className={styles.popover}
					role="dialog"
					aria-label="Desglose de cotización del jugador"
				>
					<div className={styles.popoverHeader}>
						<div>
							<h4 className={styles.title}>Desglose de Cotización</h4>
							<p className={styles.subtitle}>
								Composición algorítmica de la variación
							</p>
						</div>
						<button
							type="button"
							className={styles.closeButton}
							onClick={() => setIsOpen(false)}
							aria-label="Cerrar desglose"
						>
							<X className={styles.closeIcon} aria-hidden="true" />
						</button>
					</div>

					<div className={styles.factorsList}>
						{/* Factor Deportivo */}
						<div className={styles.factorItem}>
							<div className={styles.factorItem__header}>
								<div className={styles.factorItem__titleGroup}>
									<Activity
										className={styles.factorItem__iconSport}
										aria-hidden="true"
									/>
									<span className={styles.factorItem__name}>
										Aporte Deportivo (WhoScored)
									</span>
								</div>
								<span
									className={`${styles.factorItem__badge} ${
										breakdown.sportContributionPercentage >= 0
											? styles["factorItem__badge--positive"]
											: styles["factorItem__badge--negative"]
									}`}
								>
									{sportSign}
									{breakdown.sportContributionPercentage.toFixed(2)} %
								</span>
							</div>
							<div className={styles.factorItem__meta}>
								<span>
									Multiplicador: x{breakdown.sportMultiplier.toFixed(4)}
								</span>
							</div>
						</div>

						{/* Factor de Demanda de Mercado */}
						<div className={styles.factorItem}>
							<div className={styles.factorItem__header}>
								<div className={styles.factorItem__titleGroup}>
									<Flame
										className={styles.factorItem__iconMarket}
										aria-hidden="true"
									/>
									<span className={styles.factorItem__name}>
										Presión de Mercado (Demanda)
									</span>
								</div>
								<span
									className={`${styles.factorItem__badge} ${
										breakdown.marketContributionPercentage >= 0
											? styles["factorItem__badge--positive"]
											: styles["factorItem__badge--negative"]
									}`}
								>
									{marketSign}
									{breakdown.marketContributionPercentage.toFixed(2)} %
								</span>
							</div>
							<div className={styles.factorItem__meta}>
								<span>
									Neto:{" "}
									{breakdown.netDemandTokens > 0
										? `+${breakdown.netDemandTokens}`
										: breakdown.netDemandTokens}{" "}
									<TokenSymbol size="xs" />
								</span>
								<span>
									Multiplicador: x{breakdown.marketMultiplier.toFixed(4)}
								</span>
							</div>
						</div>

						{/* Total Combinado */}
						<div className={styles.totalRow}>
							<span className={styles.totalLabel}>
								Variación Total Combinada
							</span>
							<strong
								className={`${styles.totalValue} ${
									breakdown.totalVariationPercentage >= 0
										? styles["totalValue--positive"]
										: styles["totalValue--negative"]
								}`}
							>
								{totalSign}
								{breakdown.totalVariationPercentage.toFixed(2)} %
							</strong>
						</div>
					</div>

					<p className={styles.footnote}>
						El precio final surge del producto entre el rendimiento en campo y
						la presión neta compradora comunitaria.
					</p>
				</div>
			)}
		</div>
	);
}
