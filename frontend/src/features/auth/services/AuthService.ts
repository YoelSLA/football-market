import { http } from "@/infrastructure/http";
import { toAuthSession, toCredentialsDTO, toCurrentUser } from "../mappers";
import type {
	AuthSession,
	CurrentUser,
	CurrentUserDTO,
	LoginForm,
	LoginResponseDTO,
	RegisterForm,
} from "../types";

async function register(form: RegisterForm): Promise<void> {
	await http.post<void>("/auth/register", toCredentialsDTO(form));
}

async function login(form: LoginForm): Promise<AuthSession> {
	const { data } = await http.post<LoginResponseDTO>(
		"/auth/login",
		toCredentialsDTO(form),
	);
	return toAuthSession(data);
}

async function currentUser(signal: AbortSignal): Promise<CurrentUser> {
	const { data } = await http.get<CurrentUserDTO>("/auth/me", {
		authenticated: true,
		signal,
	});
	return toCurrentUser(data);
}

export const authService = { register, login, currentUser };
