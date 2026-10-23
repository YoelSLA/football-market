import { Link, useParams } from "react-router-dom";
import { PageLayout } from "@/app/layouts";
import { DEMO_MESSAGES } from "@/features/players/constants";
import { usePlayerDetailPage } from "@/features/players/hooks";
import PlayerBreadcrumb from "./PlayerBreadcrumb";
import styles from "./PlayerDetailPage.module.scss";
import PlayerOverview from "./PlayerOverview";
import PlayerQuoteEvolutionChart from "./PlayerQuoteEvolutionChart";
import PlayerQuotePanel from "./PlayerQuotePanel";
import PlayerSportsSummary from "./PlayerSportsSummary";

export default function PlayerDetailPage() {
	const params = useParams();
	const detail = usePlayerDetailPage(params.id);

	return (
		<div className={styles.playerDetail} aria-busy={detail.state === "loading"}>
			<PageLayout
				header={<PlayerBreadcrumb playerName={detail.player?.name} />}
			>
				{detail.state === "loading" ? (
					<p className={styles.playerDetail__message} role="status">
						Cargando jugador…
					</p>
				) : detail.state === "invalid-id" ? (
					<section className={styles.playerDetail__message} role="alert">
						<p>{DEMO_MESSAGES.invalidId}</p>
						<Link to="/players">{DEMO_MESSAGES.backToCatalog}</Link>
					</section>
				) : detail.state === "not-found" ? (
					<section className={styles.playerDetail__message} role="alert">
						<p>{DEMO_MESSAGES.notFound}</p>
						<Link to="/players">{DEMO_MESSAGES.backToCatalog}</Link>
					</section>
				) : detail.state === "error" ? (
					<section className={styles.playerDetail__message} role="alert">
						<p>{detail.errorMessage}</p>

						<button type="button" onClick={detail.retry}>
							{DEMO_MESSAGES.retry}
						</button>

						<Link to="/players">{DEMO_MESSAGES.backToCatalog}</Link>
					</section>
				) : (
					detail.player && (
						<div className={styles.playerDetail__grid}>
							<PlayerOverview
								key={detail.player.id}
								className={styles.playerDetail__overview}
								player={detail.player}
								sampleSeason={detail.sampleSeason}
								quote={detail.tokenQuote?.snapshot}
							/>

							<div className={styles.playerDetail__sidebar}>
								<PlayerSportsSummary
									className={styles.playerDetail__sports}
									sampleSeason={detail.sampleSeason}
								/>

								<PlayerQuotePanel
									className={styles.playerDetail__quotes}
									snapshot={detail.tokenQuote?.snapshot}
									loading={!detail.tokenQuote}
								/>
							</div>

							<PlayerQuoteEvolutionChart
								className={styles.playerDetail__evolution}
								series={detail.tokenQuote}
							/>
						</div>
					)
				)}
			</PageLayout>
		</div>
	);
}
