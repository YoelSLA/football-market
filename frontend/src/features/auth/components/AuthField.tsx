import type { InputHTMLAttributes } from "react";
import styles from "./AuthForm.module.scss";

export function AuthField({ label, error, ...input }: InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  error?: string;
}) {
  return (
    <div className={styles.field}>
      <label htmlFor={input.id}>{label}</label>
      <input {...input} aria-invalid={Boolean(error)} aria-describedby={error ? `${input.id}-error` : undefined} />
      <span id={`${input.id}-error`} className={styles.fieldError}>{error}</span>
    </div>
  );
}
