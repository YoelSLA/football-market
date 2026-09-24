export interface CredentialsDTO {
	email: string;
	password: string;
}

export interface LoginResponseDTO {
	token: string;
}

export interface CurrentUserDTO {
	id: string;
	email: string;
}
