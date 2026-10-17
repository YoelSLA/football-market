import type { UserRole } from "./models";

export type CredentialsDTO = {
	email: string;
	password: string;
};

export type RegisterRequestDTO = {
	username: string;
	email: string;
	password: string;
};

export type LoginResponseDTO = {
	token: string;
};

export type CurrentUserDTO = {
	id: number;
	username: string;
	email: string;
	role: UserRole;
};
