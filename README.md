# Football Market

Football Market es una aplicación web de mercado de jugadores de fútbol en desarrollo. El objetivo es consultar jugadores y sus cotizaciones, operar con tokens y gestionar el portfolio de cada usuario. Actualmente están implementados el registro e inicio de sesión con JWT, el catálogo paginado de jugadores y su sincronización manual desde Football-Data.org. El frontend ofrece las pantallas de autenticación y consulta del catálogo; las operaciones de mercado y portfolio siguen en desarrollo.

El proyecto se desarrolla siguiendo **Spec-Driven Development (SDD)**: los artefactos de las funcionalidades están en [`specs/`](specs/).

## Requisitos

- Java 21 (el backend utiliza el Gradle Wrapper incluido).
- PostgreSQL accesible en `localhost:5432`.
- Node.js 24 o superior y npm (la versión usada por el proyecto figura en [`.nvmrc`](.nvmrc)).
- Una clave de API de [Football-Data.org](https://www.football-data.org/) para sincronizar jugadores.

## Puesta en marcha local

Ejecutar los siguientes pasos desde la raíz del repositorio. El perfil `dev` del backend utiliza la base `football_market_dev` con usuario `postgres` y contraseña `root`, tal como figura en [`application-dev.yml`](backend/src/main/resources/application-dev.yml). Asegurarse de que PostgreSQL esté iniciado y de que ese usuario tenga acceso antes de crear la base:

```bash
psql -h localhost -U postgres -d postgres -c 'CREATE DATABASE football_market_dev;'
```

Si la base ya existe, omitir ese comando. Flyway aplica las migraciones al arrancar el backend; no hay que crear las tablas a mano.

### 1. Backend

En una terminal, definir las variables de entorno y arrancar la API:

```bash
cd backend
export JWT_SECRET='una-clave-aleatoria-de-al-menos-32-bytes'
export FOOTBALL_DATA_API_KEY='tu-clave-de-football-data'
export FOOTBALL_DATA_COMPETITIONS='PL,BL1,PD,SA,FL1'
./gradlew bootRun
```

Reemplazar los valores de ejemplo antes de iniciar. `JWT_SECRET` debe tener al menos 32 bytes para HS256; mantener la clave real fuera del repositorio. Las cinco competiciones configuradas son Premier League (`PL`), Bundesliga (`BL1`), La Liga (`PD`), Serie A (`SA`) y Ligue 1 (`FL1`). El backend queda disponible en `http://localhost:8080` y la documentación interactiva de la API en `http://localhost:8080/swagger-ui/index.html`.

### 2. Frontend

En otra terminal, crear la configuración local del cliente e iniciar Vite:

```bash
cd frontend
printf 'VITE_API_URL=http://localhost:8080\n' > .env.local
npm ci
npm run dev
```

Abrir `http://localhost:5173`. El frontend necesita `VITE_API_URL` para dirigir sus peticiones al backend; en desarrollo, la API admite solicitudes desde ese origen. Si ya existe un archivo `.env.local`, editar allí `VITE_API_URL` en vez de sobrescribirlo. No incluir secretos en variables `VITE_`, ya que se exponen al navegador.

Después de registrarse e iniciar sesión, se puede consultar el catálogo. Para cargar jugadores, ejecutar la sincronización manual `POST /api/players/sync` con el token JWT obtenido al iniciar sesión (por ejemplo, desde Swagger UI). La consulta `GET /api/players` también requiere autenticación. La sincronización depende del acceso a Football-Data.org y de la clave configurada.

## Estructura y referencias

- [`backend/`](backend/): API REST con Spring Boot, PostgreSQL y Flyway.
- [`frontend/`](frontend/): aplicación React, TypeScript y Vite.
- [`postman/collections/`](postman/collections/): colección de peticiones HTTP.
- [`docs/`](docs/): arquitectura, tecnologías y convenciones del proyecto.

Las versiones y dependencias efectivas se consultan en [`backend/build.gradle.kts`](backend/build.gradle.kts), [`frontend/package.json`](frontend/package.json) y los respectivos archivos de configuración.
