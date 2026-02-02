# Architecture — spring-ai-ollama-demo

This document describes the architecture of the `spring-ai-ollama-demo` monorepo in detail: components and
responsibilities, deployment topology, authentication flow, endpoint map, security matrix and key environment variables.

## 1. High-level overview

- Purpose: demo/integration project that shows how a Spring Boot microservice (`ms-suggestions`) can call a local Ollama
  model backend while access is protected by Keycloak and routed through a single API Gateway (`ms-api-gateway`).

- Primary components:
    - `ms-api-gateway`: Spring Cloud Gateway — single public entrypoint, handles routing, authentication and JWT
      validation.
    - `ms-suggestions`: Spring Boot microservice — business logic, sends requests to Ollama for inference (sync and
      streaming endpoints).
    - `ollama-service`: Ollama model server container — model runtime used by `ms-suggestions`.
    - `keycloak`: Keycloak server container — identity provider, realm imported from `docker/realm-import.json`.
    - `keycloak-db`: PostgreSQL database for Keycloak.

## 2. Component responsibilities

- ms-api-gateway
    - Exposes a single public port (8080) to host.
    - Defines routes and rewrites to backend services (configured in `application-prod.yml`).
    - Proxies Keycloak endpoints under `/public/auth/**` (token endpoint allowed publicly for obtaining tokens).
    - Validates JWT tokens (uses issuer-uri to configure a ReactiveJwtDecoder).
    - Maps realm roles from `realm_access.roles` to Spring authorities `ROLE_<role>`.
    - Enforces authorization rules (e.g. `ollama-user` for app endpoints, `ollama-admin` for admin/proxied routes).

- ms-suggestions
    - Receives validated requests from gateway.
    - Calls Ollama base URL (configured via `SPRING_AI_OLLAMA_BASE_URL`) for inference.
    - Supports synchronous and streaming recommender endpoints (`/ai/recommender/sync`, `/ai/recommender/stream`).

- Ollama
    - Serves model endpoints via its API (e.g., `/v1/models`, `/api/chat`), listening inside the container on 11434.
    - Accessed by `ms-suggestions` through the Compose network.

- Keycloak
    - Identity provider with pre-imported realm `ollama-realm` (see `docker/realm-import.json`).
    - Provides token endpoint used by clients to obtain JWTs.

## 3. Deployment topology

- All services run on a single Docker Compose network (`app_net`).
- Default exposure:
    - Gateway: `localhost:8080` (mapped `8080:8080` in `docker-compose.yml`).
    - Keycloak and Ollama are not exposed to host by default (reachable by service name inside the network).

## 4. Auth / request sequence

Sequence for a typical authenticated client call:

1. Client requests token (password grant) by POSTing to the token endpoint. If Keycloak is not published to host, the
   client calls the gateway token proxy:

   POST http://localhost:8080/public/auth/realms/ollama-realm/protocol/openid-connect/token

2. Gateway proxies the token request to Keycloak. Keycloak validates credentials and returns a JWT.
3. Client uses the JWT to call protected application endpoints via the gateway:

   GET/POST http://localhost:8080/ai/recommender/sync (or /stream)

4. Gateway validates the JWT (checks issuer, signature, expiry) and extracts roles from `realm_access.roles` claim.
5. If authorization passes, gateway forwards the request to `ms-suggestions`.
6. `ms-suggestions` may call Ollama (internal URL `http://ollama-service:11434`) to perform inference and returns a
   response through the gateway to the client.

## 5. Endpoint map (routes configured in `ms-api-gateway`)

The gateway route list (from `application-prod.yml`) includes:

- `/ai/recommender/stream` => `SUGGESTIONS_SERVICE_URI` (streaming recommender)
- `/ai/recommender/sync` => `SUGGESTIONS_SERVICE_URI` (synchronous recommender)
- `/suggestions/api-docs/**` => `SUGGESTIONS_SERVICE_URI` (docs)
- `/public/auth/**` => `KEYCLOAK_AUTH_URI` (Keycloak proxy)

