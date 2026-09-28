import { Link } from "react-router-dom";
import { LogoutButton } from "../../components";
import styles from "./HomePage.module.scss";

export function HomePage() {
	return (
		<main className={styles["home-page"]}>
			<section className={styles["home-page__card"]}>
				<p className={styles["home-page__brand"]}>FOOTBALL MARKET</p>
				<span className={styles["home-page__badge"]}>Sesión verificada</span>
				<h1>¡Ya estás dentro!</h1>
				<p>
					Has accedido correctamente a Football Market. Este es tu espacio de
					bienvenida temporal.
				</p>
				<div className={styles["home-page__actions"]}>
					<Link className={styles["home-page__players-link"]} to="/players">
						Ver jugadores
					</Link>
					<LogoutButton className={styles["home-page__logout"]} />
				</div>
			</section>
		</main>
	);
}
