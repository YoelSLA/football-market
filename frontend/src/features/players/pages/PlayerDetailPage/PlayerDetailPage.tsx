import { Link, useParams } from "react-router-dom";
import { PageLayout } from "@/app/layouts";
import {
	PlayerBreadcrumb,
	PlayerOverview,
	PlayerQuoteEvolutionChart,
	PlayerQuotePanel,
	PlayerSportsSummary,
} from "../../components";
import { DEMO_MESSAGES } from "../../constants";
import { usePlayerDetailPage } from "../../hooks";

import styles from "./PlayerDetailPage.module.scss";

export function PlayerDetailPage() {
	const params = useParams();
	const detail = usePlayerDetailPage(params.id);

	return (
		<div
			className={styles["player-detail"]}
			aria-busy={detail.state === "loading"}
		>
			<PageLayout
				header={<PlayerBreadcrumb playerName={detail.player?.name} />}
			>
				{detail.state === "loading" ? (
					<p className={styles["player-detail__message"]} role="status">
						Cargando jugador…
					</p>
				) : detail.state === "invalid-id" ? (
					<section className={styles["player-detail__message"]} role="alert">
						<p>{DEMO_MESSAGES.invalidId}</p>
						<Link to="/players">{DEMO_MESSAGES.backToCatalog}</Link>
					</section>
				) : detail.state === "not-found" ? (
					<section className={styles["player-detail__message"]} role="alert">
						<p>{DEMO_MESSAGES.notFound}</p>
						<Link to="/players">{DEMO_MESSAGES.backToCatalog}</Link>
					</section>
				) : detail.state === "error" ? (
					<section className={styles["player-detail__message"]} role="alert">
						<p>{detail.errorMessage}</p>

						<button type="button" onClick={detail.retry}>
							{DEMO_MESSAGES.retry}
						</button>

						<Link to="/players">{DEMO_MESSAGES.backToCatalog}</Link>
					</section>
				) : (
					detail.player && (
						<div className={styles["player-detail__grid"]}>
							<PlayerOverview
								key={detail.player.id}
								className={styles["player-detail__overview"]}
								player={detail.player}
								sampleSeason={detail.sampleSeason}
								quote={detail.tokenQuote?.snapshot}
							/>

							<div className={styles["player-detail__sidebar"]}>
								<PlayerSportsSummary
									className={styles["player-detail__sports"]}
									sampleSeason={detail.sampleSeason}
								/>

								<PlayerQuotePanel
									className={styles["player-detail__quotes"]}
									snapshot={detail.tokenQuote?.snapshot}
									loading={!detail.tokenQuote}
								/>
							</div>

							<PlayerQuoteEvolutionChart
								className={styles["player-detail__evolution"]}
								series={detail.tokenQuote}
							/>
						</div>
					)
				)}
			</PageLayout>
		</div>
	);
}
