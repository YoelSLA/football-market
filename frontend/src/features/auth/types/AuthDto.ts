export interface CredentialsDto {
  email: string;
  password: string;
}

export interface LoginResponseDto {
  token: string;
}

export interface CurrentUserDto {
  id: string;
  email: string;
}
