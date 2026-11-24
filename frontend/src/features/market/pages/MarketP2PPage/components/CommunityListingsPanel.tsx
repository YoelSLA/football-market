import { useState } from "react";
import MarketPagination from "@/features/market/components/MarketPagination";
import PlayerHeroCard from "@/features/market/components/PlayerHeroCard";
import type {
	CommunityMarketPlayer,
	MarketListing,
	MarketQuote,
} from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { Button } from "@/shared/components";
import { ArrowLeft, Package } from "@/shared/icons";
import { CommunityListingCard } from "./CommunityListingCard";
import styles from "./CommunityListingsPanel.module.scss";
import P2POpportunityBadge from "./P2POpportunityBadge";

interface Props {
	player: CommunityMarketPlayer;
	hasPriceLimit: boolean;
	officialQuote?: MarketQuote | null;
	onBack: () => void;
	onClearPriceLimit: () => void;
	onBuy: (listing: MarketListing, trigger: HTMLElement | null) => void;
}

const PAGE_SIZE = 8;

export default function CommunityListingsPanel({
	player,
	hasPriceLimit,
	officialQuote,
	onBack,
	onClearPriceLimit,
	onBuy,
}: Props) {
	const listings = player.listings ?? [];
	const [page, setPage] = useState(1);
	const [prevPlayerId, setPrevPlayerId] = useState(player.id);

	if (player.id !== prevPlayerId) {
		setPrevPlayerId(player.id);
		setPage(1);
	}

	const totalPages = Math.max(1, Math.ceil(listings.length / PAGE_SIZE));
	const safePage = Math.min(page, totalPages);

	const visibleListings = listings.slice(
		(safePage - 1) * PAGE_SIZE,
		safePage * PAGE_SIZE,
	);

	return (
		<section className={styles.panel} aria-labelledby="community-player-title">
			{/* COLUMNA IZQUIERDA: HERO CARD (Ocupa toda la altura) */}
			<div className={styles.panel__leftCol}>
				<Button
					className={styles.panel__back}
					type="button"
					unstyled
					onClick={onBack}
				>
					<ArrowLeft aria-hidden="true" />
					<span>Volver a jugadores</span>
				</Button>

				<PlayerHeroCard
					player={player}
					headerRight={
						<span className={styles.pillCommunity}>👥 Mercado P2P</span>
					}
					statBoxLeft={
						<>
							<span className={styles.statLabel}>Mejor Precio P2P</span>
							<div className={styles.statValRow}>
								<strong className={styles.statValue}>
									{formatCredits(player.minimumUnitPrice)}
								</strong>
								{officialQuote && (
									<P2POpportunityBadge
										unitPrice={player.minimumUnitPrice}
										officialPrice={officialQuote.price}
									/>
								)}
							</div>
							<span className={styles.statSub}>
								{officialQuote
									? `Ref. oficial: ${formatCredits(officialQuote.price)}`
									: "Mejor oferta"}
							</span>
						</>
					}
					statBoxRight={
						<>
							<span className={styles.statLabel}>Disponibles</span>
							<div className={styles.statValRow}>
								<strong className={styles.statValue_cyan}>
									{player.availableQuantity}
								</strong>
								<span className={styles.lotesBadge}>
									{player.listingCount}{" "}
									{player.listingCount === 1 ? "lote" : "lotes"}
								</span>
							</div>
							<span className={styles.statSub}>Lotes indivisibles</span>
						</>
					}
				/>
			</div>

			{/* COLUMNA DERECHA: GRID DE LOTES CON SCROLL PROPIO Y PAGINACIÓN */}
			<div className={styles.panel__rightCol}>
				<header className={styles.panel__noticeBar}>
					<div className={styles.panel__noticeTitle}>
						<Package aria-hidden="true" />
						<h2 id="community-player-title" className={styles.panel__title}>
							Ofertas de {player.name}
						</h2>
						<span className={styles.panel__countBadge}>
							{listings.length}{" "}
							{listings.length === 1 ? "publicación" : "publicaciones"}
						</span>
					</div>

					<div className={styles.panel__noticePill}>
						<span>
							⚡ Las publicaciones se compran completas; no admiten cantidades
							parciales.
						</span>
					</div>

					{hasPriceLimit && (
						<Button
							type="button"
							unstyled
							className={styles.panel__clearPriceBtn}
							onClick={onClearPriceLimit}
						>
							Quitar límite de precio
						</Button>
					)}
				</header>

				{/* Región con scroll dedicado para las tarjetas de lotes */}
				<div className={styles.panel__scrollRegion}>
					<ul
						className={styles.panel__list}
						aria-label={`Publicaciones disponibles de ${player.name}`}
					>
						{visibleListings.length === 0 ? (
							<li role="status" className={styles.panel__emptyItem}>
								No quedan publicaciones disponibles.
							</li>
						) : (
							visibleListings.map((listing) => (
								<CommunityListingCard
									key={listing.listingId}
									listing={listing}
									officialQuote={officialQuote ?? null}
									onBuy={onBuy}
								/>
							))
						)}
					</ul>
				</div>

				{/* Footer de paginación fijo con máximo 8 por página */}
				{totalPages > 1 && (
					<footer className={styles.panel__footer}>
						<MarketPagination
							page={safePage}
							totalPages={totalPages}
							totalPlayers={listings.length}
							visiblePlayers={visibleListings.length}
							pageSize={PAGE_SIZE}
							showSummary={false}
							variant="official"
							onPageChange={setPage}
						/>
					</footer>
				)}
			</div>
		</section>
	);
}
