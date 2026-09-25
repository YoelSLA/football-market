import axios from "axios";
import { http } from "./http";
import type { HttpCredentials } from "./types";

export function configureHttpCredentials(credentials: HttpCredentials) {
	const request = http.interceptors.request.use((config) => {
		if (config.authenticated) {
			const token = credentials.getToken();

			if (token) {
				config.headers.set("Authorization", `Bearer ${token}`);
			} else {
				credentials.onUnauthorized();
				throw new Error("Authenticated request has no token");
			}
		}

		return config;
	});

	const response = http.interceptors.response.use(
		undefined,
		(error: unknown) => {
			if (!axios.isAxiosError<unknown>(error)) {
				return Promise.reject(error);
			}

			if (
				error.response?.status === 401 &&
				error.config?.authenticated &&
				error.config.headers.get("Authorization") ===
					`Bearer ${credentials.getToken()}`
			) {
				credentials.onUnauthorized();
			}

			return Promise.reject(error);
		},
	);

	return () => {
		http.interceptors.request.eject(request);
		http.interceptors.response.eject(response);
	};
}
