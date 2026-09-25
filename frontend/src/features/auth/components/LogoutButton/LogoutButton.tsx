import { Button } from "@/shared/components";
import { useLogout } from "../../hooks";

export function LogoutButton({ className }: { className?: string }) {
	const logout = useLogout();
	return (
		<Button className={className} type="button" onClick={logout}>
			Cerrar sesión
		</Button>
	);
}
