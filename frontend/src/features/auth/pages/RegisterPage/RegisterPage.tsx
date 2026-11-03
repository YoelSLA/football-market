import { Link } from "react-router-dom";
import styles from "@/features/auth/AuthForm.module.scss";
import { AuthLayout } from "@/features/auth/components";
import { useRegisterPage } from "@/features/auth/hooks";
import { Button, Field } from "@/shared/components";

export default function RegisterPage() {
	const { form, onSubmit, error, isPending } = useRegisterPage();
	return (
		<AuthLayout
			title="Tu equipo empieza aquí."
			description="Crea tu cuenta y entra en Football Market."
		>
			<form
				className={styles.authForm}
				onSubmit={onSubmit}
				noValidate
				aria-busy={isPending}
			>
				<fieldset className={styles.authForm__fields} disabled={isPending}>
					<Field
						id="register-username"
						label="Tu nombre de usuario"
						autoComplete="username"
						required
						{...form.register("username")}
						error={form.formState.errors.username?.message}
					/>
					<Field
						id="register-email"
						label="Tu email"
						type="email"
						autoComplete="email"
						required
						{...form.register("email")}
						error={form.formState.errors.email?.message}
					/>
					<Field
						id="register-password"
						label="Tu contraseña · mínimo 8 caracteres"
						type="password"
						autoComplete="new-password"
						required
						{...form.register("password")}
						error={form.formState.errors.password?.message}
					/>
					<Field
						id="register-confirmation"
						label="Confirma tu contraseña"
						type="password"
						autoComplete="new-password"
						required
						{...form.register("passwordConfirmation")}
						error={form.formState.errors.passwordConfirmation?.message}
					/>
				</fieldset>
				<div className={styles.authForm__feedback}>
					{error && (
						<p className={styles.authForm__error} role="alert">
							{error}
						</p>
					)}
				</div>
				<Button
					className={styles.authForm__submit}
					type="submit"
					disabled={isPending}
				>
					{isPending ? "Creando tu cuenta…" : "Crear cuenta"}
				</Button>
				<p className={styles.authForm__switch}>
					¿Ya tienes cuenta?{" "}
					<Link
						className={styles.authForm__link}
						to="/login"
						aria-disabled={isPending}
						onClick={(event) => {
							if (isPending) event.preventDefault();
						}}
					>
						Iniciar sesión
					</Link>
				</p>
			</form>
		</AuthLayout>
	);
}
