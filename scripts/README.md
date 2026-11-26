# Ecosistema de Scripts de Automatización y Mercado

Este directorio contiene la suite de herramientas y utilitarios en Bash para administrar, simular y monitorear el mercado de cotizaciones, la oferta P2P, las transacciones secundarias y la actividad en tiempo real del **Dashboard** de Football Market.

---

## 📋 Resumen Rápido de Scripts

| Script | Propósito | Uso Típico | Impacto en el Sistema |
| :--- | :--- | :--- | :--- |
| **[`market-engine.sh`](#1-market-enginesh--motor-autónomo-de-mercado-continuo-10-transacciones--3-min)** | Motor integral: **10 transacciones confirmadas cada 3 min** (oficial + P2P). | `./scripts/market-engine.sh start` | Mantiene vivo el 100% del mercado, cotizaciones, Dashboard y órdenes. |
| **[`market-status.sh`](#2-market-statussh--monitor-de-estado-en-terminal)** | Tablero de control y diagnóstico en terminal. | `./scripts/market-status.sh` | Inspección general de salud, BD, cotizaciones y trades. |
| **[`recalculate-quotes.sh`](#3-recalculate-quotes-sh--recálculo-atómico-de-cotizaciones)** | Recalcula cotizaciones con climas bursátiles aleatorios. | `./scripts/recalculate-quotes.sh` | Mueve precios oficiales, alimenta Gainers, Losers y Ticker. |
| **[`run-quote-scheduler.sh`](#4-run-quote-scheduler-sh--scheduler-de-4-recálculos-obligatorios)** | Garantiza 4 recálculos diarios con *catch-up* tras suspensión. | `./scripts/run-quote-scheduler.sh &` o `--once` | Automatización diaria ininterrumpida. |
| **[`seed-p2p-market.sh`](#5-seed-p2p-market-sh--generador-de-ofertas-p2p)** | Siembra publicaciones P2P con cantidades aleatorias y trades. | `./scripts/seed-p2p-market.sh` | Llena el catálogo P2P y genera compras parciales. |
| **[`run-p2p-market-generator.sh`](#6-run-p2p-market-generator-sh--demonio-p2p-continuo)** | Bucle continuo para generar ofertas P2P periódicas. | `./scripts/run-p2p-market-generator.sh &` | Rondas continuas de publicaciones cada N segundos. |
| **[`simulate-trades.sh`](#7-simulate-trades-sh--simulador-de-compras-y-volumen)** | Compras P2P y oficiales simuladas (usuarios estándar). | `./scripts/simulate-trades.sh` | Alimenta el Live Activity Feed y el Volumen 24h. |
| **[`auto-trader.sh`](#8-auto-tradersh--motor-de-trading-automático-yoel-nicolas-y-mercado)** | Trading continuo entre usuarios reales (Yoel y Nicolas) y recálculos. | `./scripts/auto-trader.sh` o `--loop` | Compras/ventas cruzadas reales, rotación de carteras y cotizaciones. |
| **[`seed-user-portfolio.sh`](#9-seed-user-portfolio-sh--fondeo-de-portfolio-de-usuario)** | Acredita saldo y tokens diversificados a un usuario. | `./scripts/seed-user-portfolio.sh yoel@gmail.com` | Llena el Donut Chart, Sorare Cards y Patrimonio. |
| **[`setup-demo.sh`](#10-setup-demo-sh--preparación-one-click-para-demo)** | Master script para dejar el entorno 100% listo para demo. | `./scripts/setup-demo.sh` | Ejecuta la secuencia completa de inicialización y siembra. |
| **[`clean-demo-data.sh`](#11-clean-demo-data-sh--limpieza-y-mantenimiento)** | Cancela ofertas vencidas y controla el tamaño de logs. | `./scripts/clean-demo-data.sh` | Mantenimiento higiénico de la base de datos y logs. |

---

## 1. `market-engine.sh` — Motor Autónomo de Mercado Continuo (10 transacciones / 3 min)

El motor principal y más completo del ecosistema. Coordina y ejecuta en cada ciclo un flujo económico cerrado garantizando **exactamente 10 transacciones financieras confirmadas** cada 3 minutos (200 transacciones/hora):

* **Distribución dinámica y balanceada de transacciones por ciclo (Variación aleatoria 40%–60%):**
  * **Equilibrio de Sentimiento de Mercado (Compras vs. Ventas):** En cada ciclo, el motor sortea un ratio aleatorio entre el **40% y el 60% de ventas** (o configurable mediante `--sells-ratio`), permitiendo que el tablero de tendencias (`/market/trends` de SPEC-029) oscile de manera realista y orgánica entre estados *BULLISH (Alcista)*, *NEUTRAL* y *BEARISH (Bajista)*.
  * **Compras en Mercado Oficial (Emisión Primaria):** Compradores (Bot 1 a Bot 8, Yoel y Nicolas) adquieren entre **1 y 10 tokens** al Superusuario, con diversificación táctica por posición y compras concentradas en futbolistas estrella ("Hot Assets").
  * **Liquidación Inmediata Oficial / Buyback a la Casa (SPEC-026 / PR #111):** Múltiples vendedores liquidan tokens directamente al Superusuario vía `POST /api/orders/sell-official` para obtener liquidez inmediata en créditos, aplicándose el spread oficial del 10% y computando como **ventas oficiales auditadas** para el sentimiento colectivo.
  * **Compras en Mercado P2P:** Compradores adquieren y liquidan publicaciones activas de la comunidad (`token_listings`), acreditando fondos a los vendedores y pasando las órdenes a `SOLD`.
  * **Mecanismo de Cuota y Balance Garantizado:** Si las transacciones iniciales no completaran la cuota (por defecto 10), el motor compensa dinámicamente: prioriza liquidaciones oficiales a la casa si faltan ventas para alcanzar la meta sorteada (40%–60%), o compras oficiales si la meta de compras aún no fue alcanzada, garantizando **estrictamente las 10 transacciones confirmadas**.
* **Inyección Continua y Robusta de Oferta P2P (15–25 órdenes activas garantizadas):**
  * Para asegurar que el catálogo P2P nunca se quede vacío, en cada ciclo todos los bots disponibles (Bot #1 a Bot #8) junto a Yoel y Nicolas publican de **8 a 16 órdenes de venta simultáneas** con cantidades realistas (1 a 5 tokens) y precios flexibles (85% a 115% de cotización con etiquetas *Best Deal* o *Sobreprecio*).
  * Si un bot carece de variedad en su cartera, el motor le abastece tokens oficiales de diferentes posiciones y clubes automáticamente antes de publicar.
  * El motor regula las compras cruzadas P2P a un máximo de 2 a 4 adquisiciones por ciclo, garantizando que el libro de órdenes conserve una **profundidad viva de entre 15 y 25+ órdenes activas** en todo momento para que los usuarios humanos puedan navegar y comprar en la web.
* **Comando de Inyección Directa (`--inject-p2p`):** Permite sembrar un lote instantáneo de N publicaciones P2P (por defecto 16) con un solo comando.
* **Publicaciones P2P con Precios Flexibles (SPEC-026):** Antes de las compras, los vendedores ofrecen paquetes de entre 1 y 5 tokens asignando un precio unitario flexible (`unitPrice`) entre el 85% y 115% de la cotización actual, creando ofertas con descuento o con sobreprecio.
* **Cancelaciones Esporádicas (SPEC-030):** Cancela con un 15% de probabilidad una orden activa para poblar el estado `CANCELLED` en la pantalla de *Mis órdenes*.
* **Recálculo Bursátil Controlado (Máximo 4 al día):** Se integra con `run-quote-scheduler.sh` para respetar estrictamente las 4 ventanas diarias obligatorias (00:00, 06:00, 12:00 y 18:00). No recalcula en cada ciclo de 3 minutos para no inflar artificialmente las ejecuciones diarias. Solo cuando se cruza a una nueva franja de 6 horas se ejecuta un recálculo bursátil con climas aleatorios (*Calmo, Moderado o Volátil*). También cuenta con la bandera `--force-recalc` por si se desea forzar el recálculo en cada ciclo durante una demo. Al ejecutarse tras las publicaciones P2P, aquellas ofertas cuyos jugadores suben de precio se transforman orgánicamente en **ofertas con descuento**, alimentando el widget *P2P Best Deals*.
* **Auto-liquidez dinámica:** Si el saldo de cualquier participante cae por debajo de 20 créditos (2.000 centésimas), se le inyectan créditos automáticamente para que el ciclo corra de forma indefinida sin bloquearse jamás.
* **Auto-rotación de logs:** Si `market-engine.log` supera los 10 MB, se archiva automáticamente en `.bak`.

### Comandos de servicio (segundo plano):
```bash
# Iniciar en segundo plano (demonio con 10 transacciones cada 3 minutos):
./scripts/market-engine.sh start

# Ver estado, PID activo, cuota y últimas transacciones en vivo:
./scripts/market-engine.sh status

# Detener el demonio ordenadamente:
./scripts/market-engine.sh stop

# Reiniciar el demonio:
./scripts/market-engine.sh restart
```

### Modos interactivos en consola:
```bash
# Bucle interactivo con salida coloreada (10 transacciones cada 180s):
./scripts/market-engine.sh

# Modo Demo Rápido (10 transacciones cada 20 segundos para presentaciones):
./scripts/market-engine.sh --fast

# Modo Ráfaga (dispara 3 ciclos inmediatos de 10 transacciones y luego continúa normal):
./scripts/market-engine.sh --burst

# Una sola corrida de 10 transacciones y salir (para cron o pruebas puntuales):
./scripts/market-engine.sh --once

# Inyectar inmediatamente un lote de ofertas P2P (por ejemplo 16 o 25):
./scripts/market-engine.sh --inject-p2p 20

# Personalizar intervalo o cantidad de transacciones:
./scripts/market-engine.sh --interval 60 -t 15
```

---

## 2. `market-status.sh` — Monitor de Estado en Terminal

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

## 3. `recalculate-quotes.sh` — Recálculo Atómico de Cotizaciones

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

## 4. `run-quote-scheduler.sh` — Scheduler de 4 Recálculos Obligatorios

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

## 5. `seed-p2p-market.sh` — Generador de Ofertas P2P

Crea ofertas de venta en el mercado secundario P2P utilizando un grupo de vendedores demo.

* **Cantidades aleatorias de tokens:** Sortea cantidades realistas (por defecto entre 1 y 5 tokens por publicación, con un 60% de publicaciones pequeñas de 1-2 tokens y 40% de 3-5 tokens).
* **Protección de saldo:** Calcula `MAX_INITIAL_CREDITS_CENTS / official_price` para que ningún vendedor exceda su crédito disponible.
* **Liquidación parcial automática:** Un comprador demo adquiere automáticamente el 40% de las publicaciones creadas, pasando ofertas a estado `SOLD` y generando volumen inmediato en `market_transactions`.

```bash
# Uso estándar:
./scripts/seed-p2p-market.sh

# Personalizar cantidad de vendedores y rango de tokens:
P2P_SELLER_COUNT=16 P2P_MIN_TOKENS=2 P2P_MAX_TOKENS=8 ./scripts/seed-p2p-market.sh
```

---

## 6. `run-p2p-market-generator.sh` — Demonio P2P Continuo

Mantiene un bucle en segundo plano que ejecuta periódicamente `seed-p2p-market.sh` y vuelca la salida en `scripts/p2p-generator.log`.

```bash
# Modo normal (ejecuta cada 3 horas):
./scripts/run-p2p-market-generator.sh &

# Modo ráfaga para demo (cada 15 segundos):
P2P_INTERVAL_SECONDS=15 ./scripts/run-p2p-market-generator.sh
```

---

## 7. `simulate-trades.sh` — Simulador de Compras y Volumen

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

## 8. `auto-trader.sh` — Motor de Trading Automático (Yoel, Nicolas y Mercado)

Motor de operaciones continuas diseñado para mantener el mercado dinámico involucrando las cuentas de `yoel@gmail.com` y `nicolas@gmail.com`:
* **Ventas P2P inteligentes:** Yoel y Nicolas ofrecen tokens disponibles de su tenencia en P2P.
* **Compras cruzadas:** Nicolas compra publicaciones de Yoel y Yoel compra publicaciones de Nicolas.
* **Absorción de liquidez:** Compradores demo absorben ofertas remanentes para que los usuarios cobren sus créditos.
* **Compras oficiales y recálculo:** Adquieren nuevos tokens oficiales y disparan recálculo de precios.

```bash
# Ejecutar un ciclo completo de trading y recálculo:
./scripts/auto-trader.sh

# Modo bucle continuo en segundo plano (cada 60 segundos por defecto):
./scripts/auto-trader.sh --loop &

# Modo bucle con intervalo personalizado (ej. cada 30 segundos):
./scripts/auto-trader.sh --loop --interval 30
```

---

## 9. `seed-user-portfolio.sh` — Fondeo de Portfolio de Usuario

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

## 10. `setup-demo.sh` — Preparación "One-Click" para Demos

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

## 11. `clean-demo-data.sh` — Limpieza y Mantenimiento

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

# Opción A: Motor integral continuo (10 transacciones cada 3 minutos en segundo plano como demonio):
# (Basta con ejecutar una vez en la terminal: ./scripts/market-engine.sh start)

# Opción B: Tareas periódicas en crontab:
# 1. Ejecutar 1 ciclo de 10 transacciones de mercado cada 3 minutos
*/3 * * * * cd /home/yoelsla/Escritorio/football-market && ./scripts/market-engine.sh --once >> /home/yoelsla/Escritorio/football-market/scripts/market-engine.log 2>&1 # football-market-engine

# 2. Verificación y recálculos obligatorios de cotizaciones cada 15 minutos (con catch-up automático)
*/15 * * * * cd /home/yoelsla/Escritorio/football-market && ./scripts/run-quote-scheduler.sh --once >> /home/yoelsla/Escritorio/football-market/scripts/quote-scheduler.log 2>&1 # football-market-quotes

# 3. Mantenimiento y cancelación de ofertas vencidas cada 6 horas
0 */6 * * * cd /home/yoelsla/Escritorio/football-market && ./scripts/clean-demo-data.sh >> /home/yoelsla/Escritorio/football-market/scripts/maintenance.log 2>&1 # football-market-clean
```
