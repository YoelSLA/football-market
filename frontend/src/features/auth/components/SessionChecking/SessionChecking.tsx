import { useAuth } from "../../hooks";
import { Button } from "@/shared/components";
import styles from "./SessionChecking.module.scss";

export function SessionChecking() {
	const { verificationError, retry } = useAuth();
	return (
		<main className={styles["session-checking"]}>
			<section className={styles["session-checking__card"]} aria-busy={!verificationError}>
				<p className={styles["session-checking__brand"]}>FOOTBALL MARKET</p>
				<h1>Comprobando tu sesión</h1>
				{verificationError ? (
					<>
						<p role="alert">{verificationError}</p>
						<Button className={styles["session-checking__retry"]} type="button" onClick={retry}>
							Reintentar comprobación
						</Button>
					</>
				) : (
					<p role="status">Un momento, estamos verificando tu acceso…</p>
				)}
			</section>
		</main>
	);
}
