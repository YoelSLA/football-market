import { calculateP2POpportunity } from "@/features/market/market.mapper";
import { formatCentimes, formatCredits } from "@/features/market/utils";
import { TokenSymbol } from "@/shared/components";
import { CREDIT_SYMBOL } from "@/shared/constants";
import { Coins, Tag } from "@/shared/icons";
import styles from "./P2PPriceSelector.module.scss";

interface Props {
	officialPriceCentimes: number;
	unitPriceCentimes: number;
	quantity: number;
	disabled?: boolean;
	onChange: (unitPriceCentimes: number) => void;
}

const PRESET_PERCENTAGES = [-20, -10, 0, 10, 20] as const;

export default function P2PPriceSelector({
	officialPriceCentimes,
	unitPriceCentimes,
	quantity,
	disabled = false,
	onChange,
}: Props) {
	const minPrice = Math.round(officialPriceCentimes * 0.8);
	const maxPrice = Math.round(officialPriceCentimes * 1.2);

	const opportunity = calculateP2POpportunity(
		unitPriceCentimes,
		officialPriceCentimes,
	);
	const totalCentimes = quantity * unitPriceCentimes;

	const handleSliderChange = (event: React.ChangeEvent<HTMLInputElement>) => {
		const value = Number(event.target.value);
		const clamped = Math.max(minPrice, Math.min(maxPrice, value));
		onChange(clamped);
	};

	const handleNumberChange = (event: React.ChangeEvent<HTMLInputElement>) => {
		const raw = event.target.value;
		if (raw === "") return;
		const parsedCredits = Number.parseFloat(raw);
		if (Number.isNaN(parsedCredits)) return;
		const centimes = Math.round(parsedCredits * 100);
		const clamped = Math.max(minPrice, Math.min(maxPrice, centimes));
		onChange(clamped);
	};

	const handlePresetClick = (pct: number) => {
		const targetCentimes = Math.round(officialPriceCentimes * (1 + pct / 100));
		const clamped = Math.max(minPrice, Math.min(maxPrice, targetCentimes));
		onChange(clamped);
	};

	return (
		<div
			className={styles.p2pPrice}
			id="panel-p2p"
			role="tabpanel"
			aria-labelledby="tab-p2p"
		>
			<div className={styles.p2pPrice__header}>
				<div className={styles.p2pPrice__title}>
					<Tag aria-hidden="true" />
					<span>Fijación de precio flexible P2P</span>
				</div>
				{opportunity.type !== "NEUTRAL" && (
					<span
						className={
							opportunity.type === "DISCOUNT"
								? styles.badge_discount
								: styles.badge_premium
						}
					>
						{opportunity.label}
					</span>
				)}
			</div>

			<div className={styles.p2pPrice__body}>
				<div className={styles.p2pPrice__inputGroup}>
					<label
						htmlFor="p2p-price-input"
						className={styles.p2pPrice__inputLabel}
					>
						Precio por <TokenSymbol /> ({CREDIT_SYMBOL})
					</label>
					<div className={styles.p2pPrice__inputWrapper}>
						<Coins className={styles.p2pPrice__coinsIcon} aria-hidden="true" />
						<input
							id="p2p-price-input"
							type="number"
							step="0.01"
							min={(minPrice / 100).toFixed(2)}
							max={(maxPrice / 100).toFixed(2)}
							value={(unitPriceCentimes / 100).toFixed(2)}
							disabled={disabled}
							onChange={handleNumberChange}
							className={styles.p2pPrice__numberInput}
							aria-label="Precio unitario en créditos"
						/>
					</div>
				</div>

				<div className={styles.p2pPrice__sliderGroup}>
					<input
						type="range"
						min={minPrice}
						max={maxPrice}
						step={1}
						value={unitPriceCentimes}
						disabled={disabled}
						onChange={handleSliderChange}
						className={styles.p2pPrice__slider}
						aria-label="Ajustar precio en rango de -20% a +20%"
						aria-valuemin={minPrice}
						aria-valuemax={maxPrice}
						aria-valuenow={unitPriceCentimes}
					/>
					<div className={styles.p2pPrice__sliderLabels}>
						<span>-20% ({formatCredits(minPrice)})</span>
						<span>Oficial ({formatCredits(officialPriceCentimes)})</span>
						<span>+20% ({formatCredits(maxPrice)})</span>
					</div>
				</div>

				<fieldset
					className={styles.p2pPrice__presets}
					aria-label="Atajos de variación de precio"
				>
					{PRESET_PERCENTAGES.map((pct) => {
						const isSelected = opportunity.percentageDelta === pct;
						const label = pct === 0 ? "0%" : pct > 0 ? `+${pct}%` : `${pct}%`;
						return (
							<button
								key={pct}
								type="button"
								className={
									isSelected ? styles.preset_active : styles.preset_button
								}
								disabled={disabled}
								onClick={() => handlePresetClick(pct)}
							>
								{label}
							</button>
						);
					})}
				</fieldset>

				<div className={styles.p2pPrice__divider} />

				<div className={styles.p2pPrice__totalRow}>
					<div className={styles.p2pPrice__totalHeader}>
						<span className={styles.p2pPrice__totalLabel}>
							Total bruto a recibir
						</span>
						<p className={styles.p2pPrice__disclaimer}>
							Publicación comunitaria en P2P · Válida por 24 hs
						</p>
					</div>
					<div className={styles.p2pPrice__totalAmount}>
						<strong>{formatCredits(totalCentimes)}</strong>
						<small>({formatCentimes(totalCentimes)})</small>
					</div>
				</div>
			</div>
		</div>
	);
}
