import { type KeyboardEvent, type RefObject, useEffect, useRef } from "react";

export const FOCUSABLE_SELECTOR =
	'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"]):not([disabled])';

export interface UseOverlayA11yOptions {
	isOpen: boolean;
	onClose: () => void;
	initialFocusRef?: RefObject<HTMLElement | null>;
	triggerRef?: RefObject<HTMLElement | null>;
	closeOnEsc?: boolean;
	isBusy?: boolean;
}

export function useOverlayA11y({
	isOpen,
	onClose,
	initialFocusRef,
	triggerRef,
	closeOnEsc = true,
	isBusy = false,
}: UseOverlayA11yOptions) {
	const dialogRef = useRef<HTMLDivElement>(null);
	const previousActiveElementRef = useRef<HTMLElement | null>(null);

	useEffect(() => {
		if (isOpen) {
			previousActiveElementRef.current =
				document.activeElement as HTMLElement | null;

			const previousOverflow = document.body.style.overflow;
			document.body.style.overflow = "hidden";

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
				document.body.style.overflow = previousOverflow;
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

	return { dialogRef, handleKeyDown };
}
