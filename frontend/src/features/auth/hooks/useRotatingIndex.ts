import { useEffect, useState } from "react";

export function useRotatingIndex<T>(
	visuals: readonly T[],
	intervalMs: number,
): number {
	const [currentIndex, setCurrentIndex] = useState(() =>
		Math.floor(Math.random() * visuals.length),
	);

	useEffect(() => {
		if (visuals.length <= 1) return;

		const interval = window.setInterval(() => {
			setCurrentIndex((current) => (current + 1) % visuals.length);
		}, intervalMs);

		return () => window.clearInterval(interval);
	}, [visuals.length, intervalMs]);

	return currentIndex;
}
