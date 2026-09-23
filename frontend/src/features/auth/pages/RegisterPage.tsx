import { Link } from "react-router-dom";
import { AuthField } from "../components/AuthField";
import { AuthLayout } from "../components/AuthLayout";
import styles from "../components/AuthForm.module.scss";
import { useRegister } from "../hooks/useRegister";

export function RegisterPage() {
  const { form, onSubmit, error, isPending } = useRegister();
  return (
    <AuthLayout title="Tu equipo empieza aquí." description="Crea tu cuenta y entra en Football Market.">
      <form className={styles.form} onSubmit={onSubmit} noValidate aria-busy={isPending}>
        <fieldset disabled={isPending}>
          <AuthField id="register-email" label="Tu email" type="email" autoComplete="email" required {...form.register("email")} error={form.formState.errors.email?.message} />
          <AuthField id="register-password" label="Tu contraseña · mínimo 8 caracteres" type="password" autoComplete="new-password" required {...form.register("password")} error={form.formState.errors.password?.message} />
          <AuthField id="register-confirmation" label="Confirma tu contraseña" type="password" autoComplete="new-password" required {...form.register("passwordConfirmation")} error={form.formState.errors.passwordConfirmation?.message} />
        </fieldset>
        <div className={styles.feedback}>{error && <p className={styles.error} role="alert">{error}</p>}</div>
        <button type="submit" disabled={isPending}>{isPending ? "Creando tu cuenta…" : "Crear cuenta"}</button>
        <p className={styles.switch}>¿Ya tienes cuenta? <Link to="/login" aria-disabled={isPending} onClick={(event) => { if (isPending) event.preventDefault(); }}>Iniciar sesión</Link></p>
      </form>
    </AuthLayout>
  );
}
