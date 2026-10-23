/**
 * Iconos disponibles para la navegacion.
 *
 * El conjunto es cerrado: cada clave tiene un icono Lucide asociado en
 * `NavIcon`. Agregar un icono nuevo exige sumar su clave aqui.
 */
export type NavIconKey = "home" | "players" | "market" | "portfolio" | "brand";

/**
 * Pestana interna de una seccion de la navegacion.
 */
export interface NavTab {
	/** Identificador estable de la pestana. */
	key: string;
	/** Etiqueta visible de la pestana. */
	label: string;
	/**
	 * Ruta real de la pantalla que la pestana abre.
	 *
	 * Es `null` cuando la funcionalidad es futura: la pestana se
	 * muestra deshabilitada, rotulada "Proximamente", sin ruta ni
	 * navegacion.
	 */
	path: string | null;
}

/**
 * Seccion principal de la navegacion.
 */
export interface NavSection {
	/** Identificador estable de la seccion. */
	key: string;
	/** Etiqueta visible de la seccion. */
	label: string;
	/** Icono que representa la seccion. */
	icon: NavIconKey;
	/**
	 * Ruta real de la pantalla de la seccion.
	 *
	 * Es `null` cuando la seccion es futura: se muestra deshabilitada,
	 * rotulada "Proximamente", sin ruta ni navegacion.
	 */
	path: string | null;
	/** Pestanas internas de la seccion. */
	tabs: NavTab[];
}

/** Estado de presentación derivado de la ruta actual. */
export interface ActiveNavigation {
	sectionKey: string | null;
	tabKey: string | null;
}

/** Contrato del resolver inyectado por la capa de aplicación. */
export type NavigationResolver = (
	sections: NavSection[],
	pathname: string,
) => ActiveNavigation;
