/**
 * Punto unico de exportacion de los iconos de la aplicacion.
 *
 * Regla: ningun modulo importa de `lucide-react` directamente. Todos
 * consumen este modulo, de modo que el inventario de iconos que usa
 * Football Market queda declarado en un solo lugar y un cambio de
 * biblioteca se resuelve aqui y no en cada consumidor.
 *
 * Convenciones:
 * - Se exporta cada icono una sola vez, con su nombre canonico.
 * - El consumidor da su significato al icono en su propio contexto:
 *   la etiqueta, el texto alternativo y el rol en la interfaz.
 * - `LucideIcon` es el tipo de un icono y se exporta junto a ellos,
 *   para que ningun consumidor dependa de la biblioteca.
 *
 * Anadir un icono: se agrega aqui su reexportacion y se consume
 * desde `@/shared/icons`.
 */
export {
	Activity,
	ArrowRight,
	BarChart3,
	ChartNoAxesCombined,
	ChartPie,
	CheckCircle2,
	ChevronLeft,
	ChevronRight,
	Circle,
	CircleDot,
	Clock,
	Coins,
	Database,
	FileText,
	House,
	IdCard,
	Info,
	LayoutGrid,
	List,
	Lock,
	type LucideIcon,
	Package,
	Receipt,
	ShoppingCart,
	Sigma,
	Store,
	TrendingDown,
	TrendingUp,
	Trophy,
	UsersRound,
	Wallet,
	Zap,
} from "lucide-react";
