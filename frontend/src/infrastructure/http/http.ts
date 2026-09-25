import axios from "axios";

export const http = axios.create({
	baseURL: import.meta.env.VITE_API_URL || undefined,
	timeout: 15_000,
});
