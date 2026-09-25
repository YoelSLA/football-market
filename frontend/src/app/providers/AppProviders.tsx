import { QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { BrowserRouter } from "react-router-dom";
import HttpCredentials from "@/app/providers/HttpCredentials";
import SessionLifecycle from "./SessionLifecycle";
import { configureAuthQueryClient } from "@/infrastructure/storage";
import { queryClient } from "../query";

configureAuthQueryClient(queryClient);

interface Props {
  children: ReactNode;
}

export function AppProviders({ children }: Props) {
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
