import type { NavigationResolver } from "@/shared/components";

/**
 * Indica si una ruta pertenece a la pantalla de una seccion.
 *
 * La pertenencia se resuelve por prefijo de segmento, de modo que el
 * detalle de un jugador sigue perteneciendo a la seccion de jugadores
 * sin necesitar una pestana propia.
 */
function belongsTo(path: string, pathname: string): boolean {
	return pathname === path || pathname.startsWith(`${path}/`);
}

/**
 * Resuelve la seccion y la pestana activas a partir de la ruta en curso.
 *
 * La seccion activa es la primera cuya pantalla contiene la ruta. La
 * pestana activa es la de prefijo mas largo dentro de esa seccion, de
 * modo que la pestana mas especifica gana cuando varias coinciden.
 */
export const resolveActiveNavigation: NavigationResolver = (
	sections,
	pathname,
) => {
	const matchingSections = sections.flatMap((candidate) => {
		const matchingTabs = candidate.tabs.filter(
			(tab) => tab.path !== null && belongsTo(tab.path, pathname),
		);
		const sectionMatches =
			candidate.path !== null && belongsTo(candidate.path, pathname);

		return sectionMatches || matchingTabs.length > 0
			? [{ candidate, matchingTabs }]
			: [];
	});
	const sectionMatch = matchingSections.sort((left, right) => {
		const leftPathLength = Math.max(
			left.candidate.path?.length ?? -1,
			...left.matchingTabs.map((tab) => tab.path?.length ?? -1),
		);
		const rightPathLength = Math.max(
			right.candidate.path?.length ?? -1,
			...right.matchingTabs.map((tab) => tab.path?.length ?? -1),
		);
		return rightPathLength - leftPathLength;
	})[0];

	if (!sectionMatch) {
		return { sectionKey: null, tabKey: null };
	}

	const activeTab = sectionMatch.matchingTabs.reduce<
		(typeof sectionMatch.matchingTabs)[number] | null
	>(
		(current, tab) =>
			current === null ||
			(tab.path?.length ?? -1) > (current.path?.length ?? -1)
				? tab
				: current,
		null,
	);

	return {
		sectionKey: sectionMatch.candidate.key,
		tabKey: activeTab?.key ?? null,
	};
};
