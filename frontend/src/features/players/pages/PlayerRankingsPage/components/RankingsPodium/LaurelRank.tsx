interface Props {
	rank: 1 | 2 | 3;
}

export default function LaurelRank({ rank }: Props) {
	const colors = {
		1: {
			primary: "#FBBF24",
			accent: "#F59E0B",
		},
		2: {
			primary: "#CBD5E1",
			accent: "#94A3B8",
		},
		3: {
			primary: "#F97316",
			accent: "#EA580C",
		},
	};

	const { primary } = colors[rank];

	return (
		<svg
			width="62"
			height="62"
			viewBox="0 0 64 64"
			fill="none"
			xmlns="http://www.w3.org/2000/svg"
			aria-hidden="true"
		>
			{/* Estrella superior de 5 puntas */}
			<path
				d="M32 9L33.2 12.8H37.2L34 15.1L35.2 18.9L32 16.5L28.8 18.9L30 15.1L26.8 12.8H30.8L32 9Z"
				fill={primary}
			/>

			{/* Rama izquierda de laureles */}
			<g stroke={primary} strokeWidth="1.5" strokeLinecap="round">
				<path d="M30 52C22 50 16 42 16 32C16 23 20 17 24 14" fill="none" />
			</g>
			<g fill={primary}>
				{/* Hojas izquierda */}
				<path d="M22 17C19 16 16 19 18 22C21 21 23 18 22 17Z" />
				<path d="M17 24C14 24 12 28 15 30C18 28 19 25 17 24Z" />
				<path d="M15 33C12 34 11 38 14 40C17 38 17 34 15 33Z" />
				<path d="M16 42C14 44 14 48 18 49C19 46 18 43 16 42Z" />
				<path d="M21 49C20 52 23 55 26 54C26 51 24 49 21 49Z" />

				{/* Hojas internas izquierda */}
				<path d="M24 22C23 19 26 17 28 20C27 22 25 23 24 22Z" />
				<path d="M21 30C19 28 21 25 24 27C23 29 22 31 21 30Z" />
				<path d="M20 38C19 36 20 33 23 35C22 37 21 39 20 38Z" />
				<path d="M22 45C22 43 24 41 26 43C25 45 23 46 22 45Z" />
			</g>

			{/* Rama derecha de laureles */}
			<g stroke={primary} strokeWidth="1.5" strokeLinecap="round">
				<path d="M34 52C42 50 48 42 48 32C48 23 44 17 40 14" fill="none" />
			</g>
			<g fill={primary}>
				{/* Hojas derecha */}
				<path d="M42 17C45 16 48 19 46 22C43 21 41 18 42 17Z" />
				<path d="M47 24C50 24 52 28 49 30C46 28 45 25 47 24Z" />
				<path d="M49 33C52 34 53 38 50 40C47 38 47 34 49 33Z" />
				<path d="M48 42C50 44 50 48 46 49C45 46 46 43 48 42Z" />
				<path d="M43 49C44 52 41 55 38 54C38 51 40 49 43 49Z" />

				{/* Hojas internas derecha */}
				<path d="M40 22C41 19 38 17 36 20C37 22 39 23 40 22Z" />
				<path d="M43 30C45 28 43 25 40 27C41 29 42 31 43 30Z" />
				<path d="M44 38C45 36 44 33 41 35C42 37 43 39 44 38Z" />
				<path d="M42 45C42 43 40 41 38 43C39 45 41 46 42 45Z" />
			</g>

			{/* Lazo inferior */}
			<path
				d="M30 52C31 51 33 51 34 52L36 56L32 54L28 56L30 52Z"
				fill={primary}
			/>

			{/* Número en el centro */}
			<text
				x="32"
				y="37"
				textAnchor="middle"
				dominantBaseline="central"
				fill={primary}
				fontSize="22"
				fontWeight="900"
				fontFamily="system-ui, -apple-system, sans-serif"
			>
				{rank}
			</text>
		</svg>
	);
}
