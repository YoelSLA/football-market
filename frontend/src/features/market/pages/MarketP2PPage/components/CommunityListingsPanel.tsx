import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import type {
	CommunityMarketPlayer,
	MarketListing,
} from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { Button } from "@/shared/components";
import styles from "./CommunityListingsPanel.module.scss";

interface Props {
	player: CommunityMarketPlayer;
	hasPriceLimit: boolean;
	onBack: () => void;
	onClearPriceLimit: () => void;
	onBuy: (listing: MarketListing, trigger: HTMLElement | null) => void;
}

const remaining = (expiresAt: string) => {
	const minutes = Math.max(
		0,
		Math.ceil((Date.parse(expiresAt) - Date.now()) / 60000),
	);
	return minutes < 60
		? `${minutes} min`
		: `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
};

export default function CommunityListingsPanel({
	player,
	hasPriceLimit,
	onBack,
	onClearPriceLimit,
	onBuy,
}: Props) {
	const listings = player.listings ?? [];

	return (
		<section className={styles.panel} aria-labelledby="community-player-title">
			<Button
				className={styles.panel__back}
				type="button"
				unstyled
				onClick={onBack}
			>
				← Volver a jugadores con ofertas
			</Button>
			<header className={styles.panel__header}>
				<h2 id="community-player-title" className={styles.panel__title}>
					Ofertas de {player.name}
				</h2>
				<div className={styles.panel__summary}>
					<MarketPlayerSummary player={player} variant="communityCard" />
				</div>
				<dl className={styles.panel__metrics}>
					<div>
						<dt>Mejor precio</dt>
						<dd>{formatCredits(player.minimumUnitPrice)}</dd>
						<span>Por token</span>
					</div>
					<div>
						<dt>Tokens disponibles</dt>
						<dd>{player.availableQuantity}</dd>
						<span>En lotes completos</span>
					</div>
					<div>
						<dt>Publicaciones activas</dt>
						<dd>{player.listingCount}</dd>
						<span>Ordenadas por precio</span>
					</div>
				</dl>
			</header>
			<div className={styles.panel__notice} role="note">
				<p>
					Las publicaciones se compran completas; no admiten cantidades
					parciales.
				</p>
				{hasPriceLimit && (
					<p>
						Solo se muestran ofertas dentro del precio máximo seleccionado.
						<Button type="button" unstyled onClick={onClearPriceLimit}>
							Ver todas las publicaciones
						</Button>
					</p>
				)}
			</div>
			<ul
				className={styles.panel__list}
				aria-label={`Publicaciones disponibles de ${player.name}`}
			>
				{listings.length === 0 && (
					<li role="status">No quedan publicaciones disponibles.</li>
				)}
				{listings.map((listing) => {
					const expired = Date.parse(listing.expiresAt) <= Date.now();
					return (
						<li className={styles.listing} key={listing.listingId}>
							<article className={styles.listing__content}>
								<header className={styles.listing__header}>
									<span>Publicación</span>
									<h3>#{listing.listingId}</h3>
								</header>
								<dl className={styles.listing__details}>
									<div>
										<dt>Precio por token</dt>
										<dd>{formatCredits(listing.unitPrice)}</dd>
									</div>
									<div>
										<dt>Cantidad</dt>
										<dd>{listing.quantity} tokens</dd>
									</div>
									<div>
										<dt>Vence en</dt>
										<dd>
											<time dateTime={listing.expiresAt}>
												{remaining(listing.expiresAt)}
											</time>
										</dd>
									</div>
								</dl>
								<div className={styles.listing__total}>
									<span>Precio total del lote</span>
									<strong>{formatCredits(listing.totalAmount)}</strong>
								</div>
								<Button
									className={styles.listing__action}
									disabled={expired}
									aria-disabled={expired}
									onClick={(event) => onBuy(listing, event.currentTarget)}
								>
									{expired ? "Oferta vencida" : "Comprar lote"}
								</Button>
							</article>
						</li>
					);
				})}
			</ul>
		</section>
	);
}
