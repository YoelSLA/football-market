import type { LoginForm } from "../models/AuthForm";
import type { AuthSession } from "../models/AuthSession";
import type { CurrentUser } from "../models/CurrentUser";
import type { CredentialsDto, CurrentUserDto, LoginResponseDto } from "../types/AuthDto";

export function toCredentialsDto(form: LoginForm): CredentialsDto {
  return { email: form.email, password: form.password };
}

export function toCurrentUser(dto: CurrentUserDto): CurrentUser {
  return { id: dto.id, email: dto.email };
}

export function toAuthSession(dto: LoginResponseDto): AuthSession | null {
  try {
    const parts = dto.token.split(".");
    if (parts.length !== 3 || parts.some((part) => !/^[\w-]+$/.test(part))) return null;
    const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const decoded: unknown = JSON.parse(atob(payload.padEnd(Math.ceil(payload.length / 4) * 4, "=")));
    if (typeof decoded !== "object" || decoded === null || !("exp" in decoded)
      || typeof decoded.exp !== "number") return null;
    return { token: dto.token, expiresAt: decoded.exp * 1000 };
  } catch {
    return null;
  }
}
