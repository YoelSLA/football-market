import type {
	CommunityMarketPlayer,
	MarketListing,
} from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { Button } from "@/shared/components";
import styles from "./CommunityListingsPanel.module.scss";

interface Props {
	player: CommunityMarketPlayer;
	onBack: () => void;
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
	onBack,
	onBuy,
}: Props) {
	return (
		<section className={styles.panel} aria-labelledby="community-player-title">
			<Button className={styles.panel__back} type="button" onClick={onBack}>
				← Volver a jugadores con ofertas
			</Button>
			<header className={styles.panel__header}>
				<h2 id="community-player-title">{player.name}</h2>
				<p>
					{player.teamName} · {player.position}
				</p>
				<p>
					{player.listingCount} publicaciones · {player.availableQuantity}{" "}
					tokens · Mejor precio {formatCredits(player.minimumUnitPrice)}
				</p>
			</header>
			<p className={styles.panel__notice}>
				Las publicaciones se compran completas; no admiten cantidades parciales.
			</p>
			<ul
				className={styles.panel__list}
				aria-label={`Publicaciones disponibles de ${player.name}`}
			>
				{player.listings.map((listing) => {
					const expired = Date.parse(listing.expiresAt) <= Date.now();
					return (
						<li className={styles.listing} key={listing.listingId}>
							<article className={styles.listing__content}>
								<h3>Publicación #{listing.listingId}</h3>
								<dl className={styles.listing__details}>
									<div>
										<dt>Cantidad</dt>
										<dd>{listing.quantity} tokens</dd>
									</div>
									<div>
										<dt>Precio por token</dt>
										<dd>{formatCredits(listing.unitPrice)}</dd>
									</div>
									<div>
										<dt>Total</dt>
										<dd>{formatCredits(listing.totalAmount)}</dd>
									</div>
									<div>
										<dt>Vence en</dt>
										<dd>{remaining(listing.expiresAt)}</dd>
									</div>
								</dl>
								<Button
									className={styles.listing__action}
									disabled={expired}
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
