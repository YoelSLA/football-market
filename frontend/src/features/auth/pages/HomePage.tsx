import { LogoutButton } from "../components/LogoutButton";
import styles from "./HomePage.module.scss";

export function HomePage() {
  return (
    <main className={styles.screen}>
      <section className={styles.card}>
        <p className={styles.brand}>FOOTBALL MARKET</p>
        <span className={styles.badge}>Sesión verificada</span>
        <h1>¡Ya estás dentro!</h1>
        <p>Has accedido correctamente a Football Market. Este es tu espacio de bienvenida temporal.</p>
        <LogoutButton />
      </section>
    </main>
  );
}
