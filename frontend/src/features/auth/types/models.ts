export interface LoginForm {
	email: string;
	password: string;
}

export interface RegisterForm extends LoginForm {
	passwordConfirmation: string;
}

export type CurrentUser = {
	id: string;
	email: string;
}
