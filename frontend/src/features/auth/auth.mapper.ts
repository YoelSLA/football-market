import { getJwtExpiration } from "@/infrastructure/storage/utils";
import type {
	CredentialsDTO,
	CurrentUser,
	CurrentUserDTO,
	LoginForm,
	LoginResponseDTO,
} from "./types";
import { AuthSession } from "@/infrastructure/storage/types";

export function toCredentialsDTO(form: LoginForm): CredentialsDTO {
	return { email: form.email, password: form.password };
}

export function toCurrentUser(dto: CurrentUserDTO): CurrentUser {
	return { id: dto.id, email: dto.email };
}

export function toAuthSession(dto: LoginResponseDTO): AuthSession {
	return { token: dto.token, expiresAt: getJwtExpiration(dto.token) };
}
