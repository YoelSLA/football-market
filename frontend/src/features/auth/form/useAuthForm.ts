import { zodResolver } from "@hookform/resolvers/zod";
import { useRef } from "react";
import { useForm } from "react-hook-form";
import type { LoginForm, RegisterForm } from "../models/AuthForm";
import { loginSchema, registerSchema } from "./authSchemas";

export function useLoginForm() {
  const form = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  });
  const submitting = useRef(false);
  return {
    ...form,
    submit: (action: (values: LoginForm) => Promise<void>) => form.handleSubmit(async (values) => {
      if (submitting.current) return;
      submitting.current = true;
      try { await action(values); } finally { submitting.current = false; }
    }),
  };
}

export function useRegisterForm() {
  const form = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: { email: "", password: "", passwordConfirmation: "" },
  });
  const submitting = useRef(false);
  return {
    ...form,
    submit: (action: (values: RegisterForm) => Promise<void>) => form.handleSubmit(async (values) => {
      if (submitting.current) return;
      submitting.current = true;
      try { await action(values); } finally { submitting.current = false; }
    }),
  };
}
