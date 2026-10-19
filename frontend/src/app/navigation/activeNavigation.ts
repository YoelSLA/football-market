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
	const section = sections.find(
		(candidate) =>
			candidate.path !== null && belongsTo(candidate.path, pathname),
	);

	if (!section) {
		return { sectionKey: null, tabKey: null };
	}

	let tabKey: string | null = null;
	let matchedPathLength = -1;

	for (const tab of section.tabs) {
		if (tab.path === null) continue;
		if (!belongsTo(tab.path, pathname)) continue;

		if (tab.path.length > matchedPathLength) {
			matchedPathLength = tab.path.length;
			tabKey = tab.key;
		}
	}

	return { sectionKey: section.key, tabKey };
};
