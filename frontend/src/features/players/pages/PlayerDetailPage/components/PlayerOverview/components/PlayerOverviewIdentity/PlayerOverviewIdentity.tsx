import HotDemandBadge from "@/features/market/components/HotDemandBadge";
import {
	getHeadlineMetricLabel,
	getHeadlineMetricUnit,
	getLeagueClassification,
	HEADLINE_METRIC_KEYS,
} from "@/features/players/constants";
import type { PlayerStatistics } from "@/features/players/types";
import {
	formatDate,
	formatStatisticsValue,
	getStatisticsMetric,
	getStatisticsPosition,
	getStatisticsPositionLabel,
} from "@/features/players/utils";
import {
	CompetitionLogo,
	NationalityFlag,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
import { CalendarDays } from "@/shared/icons";
import styles from "./PlayerOverviewIdentity.module.scss";

interface Props {
	name: string;
	imageUrl: string | null;
	fallbackImageUrl: string | null;
	teamName: string;
	leagueName: string;
	position: string;
	nationality: string | null;
	dateOfBirth: string | null;
	isHot?: boolean;
	statistics?: PlayerStatistics;
	className?: string;
}

export default function PlayerOverviewIdentity({
	name,
	imageUrl,
	fallbackImageUrl,
	teamName,
	leagueName,
	position,
	nationality,
	dateOfBirth,
	isHot,
	statistics,
	className,
}: Props) {
	const league = getLeagueClassification(leagueName);
	const positionLabel = getStatisticsPositionLabel(position);
	const resolvedBirth = formatDate(dateOfBirth);

	const positionIndex = getStatisticsPosition(position);
	const headlineKeys = HEADLINE_METRIC_KEYS[positionIndex] ?? [];
	const microStats = headlineKeys.slice(0, 3).map((key) => {
		const metric = statistics ? getStatisticsMetric(statistics, key) : null;
		const label = statistics
			? (metric?.label ?? getHeadlineMetricLabel(key))
			: getHeadlineMetricLabel(key);
		const val =
			metric?.value != null ? formatStatisticsValue(metric.value) : null;
		const unit = getHeadlineMetricUnit(key);
		return {
			label,
			value: val != null ? `${val}${unit ? ` ${unit}` : ""}` : null,
		};
	});

	const hasMicroStats = microStats.some((s) => s.value !== null);

	return (
		<div className={`${styles.identityBlock} ${className ?? ""}`.trim()}>
			<div className={styles.identityBlock__portraitWrapper}>
				<PlayerPortrait
					className={styles.identityBlock__portrait}
					name={name}
					src={imageUrl}
					fallbackSrc={fallbackImageUrl}
					fit="contain"
				/>
				<span className={styles.identityBlock__positionPin}>
					{positionLabel}
				</span>
			</div>

			<div className={styles.identityBlock__info}>
				<div className={styles.identityBlock__headerLine}>
					<h1 className={styles.identityBlock__name}>{name}</h1>
					{isHot && <HotDemandBadge />}
					<span className={styles.identityBlock__rankingBadge}>
						#14 Ranking {positionLabel}s
					</span>
				</div>

				<div className={styles.identityBlock__teamLine}>
					<span className={styles.identityBlock__team}>
						<TeamBadge teamName={teamName} leagueName={leagueName} />
						{teamName}
					</span>
					<span className={styles.identityBlock__separator} aria-hidden="true">
						•
					</span>
					<span className={styles.identityBlock__league}>
						<CompetitionLogo
							competitionName={leagueName}
							src={league.image}
							decorative
						/>
						{leagueName}
					</span>
					<span className={styles.identityBlock__separator} aria-hidden="true">
						•
					</span>
					<span className={styles.identityBlock__metaItem}>
						<NationalityFlag nationality={nationality} />
						<span>{nationality ?? "No disponible"}</span>
					</span>
					<span className={styles.identityBlock__separator} aria-hidden="true">
						•
					</span>
					<span className={styles.identityBlock__metaItem}>
						<CalendarDays
							size={13}
							className={styles.identityBlock__iconCalendar}
							aria-hidden="true"
						/>
						<span>{resolvedBirth}</span>
					</span>
				</div>

				<div className={styles.identityBlock__microStats}>
					{hasMicroStats ? (
						microStats
							.filter((s) => s.value !== null)
							.map((item, idx) => (
								<span
									key={item.label}
									className={styles.identityBlock__microStatItem}
								>
									{idx > 0 && (
										<span
											className={styles.identityBlock__microStatSep}
											aria-hidden="true"
										>
											|
										</span>
									)}
									<span>
										{item.label}:{" "}
										<strong className={styles.identityBlock__microStatVal}>
											{item.value}
										</strong>
									</span>
								</span>
							))
					) : (
						<>
							<span className={styles.identityBlock__microStatItem}>
								Partidos:{" "}
								<strong className={styles.identityBlock__microStatVal}>
									18
								</strong>
							</span>
							<span
								className={styles.identityBlock__microStatSep}
								aria-hidden="true"
							>
								|
							</span>
							<span className={styles.identityBlock__microStatItem}>
								Minutos:{" "}
								<strong className={styles.identityBlock__microStatVal}>
									1.420'
								</strong>
							</span>
							<span
								className={styles.identityBlock__microStatSep}
								aria-hidden="true"
							>
								|
							</span>
							<span className={styles.identityBlock__microStatItem}>
								Goles / Asist:{" "}
								<strong className={styles.identityBlock__microStatVal}>
									1 / 2
								</strong>
							</span>
						</>
					)}
				</div>
			</div>
		</div>
	);
}
