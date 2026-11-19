import { useEffect, useState } from "react";
import styles from "./Slider.module.scss";

interface BaseProps {
	id: string;
	label: string;
	min: number;
	max: number;
	step?: number;
	formatValue?: (value: number) => string;
	disabled?: boolean;
	className?: string;
}

interface SingleProps extends BaseProps {
	variant: "single";
	value: number | null;
	allowNoLimit?: boolean;
	onCommit: (value: number | null) => void;
}

interface RangeProps extends BaseProps {
	variant: "range";
	value: readonly [number, number];
	onCommit: (value: readonly [number, number]) => void;
}

type Props = SingleProps | RangeProps;

const identity = (value: number) => String(value);

export default function Slider(props: Props) {
	const formatValue = props.formatValue ?? identity;
	const [singleValue, setSingleValue] = useState(
		props.variant === "single" ? (props.value ?? props.max) : props.max,
	);
	const [singleLimited, setSingleLimited] = useState(
		props.variant === "single" ? props.value !== null : true,
	);
	const [rangeValue, setRangeValue] = useState<readonly [number, number]>(
		props.variant === "range" ? props.value : [props.min, props.max],
	);
	const externalSingleValue = props.variant === "single" ? props.value : null;
	const externalRangeMin =
		props.variant === "range" ? props.value[0] : props.min;
	const externalRangeMax =
		props.variant === "range" ? props.value[1] : props.max;

	useEffect(() => {
		if (props.variant === "single")
			setSingleValue(externalSingleValue ?? props.max);
		else setRangeValue([externalRangeMin, externalRangeMax]);
		if (props.variant === "single")
			setSingleLimited(externalSingleValue !== null);
	}, [
		externalRangeMax,
		externalRangeMin,
		externalSingleValue,
		props.max,
		props.variant,
	]);

	const commitSingle = () => {
		if (props.variant !== "single") return;
		props.onCommit(singleValue);
	};
	const commitRange = () => {
		if (props.variant !== "range") return;
		props.onCommit(rangeValue);
	};

	return (
		<div
			className={`${styles.slider} ${
				props.variant === "single" ? styles.slider_single : ""
			} ${props.className ?? ""}`}
		>
			<div className={styles.slider__heading}>
				<span id={`${props.id}-label`}>{props.label}</span>
				<strong aria-live="polite">
					{props.variant === "single"
						? props.allowNoLimit && !singleLimited
							? "Sin límite"
							: formatValue(singleValue)
						: `${formatValue(rangeValue[0])} – ${formatValue(rangeValue[1])}`}
				</strong>
			</div>

			<div
				className={`${styles.slider__control} ${
					props.variant === "range" ? styles.slider__control_range : ""
				}`}
			>
				{props.variant === "range" && (
					<input
						id={`${props.id}-min`}
						type="range"
						min={props.min}
						max={props.max}
						step={props.step ?? 1}
						value={rangeValue[0]}
						disabled={props.disabled}
						aria-label={`${props.label}: mínimo`}
						onChange={(event) =>
							setRangeValue([
								Math.min(Number(event.target.value), rangeValue[1]),
								rangeValue[1],
							])
						}
						onPointerUp={commitRange}
						onKeyUp={commitRange}
						onBlur={commitRange}
					/>
				)}
				<input
					id={props.variant === "range" ? `${props.id}-max` : props.id}
					type="range"
					min={props.min}
					max={props.max}
					step={props.step ?? 1}
					value={props.variant === "range" ? rangeValue[1] : singleValue}
					disabled={props.disabled}
					aria-label={
						props.variant === "range" ? `${props.label}: máximo` : props.label
					}
					onChange={(event) => {
						const next = Number(event.target.value);
						if (props.variant === "range")
							setRangeValue([rangeValue[0], Math.max(next, rangeValue[0])]);
						else {
							setSingleLimited(true);
							setSingleValue(next);
						}
					}}
					onPointerUp={props.variant === "range" ? commitRange : commitSingle}
					onKeyUp={props.variant === "range" ? commitRange : commitSingle}
					onBlur={props.variant === "range" ? commitRange : commitSingle}
				/>
			</div>

			<div className={styles.slider__bounds} aria-hidden="true">
				<span>{formatValue(props.min)}</span>
				<span>{formatValue(props.max)}</span>
			</div>

			{props.variant === "single" && props.allowNoLimit && (
				<button
					type="button"
					className={styles.slider__noLimit}
					disabled={props.disabled || !singleLimited}
					onClick={() => {
						setSingleLimited(false);
						props.onCommit(null);
					}}
				>
					Sin límite
				</button>
			)}
		</div>
	);
}
