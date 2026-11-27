import { isValidElement, type ReactNode } from "react";
import type { LucideIcon } from "@/shared/icons";
import styles from "./PageHeading.module.scss";

export interface PageHeadingProps {
	title: string;
	description?: string | ReactNode;
	icon?: LucideIcon | ReactNode;
	children?: ReactNode;
	className?: string;
}

export default function PageHeading({
	title,
	description,
	icon,
	children,
	className,
}: PageHeadingProps) {
	const classNames = [styles.pageHeading, className].filter(Boolean).join(" ");

	return (
		<div className={classNames}>
			{icon && (
				<span className={styles.pageHeading__icon} aria-hidden="true">
					{isValidElement(icon)
						? icon
						: (() => {
								const IconComponent = icon as LucideIcon;
								return <IconComponent aria-hidden="true" />;
							})()}
				</span>
			)}
			<div className={styles.pageHeading__content}>
				<h1 className={styles.pageHeading__title}>{title}</h1>
				{description && (
					<p className={styles.pageHeading__description}>{description}</p>
				)}
				{children}
			</div>
		</div>
	);
}
