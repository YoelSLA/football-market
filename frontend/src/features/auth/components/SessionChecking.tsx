import { useAuth } from "../hooks/useAuth";
import styles from "./SessionChecking.module.scss";

export function SessionChecking() {
  const { verificationError, retry } = useAuth();
  return (
    <main className={styles.screen}>
      <section className={styles.card} aria-busy={!verificationError}>
        <p className={styles.brand}>FOOTBALL MARKET</p>
        <h1>Comprobando tu sesión</h1>
        {verificationError ? <>
          <p role="alert">{verificationError}</p>
          <button type="button" onClick={retry}>Reintentar comprobación</button>
        </> : <p role="status">Un momento, estamos verificando tu acceso…</p>}
      </section>
    </main>
  );
}
