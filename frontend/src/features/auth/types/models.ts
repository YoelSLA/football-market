export type UserRole = "USER" | "SUPERUSER";

export interface LoginForm {
	email: string;
	password: string;
}

export interface RegisterForm extends LoginForm {
	username: string;
	passwordConfirmation: string;
}

export type CurrentUser = {
	id: number;
	username: string;
	email: string;
	role: UserRole;
};
