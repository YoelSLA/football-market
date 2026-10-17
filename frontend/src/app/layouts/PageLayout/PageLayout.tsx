import type { ReactNode } from "react";
import styles from "./PageLayout.module.scss";

type Props = {
	children: ReactNode;
	header?: ReactNode;
	footer?: ReactNode;
	/** Permite que el footer se incruste en la banda inferior sin superponerse. */
	floatingFooter?: boolean;
};

export default function PageLayout({
	children,
	header,
	footer,
	floatingFooter = false,
}: Props) {
	return (
		<section
			className={
				floatingFooter
					? `${styles.layout} ${styles["layout--floating"]}`
					: styles.layout
			}
		>
			{header && <header className={styles.layout__header}>{header}</header>}

			<div className={styles.layout__content}>{children}</div>

			{footer && <footer className={styles.layout__footer}>{footer}</footer>}
		</section>
	);
}
