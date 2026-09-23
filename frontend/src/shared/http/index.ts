import axios from "axios";

declare module "axios" {
  interface AxiosRequestConfig {
    authenticated?: boolean;
  }
}

export class HttpError extends Error {
  constructor(
    public readonly status: number | undefined,
    public readonly body: unknown,
    public readonly kind: "response" | "network" | "timeout" | "cancelled",
  ) {
    super("HTTP request failed");
  }
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || undefined,
  timeout: 15_000,
});

export function configureHttpCredentials(credentials: {
  getToken: () => string | null;
  onUnauthorized: () => void;
}) {
  const request = http.interceptors.request.use((config) => {
    if (config.authenticated) {
      const token = credentials.getToken();
      if (token) config.headers.set("Authorization", `Bearer ${token}`);
      else {
        credentials.onUnauthorized();
        throw new HttpError(401, undefined, "response");
      }
    }
    return config;
  });
  const response = http.interceptors.response.use(undefined, (error: unknown) => {
    if (!axios.isAxiosError<unknown>(error)) return Promise.reject(error);
    if (
      error.response?.status === 401 &&
      error.config?.authenticated &&
      error.config.headers.get("Authorization") === `Bearer ${credentials.getToken()}`
    ) {
      credentials.onUnauthorized();
    }
    return Promise.reject(new HttpError(
      error.response?.status,
      error.response?.data,
      axios.isCancel(error) ? "cancelled" : error.response ? "response"
        : error.code === "ECONNABORTED" ? "timeout" : "network",
    ));
  });
  return () => {
    http.interceptors.request.eject(request);
    http.interceptors.response.eject(response);
  };
}
