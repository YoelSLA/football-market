# Ejecutar Prometheus y Grafana localmente

Esta guía describe cómo levantar el backend y el stack de monitoreo para la demostración.
El agente no ejecuta tests, builds ni contenedores; las comprobaciones de esta guía son para
la persona usuaria.

## Requisitos previos

- Docker Desktop (Docker Engine y Docker Compose v2) iniciado.
- Proyecto abierto en IntelliJ IDEA.
- Backend de Spring Boot iniciado y disponible en el puerto `8080`.
- Endpoint de métricas de SPEC-032 disponible en `http://localhost:8080/actuator/prometheus`.

## 1. Iniciar el backend

Inicia la aplicación Spring Boot desde IntelliJ IDEA y espera a que indique que está escuchando
en el puerto `8080`. Prometheus consultará ese backend desde Docker mediante
`host.docker.internal:8080`.

## 2. Configurar credenciales locales de Grafana

Abre la pestaña **Terminal** de IntelliJ y confirma que estás en la raíz del repositorio
(`football-market`). Abre el archivo local de credenciales:

```powershell
notepad .\monitoring\.env
```

Escribe estas variables y reemplaza el valor de ejemplo por una contraseña segura propia:

```dotenv
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=REEMPLAZA_POR_UNA_CONTRASENA_SEGURA
```

Guarda el archivo. `monitoring/.env` está ignorado por Git; puedes comprobarlo desde la raíz:

```powershell
git check-ignore -v monitoring/.env
```

No publiques ni compartas este archivo. Si ya existe, conserva su contenido y añade o actualiza
solo estas dos variables.

## 3. Levantar Prometheus y Grafana

Desde la misma terminal y la raíz del repositorio, ejecuta:

```powershell
docker compose --env-file monitoring/.env -f monitoring/compose.yml up -d
docker compose --env-file monitoring/.env -f monitoring/compose.yml ps
```

`up -d` crea e inicia los servicios en segundo plano. `ps` muestra su estado. Deben aparecer
Prometheus y Grafana en ejecución; Grafana se publica solo en `127.0.0.1:3000` y Prometheus no
publica su puerto en el host.

## 4. Abrir Grafana

En el navegador del mismo equipo, abre <http://127.0.0.1:3000> e inicia sesión con el usuario y
la contraseña definidos en `monitoring/.env`. Abre el dashboard provisionado para consultar las
métricas disponibles de CPU, JVM y solicitudes HTTP.

Prometheus recolecta cada minuto. Para ver muestras en el dashboard, espera al menos dos ciclos
de recolección y selecciona un intervalo que los incluya. Algunas métricas —por ejemplo, las
pausas de GC— pueden no tener muestras hasta que ocurra el evento correspondiente.

## 5. Detener los servicios

Desde la raíz del repositorio:

```powershell
docker compose --env-file monitoring/.env -f monitoring/compose.yml down
```

Esto detiene y elimina los contenedores y la red del proyecto, pero conserva los volúmenes con
los datos locales. No añadas `-v` si quieres conservarlos.

## Comprobaciones manuales

- Confirmar que Grafana solicita autenticación y se abre en `127.0.0.1:3000`.
- Confirmar que Prometheus no tiene un puerto publicado en el host.
- Confirmar que el dashboard muestra las métricas cuando hay muestras y refleja la ausencia de
  datos cuando no las hay.
- `/actuator/prometheus` se reutiliza conforme a SPEC-032 y no requiere JWT. Esta feature no
  cambia la exposición ni la autenticación del backend.

Estas instrucciones no acreditan que el stack, la recolección ni el dashboard se hayan
ejecutado o aprobado.
