---
description: "Lista de tareas para la autenticación de usuarios en el frontend"
---

# Tasks: Autenticación de usuarios en el frontend

**Input**: Documentos de diseño de `/specs/004-frontend-user-auth/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/auth.http.md`

**Tests**: No se crean ni ejecutan tests de frontend según la constitución y el plan.

## Phase 1: Setup (infraestructura compartida)

**Purpose**: Completar la base técnica necesaria para la feature de autenticación.

- [ ] T001 Revisar la configuración existente de React, TypeScript, Sass, React Router, TanStack Query, React Hook Form, Zod y Axios en `frontend/package.json` y `frontend/src/main.tsx`
- [ ] T002 Configurar el cliente HTTP compartido y sus tipos de error en `frontend/src/shared/http/` sin mover URLs de autenticación fuera del service
- [ ] T003 [P] Configurar el adaptador de almacenamiento de `localStorage` en `frontend/src/infrastructure/storage/`
- [ ] T004 [P] Configurar el cliente de TanStack Query en `frontend/src/app/query/queryClient.ts`

## Phase 2: Foundational (prerrequisitos bloqueantes)

**Purpose**: Crear contratos, modelos y estado común de sesión antes de las stories.

- [ ] T005 Crear los DTOs y tipos públicos de autenticación en `frontend/src/features/auth/types/`
- [ ] T006 [P] Crear los modelos de `CurrentUser`, `AuthSession` y formularios en `frontend/src/features/auth/models/`
- [ ] T007 [P] Implementar los mappers entre DTOs y modelos en `frontend/src/features/auth/mappers/`
- [ ] T008 Implementar schemas Zod y tipos de formulario de login y registro en `frontend/src/features/auth/form/`
- [ ] T009 Implementar `AuthService` con `POST /api/auth/login`, `POST /api/auth/register` y `GET /api/auth/me` en `frontend/src/features/auth/services/AuthService.ts`
- [ ] T010 Implementar la persistencia e invalidación del JWT mediante el adaptador de almacenamiento en `frontend/src/features/auth/services/AuthSessionStorage.ts`
- [ ] T011 Crear el contexto y hook de autenticación en `frontend/src/features/auth/hooks/useAuth.tsx`, con `unknown` durante la inicialización, `checking` mientras se consulta `/me`, `authenticated` tras `200` y `anonymous` sin sesión restaurable o tras `401`
- [ ] T012 Configurar el interceptor funcional para enviar `Authorization: Bearer <token>` e invalidar la sesión ante `401` en `frontend/src/shared/http/`
- [ ] T013 Exportar la API pública de la feature en `frontend/src/features/auth/index.ts`

**Checkpoint**: La infraestructura de sesión, contratos y estado de autenticación está disponible; las stories pueden comenzar.

## Phase 3: User Story 1 - Crear una cuenta (Priority: P1) MVP

**Goal**: Permitir registrar una cuenta válida, mostrar errores y regresar a login sin autenticar automáticamente.

**Independent Test**: Abrir `/register`, enviar datos válidos y comprobar llegada a `/login` con confirmación; repetir con email duplicado y datos inválidos.

### Implementation for User Story 1

- [ ] T014 [P] [US1] Implementar `RegisterPage` con formulario, enlaces a login, estados de carga y mensajes en `frontend/src/features/auth/pages/RegisterPage.tsx`
- [ ] T015 [US1] Implementar la mutación de registro mediante TanStack Query en `frontend/src/features/auth/hooks/useRegister.ts`
- [ ] T016 [US1] Conectar la validación Zod, el bloqueo de envíos simultáneos y el payload sin confirmación en `frontend/src/features/auth/hooks/useRegister.ts`
- [ ] T017 [US1] Traducir respuestas `400`, `409` y fallos de comunicación a feedback recuperable con reintento manual en `frontend/src/features/auth/hooks/useRegister.ts`

**Checkpoint**: El registro funciona independientemente sin iniciar sesión ni proteger contenido privado.

## Phase 4: User Story 2 - Iniciar sesión y acceder al home (Priority: P1)

**Goal**: Autenticar credenciales válidas, persistir el token y mostrar el acceso confirmado en `/home`.

**Independent Test**: Abrir `/login`, iniciar sesión con una cuenta existente y verificar llegada a `/home`; repetir con credenciales incorrectas, campos inválidos y enlace a registro.

