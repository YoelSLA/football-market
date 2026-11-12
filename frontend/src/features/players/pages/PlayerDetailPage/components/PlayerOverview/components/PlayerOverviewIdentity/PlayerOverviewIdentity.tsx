import { getLeagueClassification } from "@/features/players/constants";
import {
	formatDate,
	getStatisticsPositionLabel,
} from "@/features/players/utils";
import {
	CompetitionLogo,
	NationalityFlag,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
import { CalendarDays, Shield } from "@/shared/icons";
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
	className,
}: Props) {
	const league = getLeagueClassification(leagueName);
	const positionLabel = getStatisticsPositionLabel(position);
	const resolvedBirth = formatDate(dateOfBirth);

	return (
		<div className={`${styles.identityBlock} ${className ?? ""}`.trim()}>
			<PlayerPortrait
				className={styles.identityBlock__portrait}
				name={name}
				src={imageUrl}
				fallbackSrc={fallbackImageUrl}
				fit="contain"
			/>
			<div className={styles.identityBlock__info}>
				<h1 className={styles.identityBlock__name}>{name}</h1>

				<div className={styles.identityBlock__teamLine}>
					<span className={styles.identityBlock__team}>
						<TeamBadge teamName={teamName} leagueName={leagueName} />
						{teamName}
					</span>
					<span className={styles.identityBlock__separator} aria-hidden="true">
						|
					</span>
					<span className={styles.identityBlock__league}>
						<CompetitionLogo
							competitionName={leagueName}
							src={league.image}
							decorative
						/>
						{leagueName}
					</span>
				</div>

				<div className={styles.identityBlock__metaLine}>
					<span className={styles.identityBlock__metaItem}>
						<Shield
							size={14}
							className={styles.identityBlock__iconShield}
							aria-hidden="true"
						/>
						<span>{positionLabel}</span>
					</span>
					<span className={styles.identityBlock__separator} aria-hidden="true">
						|
					</span>
					<span className={styles.identityBlock__metaItem}>
						<NationalityFlag nationality={nationality} />
						<span>{nationality ?? "No disponible"}</span>
					</span>
					<span className={styles.identityBlock__separator} aria-hidden="true">
						|
					</span>
					<span className={styles.identityBlock__metaItem}>
						<CalendarDays
							size={14}
							className={styles.identityBlock__iconCalendar}
							aria-hidden="true"
						/>
						<span>{resolvedBirth}</span>
					</span>
				</div>
			</div>
		</div>
	);
}
