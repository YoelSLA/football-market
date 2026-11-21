import { useState } from "react";
import { Flame, Radar, Sparkles } from "@/shared/icons";
import type { PlayerMover, ValuePick } from "../../types/models";
import { SorarePlayerCard } from "./SorarePlayerCard/SorarePlayerCard";
import styles from "./SorareShowcaseGrid.module.scss";

export interface SorareShowcaseGridProps {
	valuePicks: ValuePick[];
	topGainers: PlayerMover[];
	className?: string;
}

export function SorareShowcaseGrid({
	valuePicks,
	topGainers,
	className,
}: SorareShowcaseGridProps) {
	const [activeTab, setActiveTab] = useState<"value" | "gainers">("value");

	return (
		<section
			className={`${styles.container} ${className ?? ""}`.trim()}
			aria-label="Showcase de activos coleccionables"
		>
			<header className={styles.header}>
				<div className={styles.titleGroup}>
					<span className={styles.headerIconTile} aria-hidden="true">
						<Sparkles className={styles.headerIcon} />
					</span>
					<div className={styles.headerTitles}>
						<h2 className={styles.title}>Showcase Deportivo</h2>
						<span className={styles.subtitle}>
							Cartas coleccionables destacadas por algoritmo y rendimiento
						</span>
					</div>
				</div>

				<div
					className={styles.tabGroup}
					role="tablist"
					aria-label="Filtro del showcase deportivo"
				>
					<button
						type="button"
						role="tab"
						aria-selected={activeTab === "value"}
						className={`${styles.tabBtn} ${activeTab === "value" ? styles.tabBtnActive : ""}`}
						onClick={() => setActiveTab("value")}
					>
						<Radar size={14} className={styles.tabIcon} aria-hidden="true" />
						<span>Radar de Oportunidades</span>
					</button>
					<button
						type="button"
						role="tab"
						aria-selected={activeTab === "gainers"}
						className={`${styles.tabBtn} ${activeTab === "gainers" ? styles.tabBtnActive : ""}`}
						onClick={() => setActiveTab("gainers")}
					>
						<Flame size={14} className={styles.tabIcon} aria-hidden="true" />
						<span>Estrellas en Racha</span>
					</button>
				</div>
			</header>

			<div className={styles.grid}>
				{activeTab === "value" ? (
					valuePicks.length === 0 ? (
						<div className={styles.emptyNotice}>
							<p>No se encontraron oportunidades destacadas de valor.</p>
						</div>
					) : (
						valuePicks.map((pick) => (
							<SorarePlayerCard
								key={pick.playerId}
								playerId={pick.playerId}
								name={pick.name}
								team={pick.team}
								league={pick.league}
								position={pick.position}
								imageUrl={pick.imageUrl}
								score={pick.score}
								price={pick.price}
								valueRatio={pick.valueRatio}
							/>
						))
					)
				) : topGainers.length === 0 ? (
					<div className={styles.emptyNotice}>
						<p>No se registran estrellas en racha alcista hoy.</p>
					</div>
				) : (
					topGainers.map((mover) => (
						<SorarePlayerCard
							key={mover.playerId}
							playerId={mover.playerId}
							name={mover.name}
							team={mover.team}
							league={mover.league}
							position={mover.position}
							imageUrl={mover.imageUrl}
							score={mover.score}
							price={mover.currentPrice}
							deltaPercentage={mover.deltaPercentage}
						/>
					))
				)}
			</div>
		</section>
	);
}
