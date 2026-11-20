import { useLogout } from "@/features/auth/hooks";
import { Button } from "@/shared/components";

interface Props {
	className?: string;
}

export default function LogoutButton({ className }: Props) {
	const logout = useLogout();
	return (
		<Button className={className} type="button" onClick={logout}>
			Cerrar sesión
		</Button>
	);
}
