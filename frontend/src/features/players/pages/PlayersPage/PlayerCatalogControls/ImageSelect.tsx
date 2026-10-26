import { useEffect, useRef, useState } from "react";
import styles from "./ImageSelect.module.scss";

export interface ImageSelectOption {
	value: string;
	label: string;
	image?: string | null;
}

interface Props {
	label: string;
	icon?: React.ReactNode;
	value: string;
	options: ImageSelectOption[];
	disabled?: boolean;
	onChange: (value: string) => void;
	placeholder?: string;
}

export default function ImageSelect({ label, icon, value, options, disabled, onChange, placeholder }: Props) {
	const [open, setOpen] = useState(false);
	const [activeIndex, setActiveIndex] = useState(0);
	const rootRef = useRef<HTMLDivElement | null>(null);
	const buttonRef = useRef<HTMLButtonElement | null>(null);
	const listRef = useRef<HTMLUListElement | null>(null);

	const selected = options.find((option) => option.value === value) ?? null;
	const displayLabel = selected?.label ?? placeholder ?? "Todos";

	useEffect(() => {
		if (!open) return;
		const index = Math.max(0, options.findIndex((option) => option.value === value));
		setActiveIndex(index === -1 ? 0 : index);
	}, [open, options, value]);

	useEffect(() => {
		if (!open) return;
		const handlePointer = (event: MouseEvent) => {
			if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
				setOpen(false);
			}
		};
		document.addEventListener("mousedown", handlePointer);
		return () => document.removeEventListener("mousedown", handlePointer);
	}, [open]);

	useEffect(() => {
		if (open && listRef.current) {
			const item = listRef.current.children[activeIndex] as HTMLElement | undefined;
			item?.scrollIntoView({ block: "nearest" });
		}
	}, [activeIndex, open]);

	const commit = (index: number) => {
		const option = options[index];
		if (!option) return;
		onChange(option.value);
		setOpen(false);
		buttonRef.current?.focus();
	};

	const handleKeyDown = (event: React.KeyboardEvent) => {
		if (disabled) return;
		if (!open) {
			if (event.key === "ArrowDown" || event.key === "ArrowUp" || event.key === "Enter" || event.key === " ") {
				event.preventDefault();
				setOpen(true);
			}
			return;
		}
		if (event.key === "Escape") {
			event.preventDefault();
			setOpen(false);
			buttonRef.current?.focus();
			return;
		}
		if (event.key === "ArrowDown") {
			event.preventDefault();
			setActiveIndex((index) => Math.min(options.length - 1, index + 1));
			return;
		}
		if (event.key === "ArrowUp") {
			event.preventDefault();
			setActiveIndex((index) => Math.max(0, index - 1));
			return;
		}
		if (event.key === "Enter" || event.key === " ") {
			event.preventDefault();
			commit(activeIndex);
			return;
		}
		if (event.key === "Home") {
			event.preventDefault();
			setActiveIndex(0);
			return;
		}
		if (event.key === "End") {
			event.preventDefault();
			setActiveIndex(options.length - 1);
		}
	};

	const listId = `listbox-${label.replace(/\s+/g, "-").toLowerCase()}`;

	return (
		<div className={styles.imageSelect} ref={rootRef}>
			<div className={styles.imageSelect__label}>
				{icon}
				<span>{label}</span>
			</div>
			<button
				ref={buttonRef}
				type="button"
				className={styles.imageSelect__button}
				disabled={disabled}
				aria-haspopup="listbox"
				aria-expanded={open}
				aria-controls={listId}
				onClick={() => !disabled && setOpen((value) => !value)}
				onKeyDown={handleKeyDown}
			>
				{selected?.image && (
					<img className={styles.imageSelect__thumb} src={selected.image} alt="" />
				)}
				<span className={styles.imageSelect__value}>{displayLabel}</span>
				<span className={styles.imageSelect__caret} aria-hidden="true">▾</span>
			</button>
			{open && (
				<ul
					id={listId}
					ref={listRef}
					className={styles.imageSelect__list}
					role="listbox"
					aria-label={label}
					tabIndex={-1}
				>
					{options.map((option, index) => (
						<li
							key={option.value || "__all__"}
							role="option"
							aria-selected={option.value === value}
							className={
								index === activeIndex
									? `${styles.imageSelect__option} ${styles.imageSelect__option_active}`
									: styles.imageSelect__option
							}
							onMouseEnter={() => setActiveIndex(index)}
							onClick={() => commit(index)}
						>
							{option.image && (
								<img className={styles.imageSelect__optionImage} src={option.image} alt="" />
							)}
							<span>{option.label}</span>
						</li>
					))}
				</ul>
			)}
		</div>
	);
}