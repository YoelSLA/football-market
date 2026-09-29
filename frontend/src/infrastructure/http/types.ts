declare module "axios" {
	interface AxiosRequestConfig {
		authenticated?: boolean;
	}
}

export interface ApiError {
	timestamp: string;
	status: number;
	error: string;
	code: string;
	message: string;
	path: string;
}

export interface HttpCredentials {
	getToken: () => string | null;
	onUnauthorized: () => void;
}
