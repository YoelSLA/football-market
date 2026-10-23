import { Link, type Location, NavLink } from "react-router-dom";
import Button from "@/shared/components/Button";
import styles from "./Navbar.module.scss";
import NavIcon from "./NavIcon";
import SoonBadge from "./SoonBadge";
import TabLink from "./TabLink";
import type { NavigationResolver, NavSection } from "./types";
import { disabledSectionClasses, sectionClasses } from "./utils";

interface Props {
	sections: NavSection[];
	location: Pick<Location, "pathname">;
	resolveActiveNavigation: NavigationResolver;
	userName: string | null;
	onLogout: () => void;
}

/** Compone la navegación y deriva su estado mediante el resolver de app. */
export default function Navbar({
	sections,
	location,
	resolveActiveNavigation,
	userName,
	onLogout,
}: Props) {
	const active = resolveActiveNavigation(sections, location.pathname);
	const activeSection = sections.find(
		(section) => section.key === active.sectionKey,
	);
	const userInitial = userName?.trim().charAt(0).toUpperCase() || "U";

	return (
		<header className={styles.navbar}>
			<div className={styles.navbar__top}>
				<Link to="/home" className={styles.navbar__brand}>
					<NavIcon name="brand" />
					<span>Football Market</span>
				</Link>
				<nav className={styles.navbar__sections} aria-label="Secciones">
					<ul className={styles.navbar__sectionList}>
						{sections.map((section) => (
							<li key={section.key} className={styles.navbar__sectionItem}>
								{section.path ? (
									<NavLink
										to={section.path}
										className={sectionClasses(
											section.key === active.sectionKey,
										)}
										aria-current={
											section.key === active.sectionKey ? "page" : undefined
										}
									>
										<NavIcon name={section.icon} />
										<span>{section.label}</span>
									</NavLink>
								) : (
									<span
										className={disabledSectionClasses()}
										aria-disabled="true"
									>
										<NavIcon name={section.icon} />
										<span>{section.label}</span>
										<SoonBadge />
									</span>
								)}
							</li>
						))}
					</ul>
				</nav>
				<div className={styles.navbar__account}>
					<span className={styles.navbar__avatar} aria-hidden="true">
						{userInitial}
					</span>
					<span
						className={styles.navbar__userName}
						title={userName ?? undefined}
					>
						{userName}
					</span>
					<Button className={styles.navbar__logout} onClick={onLogout}>
						Cerrar sesión
					</Button>
				</div>
			</div>
			{activeSection && activeSection.tabs.length > 0 && (
				<nav
					className={styles.navbar__tabs}
					aria-label={`Opciones de ${activeSection.label}`}
				>
					<ul className={styles.navbar__tabList}>
						{activeSection.tabs.map((tab) => (
							<li key={tab.key} className={styles.navbar__tabItem}>
								<TabLink tab={tab} active={tab.key === active.tabKey} />
							</li>
						))}
					</ul>
				</nav>
			)}
		</header>
	);
}
