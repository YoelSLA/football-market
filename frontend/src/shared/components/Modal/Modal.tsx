import {
	type KeyboardEvent,
	type ReactNode,
	type RefObject,
	useEffect,
	useId,
	useRef,
} from "react";
import { createPortal } from "react-dom";
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

const FOCUSABLE_SELECTOR =
	'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"]):not([disabled])';

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
	const dialogRef = useRef<HTMLDivElement>(null);
	const previousActiveElementRef = useRef<HTMLElement | null>(null);

	// Guardar elemento activo al abrir y restaurar foco al cerrar/desmontar
	useEffect(() => {
		if (isOpen) {
			previousActiveElementRef.current =
				document.activeElement as HTMLElement | null;

			// Dar foco inicial
			const timer = window.setTimeout(() => {
				if (initialFocusRef?.current) {
					initialFocusRef.current.focus({ preventScroll: true });
				} else if (dialogRef.current) {
					const firstFocusable =
						dialogRef.current.querySelector<HTMLElement>(FOCUSABLE_SELECTOR);
					if (firstFocusable) {
						firstFocusable.focus({ preventScroll: true });
					} else {
						dialogRef.current.focus();
					}
				}
			}, 0);

			return () => {
				window.clearTimeout(timer);
				const returnTarget =
					triggerRef?.current || previousActiveElementRef.current;
				if (
					returnTarget?.isConnected &&
					typeof returnTarget.focus === "function"
				) {
					returnTarget.focus({ preventScroll: true });
				}
			};
		}
	}, [isOpen, initialFocusRef, triggerRef]);

	if (!isOpen || typeof document === "undefined") {
		return null;
	}

	const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
		if (e.key === "Escape") {
			if (closeOnEsc && !isBusy) {
				e.preventDefault();
				e.stopPropagation();
				onClose();
			}
			return;
		}

		if (e.key === "Tab" && dialogRef.current) {
			const focusableElements = Array.from(
				dialogRef.current.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR),
			).filter(
				(el) => el.offsetParent !== null || el === document.activeElement,
			);

			if (focusableElements.length === 0) {
				e.preventDefault();
				dialogRef.current.focus();
				return;
			}

			const firstElement = focusableElements[0];
			const lastElement = focusableElements[focusableElements.length - 1];

			if (e.shiftKey) {
				if (
					document.activeElement === firstElement ||
					!dialogRef.current.contains(document.activeElement)
				) {
					e.preventDefault();
					lastElement.focus();
				}
			} else {
				if (
					document.activeElement === lastElement ||
					!dialogRef.current.contains(document.activeElement)
				) {
					e.preventDefault();
					firstElement.focus();
				}
			}
		}
	};

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
