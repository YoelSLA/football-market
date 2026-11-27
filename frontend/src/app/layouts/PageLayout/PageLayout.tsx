import type { ReactNode } from "react";
import styles from "./PageLayout.module.scss";

interface Props {
	children: ReactNode;
}

export default function PageLayout({ children }: Props) {
	return <div className={styles.pageLayout}>{children}</div>;
}
