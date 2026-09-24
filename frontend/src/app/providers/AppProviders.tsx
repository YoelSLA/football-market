import { QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { BrowserRouter } from "react-router-dom";
import HttpCredentials from "@/app/providers/HttpCredentials";
import { AuthProvider } from "@/features/auth";
import { queryClient } from "../query";

interface Props {
  children: ReactNode;
}

export function AppProviders({ children }: Props) {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <HttpCredentials>{children}</HttpCredentials>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}