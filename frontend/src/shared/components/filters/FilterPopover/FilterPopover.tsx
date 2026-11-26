import {
	type ReactNode,
	useCallback,
	useEffect,
	useId,
	useRef,
	useState,
} from "react";
import { SlidersHorizontal, X } from "@/shared/icons";
import styles from "./FilterPopover.module.scss";

export interface FilterPopoverProps {
	/**
	 * Contenido a mostrar dentro del panel de filtros secundarios.
	 */
	children: ReactNode;

	/**
	 * Texto del botón disparador.
	 * @default "Más filtros"
	 */
	triggerLabel?: string;

	/**
	 * Título accesible y encabezado del panel desplegable.
	 * @default "Filtros avanzados"
	 */
	title?: string;

	/**
	 * Número de filtros activos controlados por este popover.
	 * Si es mayor que 0, se renderiza un badge numérico en el botón disparador.
	 * @default 0
	 */
	activeCount?: number;

	/**
	 * Estado de apertura controlado (opcional).
	 */
	isOpen?: boolean;

	/**
	 * Notificación de cambio de apertura (opcional).
	 */
	onOpenChange?: (open: boolean) => void;

	/**
	 * Callback para restablecer los filtros contenidos dentro de este panel.
	 */
	onClear?: () => void;

	/**
	 * Texto del botón de limpieza.
	 * @default "Limpiar"
	 */
	clearLabel?: string;

	/**
	 * Callback opcional al pulsar el botón de confirmación / cierre.
	 */
	onApply?: () => void;

	/**
	 * Texto del botón de confirmación / cierre.
	 * @default "Listo"
	 */
	applyLabel?: string;

	/**
	 * Alineación horizontal del panel con respecto al botón disparador.
	 * - "end": alinea el borde derecho del panel con el del botón (recomendado para toolbars).
	 * - "start": alinea el borde izquierdo del panel con el del botón.
	 * @default "end"
	 */
	align?: "start" | "end";

	/**
	 * Ancho predeterminado del panel flotante.
	 * - "sm": ~280px
	 * - "md": ~340px (predeterminado)
	 * - "lg": ~420px
	 * @default "md"
	 */
	panelWidth?: "sm" | "md" | "lg";

	/**
	 * Indica si se debe mostrar el encabezado del panel.
	 * @default true
	 */
	showHeader?: boolean;

	/**
	 * Indica si se debe mostrar el pie de página con acciones (Limpiar / Listo).
	 * @default true
	 */
	showFooter?: boolean;

	/**
	 * Clase CSS adicional para el contenedor principal.
	 */
	className?: string;

	/**
	 * Clase CSS adicional para el panel flotante.
	 */
	panelClassName?: string;

	/**
	 * Deshabilita el botón disparador.
	 * @default false
	 */
	disabled?: boolean;
}

const PANEL_WIDTH_CLASSES: Record<"sm" | "md" | "lg", string> = {
	sm: styles.filterPopover__panel_widthSm,
	md: styles.filterPopover__panel_widthMd,
	lg: styles.filterPopover__panel_widthLg,
};

