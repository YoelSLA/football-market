import { Link } from "react-router-dom";
import styles from "@/features/auth/AuthForm.module.scss";
import { AuthLayout } from "@/features/auth/components";
import { useLoginPage } from "@/features/auth/hooks";
import { Button, Field } from "@/shared/components";

export default function LoginPage() {
	const { form, onSubmit, error, registrationComplete, isPending } =
		useLoginPage();
	return (
		<AuthLayout
			title="Bienvenido de nuevo."
			description="Inicia sesión y da el siguiente paso con tu equipo."
		>
			<form
				className={styles.authForm}
				onSubmit={onSubmit}
				noValidate
				aria-busy={isPending}
			>
				<fieldset className={styles.authForm__fields} disabled={isPending}>
					<Field
						id="login-email"
						label="Tu email"
						type="email"
						autoComplete="email"
						required
						{...form.register("email")}
						error={form.formState.errors.email?.message}
					/>
					<Field
						id="login-password"
						label="Tu contraseña"
						type="password"
						autoComplete="current-password"
						required
						{...form.register("password")}
						error={form.formState.errors.password?.message}
					/>
				</fieldset>
				<div className={styles.authForm__feedback}>
					{error ? (
						<p className={styles.authForm__error} role="alert">
							{error}
						</p>
					) : (
						registrationComplete && (
							<p className={styles.authForm__success} role="status">
								Cuenta creada correctamente. Ya puedes iniciar sesión.
							</p>
						)
					)}
				</div>
				<Button
					className={styles.authForm__submit}
					type="submit"
					disabled={isPending}
				>
					{isPending ? "Iniciando sesión…" : "Iniciar sesión"}
				</Button>
				<p className={styles.authForm__switch}>
					¿Aún no tienes cuenta?{" "}
					<Link
						className={styles.authForm__link}
						to="/register"
						aria-disabled={isPending}
						onClick={(event) => {
							if (isPending) event.preventDefault();
						}}
					>
						Crear una cuenta
					</Link>
				</p>
			</form>
		</AuthLayout>
	);
}
