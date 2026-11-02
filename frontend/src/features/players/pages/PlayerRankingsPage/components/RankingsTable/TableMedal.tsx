interface Props {
	rank: 1 | 2 | 3;
}

export default function TableMedal({ rank }: Props) {
	const colors = {
		1: {
			circle: "#F59E0B",
			ribbon: "#D97706",
			text: "#FFFFFF",
		},
		2: {
			circle: "#94A3B8",
			ribbon: "#64748B",
			text: "#FFFFFF",
		},
		3: {
			circle: "#D97706",
			ribbon: "#B45309",
			text: "#FFFFFF",
		},
	};

	const { circle, ribbon, text } = colors[rank];

	return (
		<svg
			width="22"
			height="26"
			viewBox="0 0 24 28"
			fill="none"
			xmlns="http://www.w3.org/2000/svg"
			aria-hidden="true"
		>
			{/* Cintas de la medalla */}
			<path d="M8 15L5 25L9 22L12 25L10 16" fill={ribbon} />
			<path d="M16 15L19 25L15 22L12 25L14 16" fill={ribbon} />

			{/* Círculo de la medalla */}
			<circle cx="12" cy="11" r="9" fill={circle} />

			{/* Anillo interior brillante */}
			<circle
				cx="12"
				cy="11"
				r="7.5"
				stroke="rgba(255, 255, 255, 0.3)"
				strokeWidth="1"
				fill="none"
			/>

			{/* Número del puesto */}
			<text
				x="12"
				y="12"
				textAnchor="middle"
				dominantBaseline="central"
				fill={text}
				fontSize="10"
				fontWeight="900"
				fontFamily="system-ui, -apple-system, sans-serif"
			>
				{rank}
			</text>
		</svg>
	);
}
