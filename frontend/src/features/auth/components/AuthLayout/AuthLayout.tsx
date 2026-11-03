import type { ReactNode } from "react";
import styles from "./AuthLayout.module.scss";
import AuthVisualPanel from "./AuthVisualPanel";

interface Props {
	title: string;
	description: string;
	children: ReactNode;
}

export default function AuthLayout({ title, description, children }: Props) {
	return (
		<main className={styles.authLayout}>
			<div className={styles.authLayout__shell}>
				<section className={styles.authLayout__content}>
					<div className={styles.authLayout__brand}>
						<span className={styles.authLayout__brandMark} aria-hidden="true">
							FM
						</span>{" "}
						Football Market
					</div>
					<div className={styles.authLayout__formArea}>
						<header className={styles.authLayout__header}>
							<h1 className={styles.authLayout__title}>{title}</h1>
							<p className={styles.authLayout__description}>{description}</p>
						</header>
						{children}
					</div>
					<footer className={styles.authLayout__footer}>
						Tu próximo gran fichaje empieza aquí.
					</footer>
				</section>
				<AuthVisualPanel />
			</div>
		</main>
	);
}
