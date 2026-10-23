import { QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { BrowserRouter } from "react-router-dom";
import { queryClient } from "@/app/query";
import { configureAuthQueryClient } from "@/infrastructure/storage";
import HttpCredentials from "./HttpCredentials";
import SessionLifecycle from "./SessionLifecycle";

configureAuthQueryClient(queryClient);

interface Props {
	children: ReactNode;
}

export default function AppProviders({ children }: Props) {
	return (
		<QueryClientProvider client={queryClient}>
			<BrowserRouter>
				<HttpCredentials>
					<SessionLifecycle>{children}</SessionLifecycle>
				</HttpCredentials>
			</BrowserRouter>
		</QueryClientProvider>
	);
}
