import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useLoginForm } from "../form/useAuthForm";
import { authService } from "../services/AuthService";
import { useAuth } from "./useAuth";

export function useLogin() {
  const form = useLoginForm();
  const navigate = useNavigate();
  const location = useLocation();
  const { startSession } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const mutation = useMutation({ mutationFn: authService.login, networkMode: "always", retry: false, gcTime: 0 });
  const state: unknown = location.state;
  const registrationComplete = typeof state === "object" && state !== null
    && "registrationComplete" in state && state.registrationComplete === true;
  const onSubmit = form.submit(async (values) => {
    setError(null);
    try {
      const session = await mutation.mutateAsync(values);
      startSession(session);
      form.reset();
      navigate("/home", { replace: true });
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : "No pudimos iniciar sesión. Vuelve a intentarlo.");
    } finally { mutation.reset(); }
  });
  return { form, onSubmit, error, registrationComplete, isPending: form.formState.isSubmitting };
}
