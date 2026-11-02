import type { ReactNode } from "react";
import styles from "./RankingContent.module.scss";

interface Props {
	children: ReactNode;
	isLoading?: boolean;
}

export default function RankingContent({ children, isLoading = false }: Props) {
	return (
		<main
			className={styles.content}
			aria-busy={isLoading}
			aria-label="Página de ranking de jugadores"
		>
			{children}
		</main>
	);
}
