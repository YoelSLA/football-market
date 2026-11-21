# Ecosistema de Scripts de Automatización y Mercado

Este directorio contiene la suite de herramientas y utilitarios en Bash para administrar, simular y monitorear el mercado de cotizaciones, la oferta P2P, las transacciones secundarias y la actividad en tiempo real del **Dashboard** de Football Market.

---

## 📋 Resumen Rápido de Scripts

| Script | Propósito | Uso Típico | Impacto en el Sistema |
| :--- | :--- | :--- | :--- |
| **[`market-status.sh`](#1-market-statussh--monitor-de-estado-en-terminal)** | Tablero de control y diagnóstico en terminal. | `./scripts/market-status.sh` | Inspección general de salud, BD, cotizaciones y trades. |
| **[`recalculate-quotes.sh`](#2-recalculate-quotes-sh--recálculo-atómico-de-cotizaciones)** | Recalcula cotizaciones con climas bursátiles aleatorios. | `./scripts/recalculate-quotes.sh` | Mueve precios oficiales, alimenta Gainers, Losers y Ticker. |
| **[`run-quote-scheduler.sh`](#3-run-quote-scheduler-sh--scheduler-de-4-recálculos-obligatorios)** | Garantiza 4 recálculos diarios con *catch-up* tras suspensión. | `./scripts/run-quote-scheduler.sh &` o `--once` | Automatización diaria ininterrumpida. |
| **[`seed-p2p-market.sh`](#4-seed-p2p-market-sh--generador-de-ofertas-p2p)** | Siembra publicaciones P2P con cantidades aleatorias y trades. | `./scripts/seed-p2p-market.sh` | Llena el catálogo P2P y genera compras parciales. |
| **[`run-p2p-market-generator.sh`](#5-run-p2p-market-generator-sh--demonio-p2p-continuo)** | Bucle continuo para generar ofertas P2P periódicas. | `./scripts/run-p2p-market-generator.sh &` | Rondas continuas de publicaciones cada N segundos. |
| **[`simulate-trades.sh`](#6-simulate-trades-sh--simulador-de-compras-y-volumen)** | Compras P2P y oficiales simuladas (usuarios estándar). | `./scripts/simulate-trades.sh` | Alimenta el Live Activity Feed y el Volumen 24h. |
| **[`seed-user-portfolio.sh`](#7-seed-user-portfolio-sh--fondeo-de-portfolio-de-usuario)** | Acredita saldo y tokens diversificados a un usuario. | `./scripts/seed-user-portfolio.sh yoel@gmail.com` | Llena el Donut Chart, Sorare Cards y Patrimonio. |
| **[`setup-demo.sh`](#8-setup-demo-sh--preparación-one-click-para-demo)** | Master script para dejar el entorno 100% listo para demo. | `./scripts/setup-demo.sh` | Ejecuta la secuencia completa de inicialización y siembra. |
| **[`clean-demo-data.sh`](#9-clean-demo-data-sh--limpieza-y-mantenimiento)** | Cancela ofertas vencidas y controla el tamaño de logs. | `./scripts/clean-demo-data.sh` | Mantenimiento higiénico de la base de datos y logs. |

---

## 1. `market-status.sh` — Monitor de Estado en Terminal

Proporciona un reporte integral del estado del sistema en un solo comando:
* Estado de **PostgreSQL**, **Backend Spring Boot** (8080) y **Frontend Vite** (5173).
* Resumen de la última corrida de recálculo (ID, fecha, hora, estrategia, factor, cap).
* Avance de **recálculos del día** (ej. `4 de 4 obligatorios completados`).
* Métrica de publicaciones P2P: vigentes, vendidas y canceladas/expiradas.
* Transacciones y **Volumen negociado en las últimas 24 horas**.
* **Top 3 Ganadores** y **Top 3 Perdedores** de hoy con sus variaciones porcentuales.
* Tareas de automatización activas en `crontab` y procesos en segundo plano.

```bash
# Ejecución simple:
./scripts/market-status.sh
```

---

## 2. `recalculate-quotes.sh` — Recálculo Atómico de Cotizaciones

Dispara una corrida de recálculo sobre el backend vía `POST /api/quotes/recalculate` autenticado como operador técnico.

* **Climas Bursátiles Aleatorios:** Selecciona de forma orgánica uno de 3 perfiles para simular mercados vivos:
  * **Calmo (30% prob.):** Baja volatilidad (`factor ~0.12`, `cap ~0.08`).
  * **Moderado (45% prob.):** Volatilidad estándar (`factor ~0.22`, `cap ~0.15`).
  * **Volátil / Rally (25% prob.):** Alta volatilidad (`factor ~0.38`, `cap ~0.24`).
* **Estrategia dinámica:** Alterna entre `POSITIONAL_PERFORMANCE` (85%) y `GENERAL_ATTRIBUTES` (15%).
* **Agnóstico al locale:** Formatea números decimales con punto (`0.22`), garantizando compatibilidad con locales en español.

```bash
# Uso estándar con clima aleatorio:
./scripts/recalculate-quotes.sh

# Sobrescribir con parámetros manuales específicos:
QUOTE_ADJUSTMENT_FACTOR=0.30 QUOTE_VARIATION_CAP=0.20 ./scripts/recalculate-quotes.sh

# Forzar una estrategia específica:
QUOTE_STRATEGY=GENERAL_ATTRIBUTES ./scripts/recalculate-quotes.sh
```

---

## 3. `run-quote-scheduler.sh` — Scheduler de 4 Recálculos Obligatorios

Orquesta los **4 recálculos obligatorios diarios** (cada 6 horas: franjas 00:00, 06:00, 12:00 y 18:00).

* **Garantía de Catch-up (Recuperación tras suspensión o apagado):**
  Si la computadora estuvo suspendida o el backend apagado durante el inicio de una franja, el script consulta las ejecuciones reales registradas hoy (`GET /api/quotes/executions?date=YYYY-MM-DD`). Si faltan corridas para la hora actual, **las ejecuta en cadena de inmediato** con una pausa de 5 segundos entre ellas para que los precios escalen ordenadamente.
* **Tolerancia a caídas del backend:** Si el backend no responde, espera en intervalos sin fallar ni romper el ciclo.
* **Modo Demonio vs Modo Cron:**

```bash
# Modo Demonio (vigilancia continua en segundo plano):
./scripts/run-quote-scheduler.sh &

# Modo Cron / Verificación puntual (ejecuta solo si hay pendientes y sale):
./scripts/run-quote-scheduler.sh --once

# Modo rápido para demo en vivo (recalcula cada 30 segundos):
QUOTE_INTERVAL_SECONDS=30 ./scripts/run-quote-scheduler.sh
```

---

## 4. `seed-p2p-market.sh` — Generador de Ofertas P2P

Crea ofertas de venta en el mercado secundario P2P utilizando un grupo de vendedores demo.

* **Cantidades aleatorias de tokens:** En lugar de crear siempre 1 token fijo, sortea cantidades realistas (por defecto entre 1 y 5 tokens por publicación, con un 60% de publicaciones pequeñas de 1-2 tokens y 40% de 3-5 tokens).
* **Protección de saldo:** Calcula `MAX_INITIAL_CREDITS_CENTS / official_price` para que ningún vendedor exceda su crédito disponible.
* **Liquidación parcial automática:** Un comprador demo adquiere automáticamente el 40% de las publicaciones creadas, pasando ofertas a estado `SOLD` y generando volumen inmediato en `market_transactions`.

```bash
# Uso estándar:
./scripts/seed-p2p-market.sh

# Personalizar cantidad de vendedores y rango de tokens:
P2P_SELLER_COUNT=16 P2P_MIN_TOKENS=2 P2P_MAX_TOKENS=8 ./scripts/seed-p2p-market.sh
```

---

## 5. `run-p2p-market-generator.sh` — Demonio P2P Continuo

Mantiene un bucle en segundo plano que ejecuta periódicamente `seed-p2p-market.sh` y vuelca la salida en `scripts/p2p-generator.log`.

```bash
# Modo normal (ejecuta cada 3 horas):
./scripts/run-p2p-market-generator.sh &

# Modo ráfaga para demo (cada 15 segundos):
P2P_INTERVAL_SECONDS=15 ./scripts/run-p2p-market-generator.sh
```

---

## 6. `simulate-trades.sh` — Simulador de Compras y Volumen

Simula actividad de trading real en el mercado secundario y en el mercado oficial.

* **Cumplimiento de reglas de negocio:** **El superusuario no puede comprar**. El script utiliza exclusivamente compradores regulares registrados (`trader.buyerX@example.test`) con saldo de créditos asignado.
* **Impacto en el Dashboard:**
  * Alimenta el **Live Activity Feed** (compras recientes en vivo).
  * Dispara el contador de **Volumen 24h** (`market_transactions`).
  * Rota ofertas P2P de `ACTIVE` a `SOLD`.

```bash
# Uso estándar (4 compras P2P y 2 compras oficiales):
./scripts/simulate-trades.sh

# Rueda de alto volumen:
P2P_TRADES_COUNT=10 OFFICIAL_TRADES_COUNT=5 ./scripts/simulate-trades.sh
```

---

## 7. `seed-user-portfolio.sh` — Fondeo de Portfolio de Usuario

Acredita créditos y una cartera diversificada de jugadores destacados a un usuario real para que su Dashboard y su vista de Portfolio se vean poblados.

* **Cobertura total de posiciones:** Asigna tokens de Delanteros (Lamine Yamal, Raphinha), Mediocampistas (Declan Rice, Joshua Kimmich), Defensores (Giorgio Cittadini) y Arqueros (David Raya).
* **Impacto visual:**
  * **Donut Chart (Portfolio Allocation):** Muestra sectores coloridos y balanceados por posición.
  * **Sorare Showcase Grid:** Presenta cartas interactivas de jugadores de élite.
  * **Financial Hero Cockpit:** Calcula patrimonio total y P&L en tiempo real.

```bash
# Fondear el usuario principal (por defecto yoel@gmail.com con ₵200.00):
./scripts/seed-user-portfolio.sh

# Fondear otro usuario con saldo personalizado (en centésimas):
./scripts/seed-user-portfolio.sh nicolas@gmail.com 35000
```

---

## 8. `setup-demo.sh` — Preparación "One-Click" para Demos

El orquestador maestro para preparar la aplicación desde cero antes de una presentación:
1. Comprueba y levanta PostgreSQL y el Backend si están apagados.
2. Inicializa el mercado si la base de datos está vacía.
3. Ejecuta un recálculo dinámico de precios.
4. Siembra publicaciones P2P variadas.
5. Ejecuta compras para encender el Live Feed y el volumen del día.
6. Fondea el portfolio de `yoel@gmail.com`.
7. Imprime el diagnóstico final con `market-status.sh`.

```bash
./scripts/setup-demo.sh
```

---

## 9. `clean-demo-data.sh` — Limpieza y Mantenimiento

* Cancela ordenadamente las publicaciones P2P que hayan superado su ventana de expiración de 24 horas (`status = 'CANCELLED'`).
* Verifica el tamaño de los archivos `.log` en la carpeta `scripts/`. Si superan los 5 MB, los rota automáticamente para evitar archivos excesivamente grandes.
* Opción `--reset-p2p` para cancelar todas las ofertas activas y comenzar una siembra limpia.

```bash
# Limpieza rutinaria:
./scripts/clean-demo-data.sh

# Reiniciar todas las ofertas P2P activas:
./scripts/clean-demo-data.sh --reset-p2p
```

---

## ⚙️ Configuración en Crontab (Automatización Desatendida)

Para que el mercado opere de forma autónoma sin intervención humana, la configuración recomendada en el `crontab` del sistema es:

```bash
# Editar crontab:
crontab -e

# Entradas recomendadas:
# 1. Siembra periódica de publicaciones P2P cada 3 horas
0 */3 * * * cd /home/yoelsla/Escritorio/football-market && ./scripts/seed-p2p-market.sh >> /home/yoelsla/Escritorio/football-market/scripts/p2p-generator.log 2>&1 # football-market-p2p

# 2. Verificación y recálculos obligatorios de cotizaciones cada 15 minutos (con catch-up automático)
*/15 * * * * cd /home/yoelsla/Escritorio/football-market && ./scripts/run-quote-scheduler.sh --once >> /home/yoelsla/Escritorio/football-market/scripts/quote-scheduler.log 2>&1 # football-market-quotes

# 3. Mantenimiento y cancelación de ofertas vencidas cada 6 horas
0 */6 * * * cd /home/yoelsla/Escritorio/football-market && ./scripts/clean-demo-data.sh >> /home/yoelsla/Escritorio/football-market/scripts/maintenance.log 2>&1 # football-market-clean
```
