import type { ReactNode } from "react";
import { AuthVisualPanel } from "../AuthVisualPanel";
import styles from "./AuthLayout.module.scss";

export function AuthLayout({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <main className={styles["auth-layout"]}>
      <div className={styles["auth-layout__shell"]}>
        <section className={styles["auth-layout__content"]}>
          <div className={styles["auth-layout__brand"]}>
            <span className={styles["auth-layout__brand-mark"]} aria-hidden="true">FM</span> Football Market
          </div>
          <div className={styles["auth-layout__form-area"]}>
            <header className={styles["auth-layout__header"]}>
              <h1 className={styles["auth-layout__title"]}>{title}</h1>
              <p className={styles["auth-layout__description"]}>{description}</p>
            </header>
            {children}
          </div>
          <footer className={styles["auth-layout__footer"]}>Tu próximo gran fichaje empieza aquí.</footer>
        </section>
        <AuthVisualPanel />
      </div>
    </main>
  );
}
