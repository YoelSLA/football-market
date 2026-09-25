export type CredentialsDTO = {
	email: string;
	password: string;
}

export type LoginResponseDTO = {
	token: string;
}

export type CurrentUserDTO = {
	id: string;
	email: string;
}
