import { useId } from "react";
import styles from "./PatrimonySparkline.module.scss";

export interface PatrimonySparklineProps {
	data?: number[];
	trend?: "up" | "down" | "neutral";
	width?: number;
	height?: number;
	className?: string;
}

export function PatrimonySparkline({
	data = [],
	trend = "up",
	width = 120,
	height = 40,
	className,
}: PatrimonySparklineProps) {
	const gradientId = useId();

	if (!data || data.length < 2) {
		return (
			<div
				className={`${styles.container} ${className ?? ""}`.trim()}
				aria-hidden="true"
			>
				<svg
					width={width}
					height={height}
					viewBox={`0 0 ${width} ${height}`}
					className={styles.svg}
					role="img"
					aria-label="Sparkline plano"
				>
					<title>Evolución plana</title>
					<line
						x1={4}
						y1={height / 2}
						x2={width - 4}
						y2={height / 2}
						className={styles.flatLine}
						strokeDasharray="2,2"
					/>
				</svg>
			</div>
		);
	}

	const min = Math.min(...data);
	const max = Math.max(...data);
	const range = max - min || 1;
	const padding = 4;
	const usableHeight = height - padding * 2;
	const usableWidth = width - padding * 2;

	const points = data.map((val, idx) => {
		const x = padding + (usableWidth / (data.length - 1)) * idx;
		const y = height - padding - ((val - min) / range) * usableHeight;
		return { x, y };
	});

	const pathD = points.reduce((acc, pt, idx) => {
		return idx === 0 ? `M ${pt.x} ${pt.y}` : `${acc} L ${pt.x} ${pt.y}`;
	}, "");

	const lastPt = points[points.length - 1];
	const firstPt = points[0];
	const areaD = `${pathD} L ${lastPt.x} ${height} L ${firstPt.x} ${height} Z`;

	const trendClass =
		trend === "up"
			? styles.trendUp
			: trend === "down"
				? styles.trendDown
				: styles.trendNeutral;

	return (
		<div
			className={`${styles.container} ${trendClass} ${className ?? ""}`.trim()}
			aria-hidden="true"
		>
			<svg
				width={width}
				height={height}
				viewBox={`0 0 ${width} ${height}`}
				className={styles.svg}
				role="img"
				aria-label="Sparkline de tendencia de patrimonio"
			>
				<title>Evolución de patrimonio</title>
				<defs>
					<linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
						<stop offset="0%" className={styles.areaStopTop} />
						<stop offset="100%" className={styles.areaStopBottom} />
					</linearGradient>
				</defs>
				<path d={areaD} fill={`url(#${gradientId})`} className={styles.area} />
				<path d={pathD} fill="none" className={styles.line} strokeWidth="2" />
				<circle
					cx={lastPt.x}
					cy={lastPt.y}
					r="3"
					className={styles.latestDot}
				/>
			</svg>
		</div>
	);
}
