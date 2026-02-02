# spring-ai-ollama-demo

A sample Kotlin + Spring Boot monorepo that demonstrates integration with an Ollama model backend. The repository
contains a suggestions microservice (`ms-suggestions`) which calls Ollama, and an API gateway (`ms-api-gateway`). The
project includes Dockerfiles and a `docker-compose.yml` example to run Ollama and the services together.

## Table of Contents

- Quick Start
    - Prerequisites
    - Build
    - Run with Docker Compose
    - Run locally
- Configuration
    - Environment variables
    - application-prod.yml binding
- Security (Keycloak) and tokens
- Healthchecks & Debugging
- Examples (curl)
- Development
    - Run module
    - Tests
    - Build images
- Contributing
- License

## Quick Start

### Prerequisites

- JDK 17+
- Docker (20.x+) and Docker Compose (v2 `docker compose` recommended)
- (Optional) GPU drivers and `nvidia-container-toolkit` if you want to run Ollama with GPU support
- Use the included Gradle wrapper: `./gradlew`

### Build

From repository root:

```bash
./gradlew clean build
```

This builds the modules and runs unit tests.

### Run with Docker Compose

Start all services (Ollama, suggestions service, API gateway, Keycloak and DB):

```bash
docker compose up --build
```

Or run detached:

```bash
docker compose up --build --detach
```

Important notes:

- The `ms-suggestions` service expects `SPRING_AI_OLLAMA_BASE_URL` to resolve to the Ollama service inside the Compose
  network (example: `http://ollama-service:11434`).
- Avoid underscores in service aliases (e.g. use `ollama-service`) because some JVM URI APIs reject hostnames with `_`.

### Run locally (without Docker)

Export required environment variables (examples):

```bash
export SPRING_AI_OLLAMA_BASE_URL="http://localhost:11434"
export SPRING_AI_OLLAMA_MODEL="qwen3:latest"
export SPRING_PROFILES_ACTIVE=dev
```

Run the suggestions service in development mode:

```bash
./gradlew :ms-suggestions:bootRun
```

## Configuration

The production profile (`application-prod.yml`) binds to environment variables. Key properties:

- `ollama.base-url` is bound to `${SPRING_AI_OLLAMA_BASE_URL}`
- `ollama.llama.model` is bound to `${SPRING_AI_OLLAMA_MODEL}`

Example `ms-suggestions/src/main/resources/application-prod.yml` relevant fragment:

```yaml
ollama:
  base-url: ${SPRING_AI_OLLAMA_BASE_URL}
  llama:
    model: ${SPRING_AI_OLLAMA_MODEL}
```

Recommended environment values for Compose (example):

```yaml
environment:
  SPRING_AI_OLLAMA_BASE_URL: "http://ollama-service:11434"
  SPRING_AI_OLLAMA_MODEL: "qwen3:latest"
  SPRING_PROFILES_ACTIVE: "prod"
```

## Security (Keycloak) and tokens

This repository supplies a Keycloak realm import (`docker/realm-import.json`) with a realm named `ollama-realm`. The
realm includes a confidential client and a couple of users which are imported automatically when the Keycloak container
starts (see `docker-compose.yml`).

Important notes:

1. The API Gateway (`ms-api-gateway`) is the only service exposed to the host by default (see `ports: "8080:8080"` in
   `docker-compose.yml`). All external requests should go through the gateway. Other services (Keycloak, Ollama,
   suggestions) are only reachable inside the Compose network unless you explicitly expose their ports.

2. Pre-imported users (from `docker/realm-import.json`):

- `test_user_ollama` / `123456789` (role: `ollama-user`)
- `admin_ollama` / `admin123456` (role: `ollama-admin`)

The Keycloak admin bootstrap user is also set via environment variables in `docker-compose.yml`: `admin` / `admin`.

Requesting an access token (password grant) from inside the Compose network

If you do not publish Keycloak to the host, use a container inside the Compose network to call the token endpoint.
Example using the official curl image:

```bash
docker run --rm --network spring-ai-ollama-demo_app_net curlimages/curl:8.1.2 -sS -X POST \
  http://keycloak:8080/realms/ollama-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=spring-gateway" \
  --data-urlencode "client_secret=spring-gateway-secret" \
  --data-urlencode "username=test_user_ollama" \
  --data-urlencode "password=123456789" \
  --data-urlencode "scope=openid"
```

This returns a JSON containing `access_token`. To extract it using `jq`:

```bash
TOKEN=$(docker run --rm --network spring-ai-ollama-demo_app_net curlimages/curl:8.1.2 -sS -X POST \
  http://keycloak:8080/realms/ollama-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=spring-gateway" \
  --data-urlencode "client_secret=spring-gateway-secret" \
  --data-urlencode "username=test_user_ollama" \
  --data-urlencode "password=123456789" \
  --data-urlencode "scope=openid" | jq -r .access_token)

echo "$TOKEN"
```

