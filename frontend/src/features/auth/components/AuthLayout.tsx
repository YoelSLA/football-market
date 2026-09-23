import type { ReactNode } from "react";
import { AuthVisualPanel } from "./AuthVisualPanel";
import styles from "./AuthLayout.module.scss";

export function AuthLayout({ title, description, children }: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <main className={styles.background}>
      <div className={styles.shell}>
        <section className={styles.content}>
          <div className={styles.brand}><span aria-hidden="true">FM</span> Football Market</div>
          <div className={styles.formArea}>
            <header><h1>{title}</h1><p>{description}</p></header>
            {children}
          </div>
          <footer>Tu próximo gran fichaje empieza aquí.</footer>
        </section>
        <AuthVisualPanel />
      </div>
    </main>
  );
}
