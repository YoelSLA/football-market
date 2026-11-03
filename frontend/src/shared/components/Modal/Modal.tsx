import { type ReactNode, type RefObject, useId } from "react";
import { createPortal } from "react-dom";
import { useOverlayA11y } from "@/shared/hooks";
import styles from "./Modal.module.scss";

export interface ModalProps {
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
}

export default function Modal({
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
}: ModalProps) {
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

	const dialogClassName = className
		? `${styles.modal__dialog} ${className}`
		: styles.modal__dialog;

	return createPortal(
		<div className={styles.modal__overlay}>
			<button
				type="button"
				className={styles.modal__backdrop}
				aria-label="Cerrar diálogo al pulsar fuera"
				tabIndex={-1}
				disabled={!closeOnOverlayClick || isBusy}
				onClick={onClose}
			/>
			<div
				ref={dialogRef}
				role="dialog"
				aria-modal="true"
				aria-labelledby={title ? titleId : undefined}
				aria-label={!title ? ariaLabel || "Diálogo" : undefined}
				aria-busy={isBusy ? "true" : undefined}
				tabIndex={-1}
				onKeyDown={handleKeyDown}
				className={dialogClassName}
			>
				{title && (
					<h2 id={titleId} className={styles.modal__accessibleTitle}>
						{title}
					</h2>
				)}
				{children}
			</div>
		</div>,
		document.body,
	);
}