### Implementation for User Story 2

- [ ] T018 [P] [US2] Implementar `LoginPage` con formulario, enlace a registro, estados de carga y feedback en `frontend/src/features/auth/pages/LoginPage.tsx`
- [ ] T019 [US2] Implementar la mutación de login y la extracción del token en `frontend/src/features/auth/hooks/useLogin.ts`
- [ ] T020 [US2] Persistir el token, actualizar el estado autenticado y navegar a `/home` en `frontend/src/features/auth/hooks/useLogin.ts`
- [ ] T021 [US2] Traducir respuestas `400`, `401` y fallos de comunicación a feedback recuperable con reintento manual en `frontend/src/features/auth/hooks/useLogin.ts`
- [ ] T022 [P] [US2] Crear `HomePage` temporal con confirmación de acceso y espacio para cerrar sesión en `frontend/src/features/auth/pages/HomePage.tsx`

**Checkpoint**: El login conduce a un home autenticado y los errores de credenciales son visibles.

## Phase 5: User Story 3 - Conservar y proteger la sesión (Priority: P1)

**Goal**: Restaurar y validar el JWT antes de exponer rutas privadas, proteger navegación y suspender el acceso ante fallos de comunicación.

**Independent Test**: Recargar con sesión válida; abrir `/home` sin sesión, con JWT expirado o con `/me` en `401`; comprobar redirección y reintento cuando `/me` no esté disponible.

### Implementation for User Story 3

- [ ] T023 [P] [US3] Implementar la consulta de usuario actual con TanStack Query y mapper de `/me` en `frontend/src/features/auth/hooks/useCurrentUser.ts`
- [ ] T024 [US3] Implementar la restauración de sesión y las transiciones `unknown → checking → authenticated` y `unknown → anonymous` en `frontend/src/features/auth/hooks/useAuth.tsx`
- [ ] T025 [US3] Invalidar tokens ausentes, ilegibles o expirados antes de consultar `/me` en `frontend/src/features/auth/hooks/useAuth.tsx`
- [ ] T026 [US3] Transicionar `unknown → checking` al detectar un JWT restaurable y mantener `checking` en fallos de comunicación, informando y ofreciendo reintento sin invalidar el token en `frontend/src/features/auth/hooks/useAuth.tsx`
- [ ] T027 [US3] Invalidar la sesión y navegar a login ante `401` de `/me` o recursos protegidos en `frontend/src/features/auth/hooks/useAuth.tsx`
- [ ] T028 [US3] Implementar guards de `/home`, `/login` y `/register` con estados de verificación y redirección en `frontend/src/app/router/AppRouter.tsx`
- [ ] T029 [US3] Añadir la pantalla de verificación y reintento de sesión en `frontend/src/features/auth/components/SessionChecking.tsx`

**Checkpoint**: Las rutas privadas y públicas respetan el estado real de sesión y los fallos de `/me` no exponen contenido privado.

## Phase 6: User Story 4 - Cerrar sesión (Priority: P1)

**Goal**: Invalidar la sesión local desde `/home` y bloquear el acceso posterior.

**Independent Test**: Iniciar sesión, cerrar sesión desde `/home`, comprobar llegada a `/login` y verificar que una recarga o intento directo de `/home` no restaura acceso.

### Implementation for User Story 4

- [ ] T030 [P] [US4] Añadir acción de cierre de sesión en `frontend/src/features/auth/components/LogoutButton.tsx`
- [ ] T031 [US4] Implementar invalidación local, limpieza de estado y navegación a login en `frontend/src/features/auth/hooks/useLogout.ts`
- [ ] T032 [US4] Conectar la acción de cierre con el estado autenticado en `frontend/src/features/auth/pages/HomePage.tsx`

**Checkpoint**: El cierre de sesión invalida la sesión local de forma independiente del backend.

## Phase 7: User Story 5 - Utilizar páginas de acceso coherentes y adaptables (Priority: P2)

**Goal**: Presentar login y register con identidad visual compartida, layout adaptable y estados de interacción claros.

**Independent Test**: Revisar ambas páginas en escritorio y pantalla estrecha, completar formularios y comprobar loading, éxito, error y ausencia de desplazamiento horizontal.

