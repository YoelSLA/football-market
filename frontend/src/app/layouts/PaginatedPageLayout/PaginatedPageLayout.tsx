import type { ReactNode } from "react";
import PageLayout from "@/app/layouts/PageLayout";
import { Pagination } from "@/shared/components";

interface Props {
	children: ReactNode;
	header?: ReactNode;
	page?: number;
	totalPages?: number;
	onPageChange?: (page: number) => void;
}

export default function PaginatedPageLayout({
	children,
	header,
	page,
	totalPages,
	onPageChange,
}: Props) {
	const showPagination =
		page !== undefined &&
		totalPages !== undefined &&
		onPageChange !== undefined &&
		totalPages > 0;

	const footer = showPagination ? (
		<Pagination
			page={page}
			totalPages={totalPages}
			onChange={onPageChange}
			variant="capsule"
		/>
	) : undefined;

	return (
		<PageLayout header={header} footer={footer} floatingFooter>
			{children}
		</PageLayout>
	);
}
