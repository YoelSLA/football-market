export interface LoginForm {
	email: string;
	password: string;
}

export interface RegisterForm extends LoginForm {
	passwordConfirmation: string;
}

export interface AuthSession {
	token: string;
	expiresAt: number;
}

export type AuthStatus = "unknown" | "checking" | "authenticated" | "anonymous";

export interface CurrentUser {
	id: string;
	email: string;
}
