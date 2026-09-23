import { http, HttpError } from "@/shared/http";
import { toAuthSession, toCredentialsDto, toCurrentUser } from "../mappers/authMapper";
import type { LoginForm, RegisterForm } from "../models/AuthForm";
import { type AuthSession, isSessionValid } from "../models/AuthSession";
import type { CurrentUser } from "../models/CurrentUser";
import type { CurrentUserDto, LoginResponseDto } from "../types/AuthDto";

function authError(error: unknown, operation: "login" | "register" | "currentUser"): Error {
  if (error instanceof HttpError) {
    if (operation === "currentUser") return new Error("No pudimos comprobar tu sesión. Vuelve a intentarlo.");
    if (error.status === 400) return new Error("Revisa los datos introducidos y vuelve a intentarlo.");
    if (operation === "login" && error.status === 401) return new Error("El email o la contraseña no son correctos.");
    if (operation === "register" && error.status === 409) {
      const body = error.body;
      const message = typeof body === "object" && body !== null && "message" in body
        && typeof body.message === "string" && body.message.trim() ? body.message : "Este email ya está registrado.";
      return new Error(message);
    }
    if (error.kind === "network" || error.kind === "timeout") {
      return new Error("No pudimos conectar con el servicio. Comprueba tu conexión y vuelve a intentarlo.");
    }
  }
  return new Error("No pudimos completar la operación. Vuelve a intentarlo.");
}

export const authService = {
  async register(form: RegisterForm): Promise<void> {
    try {
      await http.post<void>("/auth/register", toCredentialsDto(form));
    } catch (error) { throw authError(error, "register"); }
  },
  async login(form: LoginForm): Promise<AuthSession> {
    try {
      const { data } = await http.post<LoginResponseDto>("/auth/login", toCredentialsDto(form));
      const session = toAuthSession(data);
      if (!session || !isSessionValid(session)) throw new Error("Invalid session response");
      return session;
    } catch (error) { throw authError(error, "login"); }
  },
  async currentUser(signal: AbortSignal): Promise<CurrentUser> {
    try {
      const { data } = await http.get<CurrentUserDto>("/auth/me", { authenticated: true, signal });
      if (!data || typeof data.id !== "string" || typeof data.email !== "string") {
        throw new Error("Invalid current user response");
      }
      return toCurrentUser(data);
    } catch (error) { throw authError(error, "currentUser"); }
  },
};
