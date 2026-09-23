import styles from "./AuthVisualPanel.module.scss";

export function AuthVisualPanel() {
  return (
    <aside className={styles.panel} aria-label="El mercado de Football Market">
      <p className={styles.eyebrow}>EL JUEGO EMPIEZA CONTIGO</p>
      <h2>Visión de juego.<br />Mentalidad de manager.</h2>
      <p className={styles.description}>El fútbol se vive en el campo. Tu próximo equipo empieza con una decisión.</p>
      <div className={styles.scene} aria-hidden="true">
        <div className={styles.pitch}><div /></div>
        <div className={`${styles.playerCard} ${styles.backCard}`}>
          <span>08</span><div className={styles.shirt}>FM</div><p>CREATIVIDAD</p>
        </div>
        <div className={styles.playerCard}>
          <div className={styles.cardTop}><span>09</span><span>DEL</span></div>
          <div className={styles.shirt}>FM</div>
          <p>TU PRÓXIMO FICHAJE</p>
          <div className={styles.stats}><span>RIT<strong>92</strong></span><span>TIR<strong>89</strong></span><span>PAS<strong>86</strong></span></div>
        </div>
        <div className={styles.ball}>✦</div>
      </div>
      <div className={styles.caption}><span /> CONSTRUYE TU VISIÓN DEL FÚTBOL</div>
    </aside>
  );
}
