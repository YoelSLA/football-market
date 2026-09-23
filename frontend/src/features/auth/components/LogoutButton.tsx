import { useLogout } from "../hooks/useLogout";

export function LogoutButton() {
  const logout = useLogout();
  return <button type="button" onClick={logout}>Cerrar sesión</button>;
}
