import { Outlet, useLocation } from "react-router-dom";

import { NAV_SECTIONS, resolveActiveNavigation } from "@/app/navigation";
import { useCurrentUserQuery, useLogout } from "@/features/auth";
import { useAuthStore } from "@/infrastructure/storage";
import { Navbar } from "@/shared/components";

import styles from "./AppLayout.module.scss";

export default function AppLayout() {
	const revision = useAuthStore((state) => state.revision);
	const location = useLocation();

	const logout = useLogout();

	const currentUser = useCurrentUserQuery(true, revision, location.pathname);

	return (
		<div className={styles.appLayout}>
			<Navbar
				sections={NAV_SECTIONS}
				location={location}
				resolveActiveNavigation={resolveActiveNavigation}
				userName={currentUser.data?.username ?? null}
				onLogout={logout}
			/>

			<div className={styles.appLayout__content}>
				<Outlet />
			</div>
		</div>
	);
}
