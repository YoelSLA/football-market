import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import type {
	MarketPlayer,
	MarketQuote,
	MarketQuoteStatus,
} from "@/features/market/types";
import {
	calculateQuoteVariation,
	formatCredits,
	formatQuoteEffectiveDate,
} from "@/features/market/utils";
import { Button } from "@/shared/components";
import {
	CalendarDays,
	CircleAlert,
	CircleX,
	LoaderCircle,
	RotateCw,
	ShoppingCart,
} from "@/shared/icons";
import styles from "./MarketPlayerCard.module.scss";

interface Props {
	player: MarketPlayer;
	quote: MarketQuote | null;
	quoteStatus: MarketQuoteStatus;
	quoteHttpStatus?: number;
	canRetryQuote: boolean;
	onRetryQuote: () => void;
	onBuyClick: (player: MarketPlayer, triggerEl: HTMLElement | null) => void;
}

/**
 * Ficha compacta de jugador en el mercado (rediseño SPEC-019).
 * Presenta fotografía, posición, nombre, equipo, liga, cotización vigente,
 * variación respecto al precio anterior, fecha de vigencia y acción de compra.
 * Cubre los 5 estados visuales respetando el diseño uniforme en grilla.
 */
export default function MarketPlayerCard({
	player,
	quote,
	quoteStatus,
	onRetryQuote,
	onBuyClick,
}: Props) {
	const isLoading = quoteStatus === "loading" || quoteStatus === "paused";
	const isMissing =
		quoteStatus === "missing" || (quoteStatus === "available" && !quote);
	const isAvailable = quoteStatus === "available" && quote !== null;
	const isError = !isLoading && !isMissing && !isAvailable;

	const variation = isAvailable
		? calculateQuoteVariation(quote.price, quote.previousPrice)
		: null;

	return (
		<article className={styles.card}>
			<MarketPlayerSummary player={player} />

			<div className={styles.card__content}>
				{isAvailable && quote && variation && (
					<>
						<p className={styles.card__label}>Cotización actual</p>
						<div className={styles.card__quoteRow}>
							<span className={styles.card__price}>
								{formatCredits(quote.price, true)}
							</span>
							<div
								className={`${styles.card__variation} ${styles[`card__variation_${variation.direction}`]}`}
								title={
									variation.hasPreviousPrice
										? `Variación respecto a cotización anterior: ${variation.percentageText} ${variation.amountText}`
										: "Sin variación previa"
								}
							>
								<span className={styles.card__variationPercentage}>
									<span
										className={styles.card__variationArrow}
										aria-hidden="true"
									>
										{variation.arrow}
									</span>
									{variation.percentageText}
								</span>
								<span className={styles.card__variationAmount}>
									{variation.amountText}
								</span>
							</div>
						</div>
						<p className={styles.card__effectiveDate}>
							<CalendarDays
								aria-hidden="true"
								className={styles.card__dateIcon}
							/>
							<span>
								Vigente: {formatQuoteEffectiveDate(quote.effectiveDate)}
							</span>
						</p>
					</>
				)}

				{isLoading && (
					<>
						<p className={styles.card__label}>Cotización actual</p>
						<div className={styles.card__quoteRow}>
							<div className={styles.card__skeletonPrice} aria-hidden="true" />
							<div
								className={styles.card__spinnerWrapper}
								role="status"
								aria-label="Cargando cotización"
							>
								<LoaderCircle
									className={styles.card__spinner}
									aria-hidden="true"
								/>
							</div>
						</div>
						<p className={styles.card__effectiveDate}>
							<CalendarDays
								aria-hidden="true"
								className={styles.card__dateIcon}
							/>
							<span className={styles.card__skeletonDate} aria-hidden="true" />
						</p>
					</>
				)}

				{isMissing && (
					<>
						<div className={styles.card__missingAlert} role="status">
							<CircleAlert
								className={styles.card__missingIcon}
								aria-hidden="true"
							/>
							<p className={styles.card__missingText}>
								No hay cotización disponible para este jugador.
							</p>
						</div>
						<p className={styles.card__effectiveDate}>
							<CalendarDays
								aria-hidden="true"
								className={styles.card__dateIcon}
							/>
							<span>Vigente: —</span>
						</p>
					</>
				)}

				{isError && (
					<div className={styles.card__errorAlert} role="alert">
						<CircleX className={styles.card__errorIcon} aria-hidden="true" />
						<div className={styles.card__errorText}>
							<p>No se pudo obtener la cotización.</p>
							<p>Intentá nuevamente.</p>
						</div>
					</div>
				)}
			</div>

			<div className={styles.card__actions}>
				{isAvailable && (
					<Button
						className={styles.card__buyButton}
						aria-label={`Comprar tokens de ${player.name}`}
						onClick={(e) => onBuyClick(player, e.currentTarget)}
					>
						<ShoppingCart aria-hidden="true" /> Comprar
					</Button>
				)}

				{(isLoading || isMissing) && (
					<Button
						className={styles.card__buyButtonDisabled}
						disabled
						aria-disabled="true"
					>
						<ShoppingCart aria-hidden="true" /> Comprar
					</Button>
				)}

				{isError && (
					<Button
						type="button"
						className={styles.card__retryButton}
						onClick={onRetryQuote}
						aria-label={`Reintentar cotización de ${player.name}`}
					>
						<RotateCw aria-hidden="true" /> Reintentar cotización
					</Button>
				)}
			</div>
		</article>
	);
}
