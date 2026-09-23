import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useRegisterForm } from "../form/useAuthForm";
import { authService } from "../services/AuthService";

export function useRegister() {
  const form = useRegisterForm();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const mutation = useMutation({ mutationFn: authService.register, networkMode: "always", retry: false, gcTime: 0 });
  const onSubmit = form.submit(async (values) => {
    setError(null);
    try {
      await mutation.mutateAsync(values);
      form.reset();
      navigate("/login", { replace: true, state: { registrationComplete: true } });
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : "No pudimos crear la cuenta. Vuelve a intentarlo.");
    } finally { mutation.reset(); }
  });
  return { form, onSubmit, error, isPending: form.formState.isSubmitting };
}
