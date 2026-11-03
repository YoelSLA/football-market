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
 * - El consumidor da su significado al icono en su propio contexto:
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
	Award,
	BarChart3,
	CalendarDays,
	ChartNoAxesCombined,
	ChartPie,
	CheckCircle2,
	ChevronLeft,
	ChevronRight,
	Circle,
	CircleAlert,
	CircleDot,
	CircleX,
	Clock,
	Coins,
	Crown,
	Database,
	FileText,
	Footprints,
	Globe,
	Goal,
	Heart,
	History,
	House,
	IdCard,
	Info,
	Layers3,
	LayoutDashboard,
	LayoutGrid,
	List,
	LoaderCircle,
	Lock,
	type LucideIcon,
	Medal,
	Minus,
	MoreVertical,
	Package,
	Plus,
	Puzzle,
	Receipt,
	RotateCw,
	Search,
	Shield,
	ShoppingCart,
	Sigma,
	SlidersHorizontal,
	Star,
	Store,
	Tag,
	TrendingDown,
	TrendingUp,
	Trophy,
	UsersRound,
	Wallet,
	X,
	Zap,
} from "lucide-react";