Notes:

- Gateway rewrites paths as configured in `application-prod.yml`.
- The gateway applies security rules: token endpoint is publicly permitted, other `/public/**` routes may be protected (
  see `SecurityConfig`).

## 6. Security matrix

- Roles (as defined in `docker/realm-import.json`):
    - `ollama-user` — intended for regular clients that call recommendation endpoints.
    - `ollama-admin` — administrative operations, gateway-proxied Keycloak admin routes.

- Mapping and enforcement (gateway):
    - `/public/auth/.../token` — permitAll (needed to obtain tokens).
    - `/public/**` (other paths) — requires `ollama-admin` (configurable in `SecurityConfig`).
    - `/ai/recommender/*` and other app endpoints — requires `ollama-user`.

## 7. Config & important env vars

- `SPRING_AI_OLLAMA_BASE_URL` — base URL used by `ms-suggestions` to call Ollama (e.g. `http://ollama-service:11434`).
- `SPRING_AI_OLLAMA_MODEL` — model identifier to request from Ollama (e.g. `qwen3:latest`).
- `SPRING_PROFILES_ACTIVE` — e.g. `prod` to load production configs.
- Keycloak-related envs in `docker-compose.yml`:
    - `KC_BOOTSTRAP_ADMIN_USERNAME` / `KC_BOOTSTRAP_ADMIN_PASSWORD` — admin console bootstrap credentials.
    - `KEYCLOAK_CLIENT_SECRET` used by `ms-api-gateway` via `KEYCLOAK_CLIENT_SECRET` env.
    - `KEYCLOAK_ISSUER_URI` / `KEYCLOAK_AUTH_URI` — gateway configuration to validate tokens and proxy auth routes.

## 8. Operational notes & troubleshooting

- If gateway returns "Host is not specified" or WebClient errors: check that `SPRING_AI_OLLAMA_BASE_URL` is correctly
  set and resolvable inside the Compose network (avoid underscores in service name).
- If Keycloak fails to import realm: check `keycloak-db` is healthy and Keycloak logs for import errors. Keycloak may
  take time to become ready while DB initializes.
- Follow logs:
    - `docker compose logs -f ms-api-gateway-app`
    - `docker compose logs -f ms-suggestions-app`
    - `docker compose logs -f keycloak`
    - `docker compose logs -f ollama_container`

## Diagram (PlantUML)

A PlantUML diagram is provided in `docs/architecture.puml`. You can render it locally or via Docker. Below are a few
convenient commands:

1) Render with the official PlantUML Docker image (recommended):

```bash
# From repository root
docker run --rm -v "$(pwd)":/workspace plantuml/plantuml -tpng docs/architecture.puml
# Output: docs/architecture.png
```

2) Render to SVG with Docker:

```bash
docker run --rm -v "$(pwd)":/workspace plantuml/plantuml -tsvg docs/architecture.puml
# Output: docs/architecture.svg
```

3) Render with a local plantuml.jar (if you have it):

```bash
java -jar plantuml.jar -tpng docs/architecture.puml
# or
java -jar plantuml.jar -tsvg docs/architecture.puml
```

4) Preview online using PlantUML server: open `https://www.plantuml.com/plantuml` and paste the content of
   `docs/architecture.puml`, or use an editor/IDE plugin that supports PlantUML.

Notes:

- The Docker commands mount the repository root into the container, so outputs are written into `docs/` alongside the
  `.puml` file.
- If you prefer a rendered image checked into the repo, I can generate `docs/architecture.png` and add it (ask and I
  will create it).

## 9. Recommended enhancements

- Add a lightweight healthcheck for Ollama in `docker-compose.yml` (curl `/v1/models`), and use `start_period` so other
  services wait until Ollama is ready.
- Add a `wait-for` script for `ms-suggestions` to retry Ollama/Keycloak availability during startup.
- Consider using TLS for the gateway and Keycloak in non-local environments and secrets management for client secrets.

---

For more concise usage and run instructions see `README.md`.
