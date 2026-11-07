import Button from "@/shared/components/Button";
import { ChevronLeft, ChevronRight } from "@/shared/icons";
import styles from "./PaginationNav.module.scss";

interface Props {
	direction: "previous" | "next";
	disabled: boolean;
	onClick: () => void;
}

export default function PaginationNav({ direction, disabled, onClick }: Props) {
	const isPrevious = direction === "previous";

	return (
		<Button
			unstyled
			type="button"
			className={styles.nav}
			onClick={onClick}
			disabled={disabled}
			aria-label={isPrevious ? "Página anterior" : "Página siguiente"}
		>
			{isPrevious ? (
				<ChevronLeft size={16} aria-hidden="true" />
			) : (
				<ChevronRight size={16} aria-hidden="true" />
			)}
		</Button>
	);
}