export default function FilterPopover({
	children,
	triggerLabel = "Más filtros",
	title = "Filtros avanzados",
	activeCount = 0,
	isOpen: controlledOpen,
	onOpenChange,
	onClear,
	clearLabel = "Limpiar",
	onApply,
	applyLabel = "Listo",
	align = "end",
	panelWidth = "md",
	showHeader = true,
	showFooter = true,
	className,
	panelClassName,
	disabled = false,
}: FilterPopoverProps) {
	const [internalOpen, setInternalOpen] = useState(false);
	const isControlled = controlledOpen !== undefined;
	const open = isControlled ? controlledOpen : internalOpen;

	const containerRef = useRef<HTMLDivElement | null>(null);
	const triggerRef = useRef<HTMLButtonElement | null>(null);

	const baseId = useId().replace(/:/g, "");
	const triggerId = `filter-popover-trigger-${baseId}`;
	const panelId = `filter-popover-panel-${baseId}`;
	const titleId = `filter-popover-title-${baseId}`;

	const hasActiveFilters = activeCount > 0;

	const setOpen = useCallback(
		(nextOpen: boolean) => {
			if (!isControlled) {
				setInternalOpen(nextOpen);
			}
			onOpenChange?.(nextOpen);
		},
		[isControlled, onOpenChange],
	);

	const handleToggle = () => {
		if (disabled) return;
		setOpen(!open);
	};

	const handleClose = useCallback(() => {
		setOpen(false);
		triggerRef.current?.focus();
	}, [setOpen]);

	const handleApply = () => {
		onApply?.();
		handleClose();
	};

	useEffect(() => {
		if (!open) return;

		const handlePointerDown = (event: PointerEvent) => {
			const target = event.target as Node | null;
			if (!target) return;

			// Si el clic ocurrió dentro de este contenedor (botón o panel), no cerramos
			if (containerRef.current?.contains(target)) {
				return;
			}

			// Soporte para sub-popovers nativos (ej. FilterSelect con popover="auto")
			const closestPopover = (target as Element).closest?.("[popover]");
			if (closestPopover && containerRef.current?.contains(closestPopover)) {
				return;
			}

			handleClose();
		};

		const handleKeyDown = (event: globalThis.KeyboardEvent) => {
			if (event.key === "Escape") {
				event.preventDefault();
				handleClose();
			}
		};

		document.addEventListener("pointerdown", handlePointerDown);
		document.addEventListener("keydown", handleKeyDown);

		return () => {
			document.removeEventListener("pointerdown", handlePointerDown);
			document.removeEventListener("keydown", handleKeyDown);
		};
	}, [open, handleClose]);

	const triggerClasses = [
		styles.filterPopover__trigger,
		hasActiveFilters && styles.filterPopover__trigger_active,
	]
		.filter(Boolean)
		.join(" ");

	const alignClass =
		align === "start"
			? styles.filterPopover__panel_alignStart
			: styles.filterPopover__panel_alignEnd;

	const panelClasses = [
		styles.filterPopover__panel,
		alignClass,
		PANEL_WIDTH_CLASSES[panelWidth],
		panelClassName,
	]
		.filter(Boolean)
		.join(" ");

	const rootClasses = [styles.filterPopover, className]
		.filter(Boolean)
		.join(" ");

	return (
		<div ref={containerRef} className={rootClasses}>
			<button
				ref={triggerRef}
				id={triggerId}
				type="button"
				className={triggerClasses}
				disabled={disabled}
				aria-haspopup="dialog"
				aria-expanded={open}
				aria-controls={open ? panelId : undefined}
				onClick={handleToggle}
			>
				<SlidersHorizontal
					className={styles.filterPopover__icon}
					aria-hidden="true"
				/>
				<span className={styles.filterPopover__label}>{triggerLabel}</span>
				{hasActiveFilters && (
					<>
						<span className={styles.filterPopover__badge} aria-hidden="true">
							{activeCount}
						</span>
						<span className={styles.filterPopover__srOnly}>
							({activeCount}{" "}
							{activeCount === 1 ? "filtro aplicado" : "filtros aplicados"})
						</span>
					</>
				)}
			</button>

			{open && (
				<div
					id={panelId}
					role="dialog"
					aria-modal="false"
					aria-labelledby={showHeader ? titleId : undefined}
					aria-label={!showHeader ? title : undefined}
					className={panelClasses}
				>
					{showHeader && (
						<header className={styles.filterPopover__header}>
							<h3 id={titleId} className={styles.filterPopover__title}>
								<SlidersHorizontal aria-hidden="true" />
								<span>{title}</span>
							</h3>
							<div className={styles.filterPopover__headerActions}>
								{hasActiveFilters && onClear && (
									<button
										type="button"
										className={styles.filterPopover__headerClear}
										onClick={onClear}
									>
										{clearLabel}
									</button>
								)}
								<button
									type="button"
									className={styles.filterPopover__close}
									onClick={handleClose}
									aria-label="Cerrar filtros"
								>
									<X aria-hidden="true" />
								</button>
							</div>
						</header>
					)}

					<div className={styles.filterPopover__body}>{children}</div>

					{showFooter && (
						<footer className={styles.filterPopover__footer}>
							{onClear ? (
								<button
									type="button"
									className={styles.filterPopover__footerClear}
									disabled={!hasActiveFilters}
									onClick={onClear}
								>
									{clearLabel}
								</button>
							) : (
								<div />
							)}
							<button
								type="button"
								className={styles.filterPopover__apply}
								onClick={handleApply}
							>
								{applyLabel}
							</button>
						</footer>
					)}
				</div>
			)}
		</div>
	);
}