### Implementation for User Story 5

- [ ] T033 [P] [US5] Crear componentes compartidos de layout y sección visual claramente separados en `frontend/src/features/auth/components/AuthLayout.tsx` y `frontend/src/features/auth/components/AuthVisualPanel.tsx`
- [ ] T034 [US5] Aplicar estilos compartidos, responsive y prevention de desbordamiento horizontal en `frontend/src/styles/index.scss`
- [ ] T035 [US5] Integrar layout, separación visual entre contenido y formulario y estados de carga/error visibles sin obstruir la página en `frontend/src/features/auth/pages/LoginPage.tsx` y `frontend/src/features/auth/pages/RegisterPage.tsx`
- [ ] T036 [US5] Asegurar bloqueo de acciones y feedback visible durante envíos en `frontend/src/features/auth/form/`

**Checkpoint**: Las dos páginas mantienen coherencia visual y son utilizables sin desplazamiento horizontal.

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Revisar coherencia, seguridad y flujo completo sin añadir alcance.

- [ ] T037 Revisar que ningún componente o página accede directamente a HTTP, `localStorage` o al token en `frontend/src/features/auth/`
- [ ] T038 Revisar normalización de errores `400`, `401`, `409` y comunicación en `frontend/src/shared/http/` y `frontend/src/features/auth/`
- [ ] T039 Revisar guards, redirecciones y limpieza de sesión en `frontend/src/app/router/AppRouter.tsx` y `frontend/src/features/auth/hooks/`
- [ ] T040 Validar manualmente el flujo completo siguiendo `specs/004-frontend-user-auth/quickstart.md`
- [ ] T041 Ejecutar `npm run build` desde `frontend/` como único control de código permitido

## Dependencias y orden de ejecución

- Setup (Phase 1) no tiene dependencias y puede comenzar inmediatamente.
- Foundational (Phase 2) depende de Setup y bloquea todas las stories.
- US1, US2, US3 y US4 dependen de Phase 2; US2 reutiliza el token y US3 debe integrarse con login para validar el flujo de restauración.
- US5 depende de los formularios y páginas de US1/US2 para su presentación final.
- Polish depende de las stories deseadas.

## Grafo de dependencias

```text
Setup → Foundational → US1 ─┐
                  → US2 ───┼→ Polish
                  → US3 ───┤
                  → US4 ───┘
                  → US5 ───┘
```

## Oportunidades de ejecución paralela

- T003 y T004 pueden ejecutarse en paralelo tras T001.
- T006 y T007 pueden ejecutarse en paralelo tras definir los contratos.
- T014, T018 y T022 pueden comenzar en paralelo después de Foundational.
- T023, T030 y T033 pueden implementarse en paralelo por archivos y responsabilidades distintos.
- US1, US2, US3 y US4 pueden trabajarse en paralelo después de Phase 2; US5 requiere las páginas base.

## Ejemplo de ejecución paralela por story

### US1

```text
T014 [P] [US1] RegisterPage
T015 [US1] useRegister mutation and validation
```

### US2

```text
T018 [P] [US2] LoginPage
T019 [US2] useLogin
T022 [P] [US2] HomePage
```

### US3

```text
T023 [P] [US3] useCurrentUser
T029 [US3] SessionChecking
T028 [US3] Route guards
```

### US4

```text
T030 [P] [US4] LogoutButton
T031 [US4] useLogout
T032 [US4] HomePage integration
```

## Estrategia de implementación

### MVP

1. Completar Setup y Foundational.
2. Completar US1: registro independiente y retorno a login.
3. Completar US2: login y home temporal.
4. Entregar como MVP validando manualmente el flujo básico.

### Entrega incremental

1. Añadir US3 para persistencia, `/me`, protección de rutas y recuperación de errores.
2. Añadir US4 para completar el ciclo de autenticación.
3. Añadir US5 para coherencia visual y adaptación.
4. Ejecutar el build permitido y la validación manual de quickstart.

## Notas

- [P] identifica tareas ejecutables en paralelo por archivos y dependencias independientes.
- Cada tarea incluye ruta exacta y las tareas de story están etiquetadas con su historia.
- No se generan tests frontend ni tareas de infraestructura de testing.
- El build frontend no acredita la ejecución de los flujos manuales de quickstart.
