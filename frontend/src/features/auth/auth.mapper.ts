import type { AuthSession } from "@/infrastructure/storage/types";
import { getJwtExpiration } from "@/infrastructure/storage/utils";
import type {
	CredentialsDTO,
	CurrentUser,
	CurrentUserDTO,
	LoginForm,
	LoginResponseDTO,
	RegisterForm,
	RegisterRequestDTO,
} from "./types";

export function toCredentialsDTO(form: LoginForm): CredentialsDTO {
	return { email: form.email, password: form.password };
}

export function toRegisterRequestDTO(form: RegisterForm): RegisterRequestDTO {
	return {
		username: form.username,
		email: form.email,
		password: form.password,
	};
}

export function toCurrentUser(dto: CurrentUserDTO): CurrentUser {
	return {
		id: dto.id,
		username: dto.username,
		email: dto.email,
		role: dto.role,
	};
}

export function toAuthSession(dto: LoginResponseDTO): AuthSession {
	return { token: dto.token, expiresAt: getJwtExpiration(dto.token) };
}
