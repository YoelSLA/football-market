import { formatCredits } from "@/features/market/utils";
import styles from "./MarketToastContent.module.scss";

interface BaseToastProps {
	title: string;
	badge?: string;
}

interface TradeSuccessToastProps extends BaseToastProps {
	variant: "sell" | "buy";
	playerName: string;
	quantity: number;
	totalAmount: number;
	unitPrice?: number;
	timestampText?: string;
}

interface CancelToastProps extends BaseToastProps {
	variant: "cancel";
	playerName: string;
	quantity: number;
}

interface SimpleToastProps extends BaseToastProps {
	variant: "info" | "error";
	message: string;
}

export type MarketToastProps =
	| TradeSuccessToastProps
	| CancelToastProps
	| SimpleToastProps;

export default function MarketToastContent(props: MarketToastProps) {
	switch (props.variant) {
		case "sell":
		case "buy":
			return (
				<div className={styles.toast}>
					<div className={styles.toast__header}>
						<span className={styles.toast__title}>{props.title}</span>
						{props.badge && (
							<span className={styles.toast__badge}>{props.badge}</span>
						)}
					</div>
					<p className={styles.toast__primary}>
						<strong>
							{props.quantity} {props.quantity === 1 ? "token" : "tokens"}
						</strong>{" "}
						de {props.playerName}
					</p>
					<div className={styles.toast__meta}>
						<span>
							Total: <strong>{formatCredits(props.totalAmount)}</strong>
						</span>
						{props.timestampText && (
							<>
								<span className={styles.toast__dot}>·</span>
								<span>{props.timestampText}</span>
							</>
						)}
					</div>
				</div>
			);

		case "cancel":
			return (
				<div className={styles.toast}>
					<div className={styles.toast__header}>
						<span className={styles.toast__title}>{props.title}</span>
						{props.badge && (
							<span className={styles.toast__badge}>{props.badge}</span>
						)}
					</div>
					<p className={styles.toast__primary}>
						<strong>
							{props.quantity} {props.quantity === 1 ? "token" : "tokens"}
						</strong>{" "}
						de {props.playerName}
					</p>
					<p className={styles.toast__subtext}>Tokens devueltos a tu cartera</p>
				</div>
			);

		case "info":
		case "error":
			return (
				<div className={styles.toast}>
					<div className={styles.toast__header}>
						<span
							className={
								props.variant === "error"
									? styles.toast__title_error
									: styles.toast__title
							}
						>
							{props.title}
						</span>
					</div>
					<p className={styles.toast__message}>{props.message}</p>
				</div>
			);
	}
}
