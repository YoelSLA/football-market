import axios from "axios";
import type { ApiError } from "./types";

export function isApiError(error: unknown): error is ApiError {
	if (typeof error !== "object" || error === null) return false;

	return (
		"timestamp" in error &&
		typeof error.timestamp === "string" &&
		"status" in error &&
		typeof error.status === "number" &&
		"error" in error &&
		typeof error.error === "string" &&
		"code" in error &&
		typeof error.code === "string" &&
		"message" in error &&
		typeof error.message === "string" &&
		"path" in error &&
		typeof error.path === "string"
	);
}

export function getApiError(error: unknown): ApiError | null {
	if (!axios.isAxiosError<unknown>(error)) return null;

	const body: unknown = error.response?.data;

	return isApiError(body) ? body : null;
}

export function getErrorCode(error: unknown): string | null {
	return getApiError(error)?.code ?? null;
}

export function getErrorMessage(error: unknown, fallback: string): string {
	return getApiError(error)?.message || fallback;
}
