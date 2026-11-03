import { Link } from "react-router-dom";
import styles from "./HomePage.module.scss";
import LogoutButton from "./LogoutButton";

export default function HomePage() {
	return (
		<main className={styles.homePage}>
			<section className={styles.homePage__card}>
				<p className={styles.homePage__brand}>FOOTBALL MARKET</p>
				<span className={styles.homePage__badge}>Sesión verificada</span>
				<h1>¡Ya estás dentro!</h1>
				<p>
					Has accedido correctamente a Football Market. Este es tu espacio de
					bienvenida temporal.
				</p>
				<div className={styles.homePage__actions}>
					<Link className={styles.homePage__playersLink} to="/players">
						Ver jugadores
					</Link>
					<LogoutButton className={styles.homePage__logout} />
				</div>
			</section>
		</main>
	);
}
