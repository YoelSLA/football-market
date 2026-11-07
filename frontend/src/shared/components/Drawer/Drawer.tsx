import { type ReactNode, type RefObject, useId } from "react";
import { createPortal } from "react-dom";
import { useOverlayA11y } from "@/shared/hooks";
import styles from "./Drawer.module.scss";

export interface DrawerProps {
	isOpen: boolean;
	onClose: () => void;
	title?: string;
	ariaLabel?: string;
	children: ReactNode;
	initialFocusRef?: RefObject<HTMLElement | null>;
	triggerRef?: RefObject<HTMLElement | null>;
	closeOnEsc?: boolean;
	closeOnOverlayClick?: boolean;
	isBusy?: boolean;
	className?: string;
	position?: "right" | "left";
}

export default function Drawer({
	isOpen,
	onClose,
	title,
	ariaLabel,
	children,
	initialFocusRef,
	triggerRef,
	closeOnEsc = true,
	closeOnOverlayClick = true,
	isBusy = false,
	className,
	position = "right",
}: DrawerProps) {
	const titleId = useId();
	const { dialogRef, handleKeyDown } = useOverlayA11y({
		isOpen,
		onClose,
		initialFocusRef,
		triggerRef,
		closeOnEsc,
		isBusy,
	});

	if (!isOpen || typeof document === "undefined") {
		return null;
	}

	const panelClassName = [
		styles.drawer__panel,
		position === "left" && styles.drawer__panel_left,
		className,
	]
		.filter(Boolean)
		.join(" ");

	return createPortal(
		<div className={styles.drawer__overlay}>
			<button
				type="button"
				className={styles.drawer__backdrop}
				aria-label="Cerrar panel lateral al pulsar fuera"
				tabIndex={-1}
				disabled={!closeOnOverlayClick || isBusy}
				onClick={onClose}
			/>
			<div
				ref={dialogRef}
				role="dialog"
				aria-modal="true"
				aria-labelledby={title ? titleId : undefined}
				aria-label={!title ? ariaLabel || "Panel lateral" : undefined}
				aria-busy={isBusy ? "true" : undefined}
				tabIndex={-1}
				onKeyDown={handleKeyDown}
				className={panelClassName}
			>
				{title && (
					<h2 id={titleId} className={styles.drawer__accessibleTitle}>
						{title}
					</h2>
				)}
				{children}
			</div>
		</div>,
		document.body,
	);
}
