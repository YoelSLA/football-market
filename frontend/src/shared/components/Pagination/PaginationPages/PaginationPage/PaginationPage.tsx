import Button from "@/shared/components/Button";
import styles from "./PaginationPage.module.scss";

interface Props {
	page: number;
	active: boolean;
	onClick: () => void;
}

export default function PaginationPage({ page, active, onClick }: Props) {
	const classNames = [styles.page, active && styles.page_active]
		.filter(Boolean)
		.join(" ");

	return (
		<Button
			unstyled
			type="button"
			className={classNames}
			onClick={onClick}
			aria-label={`Página ${page + 1}`}
			aria-current={active ? "page" : undefined}
		>
			{page + 1}
		</Button>
	);
}
