import {
	type KeyboardEvent,
	type ReactNode,
	useEffect,
	useId,
	useMemo,
	useRef,
	useState,
} from "react";
import { Search } from "@/shared/icons";
import FilterField from "../FilterField";
import styles from "./FilterSelect.module.scss";

export interface FilterSelectOption {
	value: string;
	label: string;
	visual?: ReactNode;
	keywords?: string[];
	disabled?: boolean;
}

interface Props {
	id: string;
	label: string;
	value: string;
	options: FilterSelectOption[];
	onChange: (value: string) => void;
	className?: string;
	placeholder?: string;
	disabled?: boolean;
	searchable?: boolean;
	searchPlaceholder?: string;
	emptyMessage?: string;
}

const normalize = (value: string) =>
	value
		.normalize("NFD")
		.replace(/[\u0300-\u036f]/g, "")
		.toLocaleLowerCase("es-AR")
		.trim();

const findFirstEnabledIndex = (options: FilterSelectOption[]) =>
	Math.max(
		0,
		options.findIndex((option) => !option.disabled),
	);

const findLastEnabledIndex = (options: FilterSelectOption[]) => {
	for (let index = options.length - 1; index >= 0; index -= 1)
		if (!options[index]?.disabled) return index;
	return 0;
};

export default function FilterSelect({
	id,
	label,
	value,
	options,
	onChange,
	className,
	placeholder = "Seleccionar",
	disabled = false,
	searchable = false,
	searchPlaceholder = "Buscar…",
	emptyMessage = "No hay opciones coincidentes.",
}: Props) {
	const generatedId = useId().replace(/:/g, "");
	const baseId = `${id}-${generatedId}`;
	const popoverId = `${baseId}-popover`;
	const listId = `${baseId}-listbox`;
	const [open, setOpen] = useState(false);
	const [query, setQuery] = useState("");
	const [activeIndex, setActiveIndex] = useState(0);
	const triggerRef = useRef<HTMLButtonElement | null>(null);
	const popoverRef = useRef<HTMLDivElement | null>(null);
	const searchRef = useRef<HTMLInputElement | null>(null);
	const optionRefs = useRef<Array<HTMLDivElement | null>>([]);
	const openingIndexRef = useRef<number | null>(null);
	const selected = options.find((option) => option.value === value) ?? null;
	const filteredOptions = useMemo(() => {
		const normalizedQuery = normalize(query);
		if (!searchable || !normalizedQuery) return options;
		return options.filter((option) =>
			normalize([option.label, ...(option.keywords ?? [])].join(" ")).includes(
				normalizedQuery,
			),
		);
	}, [options, query, searchable]);
	const activeOption = filteredOptions[activeIndex];
	const activeOptionId =
		open && activeOption ? `${listId}-option-${activeIndex}` : undefined;
	const selectOnlyComboboxProps = searchable
		? {}
		: {
				role: "combobox" as const,
				"aria-activedescendant": activeOptionId,
			};

	useEffect(() => {
		const popover = popoverRef.current;
		if (!popover) return;
		const handleToggle = (event: ToggleEvent) => {
			const isOpen = event.newState === "open";
			setOpen(isOpen);
			if (!isOpen) {
				setQuery("");
				return;
			}
			const selectedIndex = filteredOptions.findIndex(
				(option) => option.value === value && !option.disabled,
			);
			const initialIndex =
				openingIndexRef.current ?? Math.max(0, selectedIndex);
			openingIndexRef.current = null;
			setActiveIndex(initialIndex);
			window.requestAnimationFrame(() => {
				if (searchable) searchRef.current?.focus();
				else triggerRef.current?.focus();
			});
		};
		popover.addEventListener("toggle", handleToggle);
		return () => popover.removeEventListener("toggle", handleToggle);
	}, [filteredOptions, searchable, value]);

	useEffect(() => {
		if (!open) return;
		optionRefs.current[activeIndex]?.scrollIntoView({ block: "nearest" });
	}, [activeIndex, open]);

	useEffect(() => {
		if (!open || !searchable) return;
		if (filteredOptions[activeIndex] && !filteredOptions[activeIndex]?.disabled)
			return;
		setActiveIndex(findFirstEnabledIndex(filteredOptions));
	}, [activeIndex, filteredOptions, open, searchable]);

	const close = () => {
		popoverRef.current?.hidePopover();
		triggerRef.current?.focus();
	};

	const commit = (option: FilterSelectOption) => {
		if (option.disabled) return;
		onChange(option.value);
		close();
	};

	const openAt = (index: number) => {
		openingIndexRef.current = index;
		popoverRef.current?.showPopover();
	};

	const moveActive = (direction: 1 | -1) => {
		if (filteredOptions.length === 0) return;
		let nextIndex = activeIndex;
		do {
			nextIndex =
				(nextIndex + direction + filteredOptions.length) %
				filteredOptions.length;
		} while (filteredOptions[nextIndex]?.disabled && nextIndex !== activeIndex);
		setActiveIndex(nextIndex);
	};

	const handleComboboxKeyDown = (event: KeyboardEvent) => {
		if (event.key === "Tab") {
			if (open) popoverRef.current?.hidePopover();
		} else if (event.key === "Escape") {
			if (open) {
				event.preventDefault();
				close();
			}
		} else if (event.key === "ArrowDown") {
			event.preventDefault();
			if (open) moveActive(1);
			else openAt(findFirstEnabledIndex(filteredOptions));
		} else if (event.key === "ArrowUp") {
			event.preventDefault();
			if (open) moveActive(-1);
			else openAt(findLastEnabledIndex(filteredOptions));
		} else if (!searchable && open && event.key === "Home") {
			event.preventDefault();
			setActiveIndex(findFirstEnabledIndex(filteredOptions));
		} else if (!searchable && open && event.key === "End") {
			event.preventDefault();
			setActiveIndex(findLastEnabledIndex(filteredOptions));
		} else if (
			open &&
			(event.key === "Enter" || (!searchable && event.key === " "))
		) {
			event.preventDefault();
			const option = filteredOptions[activeIndex];
			if (option) commit(option);
		}
	};

	return (
		<FilterField id={id} label={label} className={className}>
			<button
				ref={triggerRef}
				id={id}
				type="button"
				className={styles.filterSelect__trigger}
				disabled={disabled}
				{...selectOnlyComboboxProps}
				aria-haspopup="listbox"
				aria-expanded={open}
				aria-controls={listId}
				popoverTarget={popoverId}
				onKeyDown={handleComboboxKeyDown}
			>
				{selected?.visual && (
					<span className={styles.filterSelect__visual} aria-hidden="true">
						{selected.visual}
					</span>
				)}
				<span className={styles.filterSelect__value}>
					{selected?.label ?? placeholder}
				</span>
				<span className={styles.filterSelect__caret} aria-hidden="true" />
			</button>
			<div
				ref={popoverRef}
				id={popoverId}
				className={styles.filterSelect__popover}
				popover="auto"
			>
				{searchable && (
					<label className={styles.filterSelect__search}>
						<Search aria-hidden="true" />
						<span className={styles.filterSelect__searchLabel}>
							Buscar {label.toLocaleLowerCase("es-AR")}
						</span>
						<input
							ref={searchRef}
							type="search"
							role="combobox"
							value={query}
							placeholder={searchPlaceholder}
							aria-autocomplete="list"
							aria-expanded={open}
							aria-controls={listId}
							aria-activedescendant={activeOptionId}
							onChange={(event) => {
								setQuery(event.target.value);
							}}
							onKeyDown={handleComboboxKeyDown}
						/>
					</label>
				)}
				<div
					id={listId}
					className={styles.filterSelect__list}
					role="listbox"
					aria-label={label}
				>
					{filteredOptions.map((option, index) => (
						<div
							ref={(element) => {
								optionRefs.current[index] = element;
							}}
							id={`${listId}-option-${index}`}
							key={option.value || "__empty__"}
							role="option"
							tabIndex={-1}
							aria-selected={index === activeIndex}
							aria-disabled={option.disabled || undefined}
							className={`${styles.filterSelect__option} ${index === activeIndex ? styles.filterSelect__option_active : ""}`}
							onMouseEnter={() => {
								if (!option.disabled) setActiveIndex(index);
							}}
							onClick={() => commit(option)}
							onKeyDown={(event) => {
								if (event.key === "Enter" || event.key === " ") {
									event.preventDefault();
									commit(option);
								}
							}}
						>
							{option.visual && (
								<span
									className={styles.filterSelect__visual}
									aria-hidden="true"
								>
									{option.visual}
								</span>
							)}
							<span>{option.label}</span>
						</div>
					))}
					{filteredOptions.length === 0 && (
						<p className={styles.filterSelect__empty}>{emptyMessage}</p>
					)}
				</div>
			</div>
		</FilterField>
	);
}