Call the API Gateway using the token (from host):

```bash
curl -sS -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/your-protected-endpoint
```

If you prefer to access Keycloak directly from the host (admin console or token endpoint), add a port mapping for
Keycloak in `docker-compose.yml` under the `keycloak` service. Example (add this snippet under `keycloak`):

```yaml
ports:
  - "8081:8080" # maps Keycloak to host:8081
```

After adding that mapping you can open the admin console at `http://localhost:8081` and use the bootstrap admin
credentials `admin` / `admin`.

### Access to Keycloak through the API Gateway

- The `ms-api-gateway` includes a route that proxies Keycloak under the path `/public/auth/**` and rewrites the path
  when forwarding it to the Keycloak service. This is configured in
  `ms-api-gateway/src/main/resources/application-prod.yml` as the `auth-keycloak` route.

- Example: to request a token via the gateway (when Keycloak is NOT exposed to the host), call the gateway endpoint:

```bash
# Token request proxied via gateway
curl -sS -X POST http://localhost:8080/public/auth/realms/ollama-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=spring-gateway" \
  -d "client_secret=spring-gateway-secret" \
  -d "username=test_user_ollama" \
  -d "password=123456789" \
  -d "scope=openid"
```

- Important: the request to obtain a token does NOT require a prior token (it is the operation that returns the
  `access_token`). However, any request to the API's protected endpoints (for example `/ai/recommender/*`) must include
  a valid `Authorization: Bearer <token>` header.

- If you want certain administrative Keycloak routes to be protected by the Gateway (for example, only users with the
  `ollama-admin` role can access them via proxy), tell me and I can propose a change in `SecurityConfig` to require
  specific roles on the proxied routes. Note that restricting the token endpoint would prevent clients from obtaining
  tokens via that route unless another mechanism is provided.

Security recommendations

- Do not use the default bootstrap passwords in production. Rotate secrets and use secure values.
- Use HTTPS/TLS for the gateway and Keycloak in production.
- Limit the scopes and roles assigned to clients and users following the principle of least privilege.

## Healthchecks & Debugging

- `docker compose logs -f ms-suggestions-app` and `docker compose logs -f ollama-service` to follow logs.
- Check Ollama models endpoint from host or inside the network:

From host (if you mapped ports):

```bash
curl http://localhost:11434/v1/models
```

From a container in the Compose network:

```bash
docker run --rm --network spring-ai-ollama-demo_app_net curlimages/curl:8.1.2 -sS http://ollama-service:11434/v1/models
```

- Verify `SPRING_AI_OLLAMA_BASE_URL` inside the running app:

```bash
docker compose exec ms-suggestions-app printenv SPRING_AI_OLLAMA_BASE_URL
```

Common issue: `WebClientRequestException: Host is not specified` — usually caused by invalid hostname (underscore) or
empty/incorrect `SPRING_AI_OLLAMA_BASE_URL`.

### Healthcheck example (Compose)

```yaml
healthcheck:
  test: [ "CMD", "curl", "-f", "http://localhost:11434/v1/models" ]
  interval: 10s
  timeout: 5s
  retries: 6
```

If using GPU in Docker, ensure host drivers + `nvidia-container-toolkit` are installed. Compose may use
`device_requests` or legacy `runtime: nvidia`.

## Examples (curl)

From host (if Ollama port is published to host):

```bash
curl -sS -X POST "http://localhost:11435/api/chat" \
  -H "Content-Type: application/json" \
  --data-raw '{
    "model": "qwen3:latest",
    "messages": [
      {"role": "system", "content": "You are a helpful assistant."},
      {"role": "user", "content": "Summarize Docker Compose in 2 sentences."}
    ]
  }'
```

From inside the Compose network (directly to Ollama):

```bash
docker run --rm --network spring-ai-ollama-demo_app_net curlimages/curl:8.1.2 -sS -X POST http://ollama-service:11434/api/chat \
  -H "Content-Type: application/json" \
  --data-raw '{"model":"qwen3:latest","messages":[{"role":"system","content":"You are a helpful assistant."},{"role":"user","content":"Hello"}]}'
```

## Development

Run the suggestions module locally:

```bash
./gradlew :ms-suggestions:bootRun
```

Run tests for the whole project:

```bash
./gradlew test
```

Build Docker images manually (optional):

```bash
docker build -t ms-suggestions:latest -f ms-suggestions/Dockerfile ms-suggestions
docker build -t ms-api-gateway:latest -f ms-api-gateway/Dockerfile ms-api-gateway
```

## Contributing

1. Create a branch from `main`:

```bash
git checkout -b feature/my-new-feature
```

2. Add tests and run `./gradlew test`.
3. Commit, push and open a Pull Request.

## License

See the `LICENSE` file in the repository root.
