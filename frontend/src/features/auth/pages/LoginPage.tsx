import { Link } from "react-router-dom";
import { AuthField } from "../components/AuthField";
import { AuthLayout } from "../components/AuthLayout";
import styles from "../components/AuthForm.module.scss";
import { useLogin } from "../hooks/useLogin";

export function LoginPage() {
  const { form, onSubmit, error, registrationComplete, isPending } = useLogin();
  return (
    <AuthLayout title="Bienvenido de nuevo." description="Inicia sesión y da el siguiente paso con tu equipo.">
      <form className={styles.form} onSubmit={onSubmit} noValidate aria-busy={isPending}>
        <fieldset disabled={isPending}>
          <AuthField id="login-email" label="Tu email" type="email" autoComplete="email" required {...form.register("email")} error={form.formState.errors.email?.message} />
          <AuthField id="login-password" label="Tu contraseña" type="password" autoComplete="current-password" required {...form.register("password")} error={form.formState.errors.password?.message} />
        </fieldset>
        <div className={styles.feedback}>
          {error ? <p className={styles.error} role="alert">{error}</p>
            : registrationComplete && <p className={styles.success} role="status">Cuenta creada correctamente. Ya puedes iniciar sesión.</p>}
        </div>
        <button type="submit" disabled={isPending}>{isPending ? "Iniciando sesión…" : "Iniciar sesión"}</button>
        <p className={styles.switch}>¿Aún no tienes cuenta? <Link to="/register" aria-disabled={isPending} onClick={(event) => { if (isPending) event.preventDefault(); }}>Crear una cuenta</Link></p>
      </form>
    </AuthLayout>
  );
}
