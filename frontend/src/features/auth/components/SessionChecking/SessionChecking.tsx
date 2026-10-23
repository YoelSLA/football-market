import { useAuthStore } from "@/infrastructure/storage";

import styles from "./SessionChecking.module.scss";

export default function SessionChecking() {
	const verificationError = useAuthStore((state) => state.verificationError);

	return (
		<main className={styles.sessionChecking}>
			<section
				className={styles.sessionChecking__card}
				aria-busy={!verificationError}
			>
				<p className={styles.sessionChecking__brand}>FOOTBALL MARKET</p>

				<h1>Comprobando tu sesión</h1>

				{verificationError ? (
					<p role="alert">{verificationError}</p>
				) : (
					<p role="status">Un momento, estamos verificando tu acceso…</p>
				)}
			</section>
		</main>
	);
}
