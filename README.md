# spring-ai-ollama-demo

Proyecto de ejemplo con Spring Boot (módulo `ms-suggestions`) que muestra integración con un backend de modelos (
Ollama).

Nuevas funcionalidades y notas importantes

- Configuración de red interna con Docker Compose (servicios se resuelven por nombre o alias dentro de la misma red).
- Soporte para ejecutar Ollama con GPU (nota: requiere drivers y configuración del host y del Docker daemon).
- Variables de entorno recomendadas para producción con `SPRING_AI_OLLAMA_BASE_URL` y `SPRING_AI_OLLAMA_MODEL`.
- Healthcheck para Ollama y ejemplos de cómo probar la conexión desde dentro de la red de Compose.
- Recomendaciones para evitar errores comunes de `WebClient` (p. ej. "Host is not specified").

Resumen rápido

- Documentación y ejemplos para integrar la aplicación con Ollama (local o remoto).

Requisitos mínimos

- JDK 17+.
- Gradle (se recomienda usar el wrapper incluido `./gradlew`).
- Docker y Docker Compose (usar `docker compose` preferiblemente). Si quiere ejecutar con GPU: drivers NVIDIA y
  `nvidia-container-toolkit` si trabaja en Linux.

Ollama: formas de uso

Puede usar Ollama de dos maneras principales:

1) Ollama local (recomendado en desarrollo)

- Instalar Ollama siguiendo la documentación oficial: https://ollama.com
- Ejecutar el servicio localmente. Por convención la API local suele exponerse en `http://localhost:11434` (confirme
  según su versión).
- Verificar que el servicio responde a la ruta de modelos:

```bash
curl http://localhost:11434/v1/models
```

2) Ollama dentro de Docker (recomendado para reproducibilidad)

- En este repo hay un `docker-compose.yml` de ejemplo que levanta Ollama y `ms-suggestions` en una red interna
  `app_net`.
- Nota importante: no use guiones bajos en los aliases/hosts porque en algunas APIs de URI (JVM) no son válidos; use
  `ollama-service` como alias si necesita un nombre sin guion bajo.

Configuración recomendada de `docker-compose.yml` (puntos clave)

- Mantener la variable que usa la app:
    - `SPRING_AI_OLLAMA_BASE_URL=http://ollama-service:11434`  <- NO cambiar esto para la app, debe resolver dentro de
      la red.
- Pasar el modelo con dos puntos entre comillas o via `.env`:
    - `SPRING_AI_OLLAMA_MODEL: "llama3:8b"`

Ejemplo mínimo (fragmento relevante):

```yaml
services:
  ollama-service:
    image: ollama/ollama:latest
    container_name: ollama_container
    volumes:
      - ollama-data:/root/.ollama
    # si usa GPU en host y tiene nvidia-container-toolkit/daemon configurado, puede usar runtime: nvidia
    # runtime: nvidia  # legacy
    environment:
      - OLLAMA_HOST=0.0.0.0
      - OLLAMA_MODELS=/root/.ollama/models
    networks:
      app_net:
        aliases:
          - ollama-service

  spring-boot-service-ms-suggestions:
    build: ./ms-suggestions
    environment:
      SPRING_AI_OLLAMA_BASE_URL: "http://ollama-service:11434"
      SPRING_AI_OLLAMA_MODEL: "llama3:8b"
      SPRING_PROFILES_ACTIVE: "prod"
    networks:
      - app_net

networks:
  app_net:
    driver: bridge
volumes:
  ollama-data:
```

Nota sobre mapeo de puertos y conflictos en el host

- Si publica Ollama en el host con `ports: - "11434:11434"` y el puerto ya está en uso, Compose fallará con
  `address already in use`.
- Solución: cambiar sólo el puerto host (ej. `11435:11434`) o eliminar la sección `ports` para que Ollama sea accesible
  sólo desde la red interna (más seguro).
- Si cambia el puerto mapeado en el host, la app que corre dentro de Compose NO necesita cambiar
  `SPRING_AI_OLLAMA_BASE_URL` (la app habla con el nombre del servicio interno). Debe cambiar únicamente comandos curl
  desde el host que apunten a `localhost:11435`.

Healthcheck y arranque fiable

- Añadir un `healthcheck` en `docker-compose.yml` para Ollama ayuda a `depends_on` a ser más expresivo (Compose v3 no
  espera por salud por defecto, pero sirve para diagnóstico):

```yaml
healthcheck:
  test: [ "CMD", "curl", "-f", "http://localhost:11434/v1/models" ]
  interval: 10s
  timeout: 5s
  retries: 6
```

GPU vs CPU: cómo saber si Ollama está usando GPU

- El log de Ollama muestra qué backend carga, por ejemplo:
  `load_backend: loaded CPU backend from /usr/lib/ollama/libggml-cpu-haswell.so` indica CPU.
- Para usar GPU dentro de Docker necesitas:
    1. Drivers GPU en el host (p. ej. NVIDIA drivers).
    2. `nvidia-container-toolkit` (o el equivalente para tu plataforma) instalado.
    3. Configurar Docker para exponer la GPU (en Compose local a veces se usan `device_requests` o `runtime: nvidia` en
       instalaciones legacy).

Ejemplo breve para Compose (si su Docker soporta `device_requests`):

```yaml
services:
  ollama-service:
    image: ollama/ollama:latest
    device_requests:
      - driver: nvidia
        count: all
        capabilities: [ "gpu" ]
```

Si el validador de Compose en su máquina da error con `device_requests`, puede usar el enfoque legacy
`runtime: nvidia` + `NVIDIA_VISIBLE_DEVICES=all` (requerirá que el daemon tenga configurado ese runtime).

Comprobaciones útiles (diagnóstico)

- Ver red y contenedores conectados:

```bash
docker network ls --filter name=spring-ai-ollama-demo_app_net
docker network inspect spring-ai-ollama-demo_app_net
```

- Probar Ollama desde dentro de la red:

```bash
docker run --rm --network spring-ai-ollama-demo_app_net curlimages/curl:8.1.2 -sS http://ollama-service:11434/v1/models
```

- Ver logs en tiempo real:

```bash
docker compose logs -f ollama-service
docker compose logs -f ms-suggestions-app
```

- Comprobar variables de entorno dentro del contenedor de la app:

```bash
docker compose exec ms-suggestions-app printenv SPRING_AI_OLLAMA_BASE_URL
```

- Si habilitó GPU y la imagen lo soporta, comprobar dentro del contenedor:

```bash
docker compose exec ollama-service nvidia-smi
```

Evitar el error WebClient: "Host is not specified"

Síntomas: `WebClientRequestException: Host is not specified` al intentar hacer una petición a
`http://ollama_service:11434/...`.

Causas y soluciones:

- Hostname inválido para construcción de URI. Evite usar guion bajo `_` en alias/host. Use `ollama-service` o `ollama` (
  sin `_`) como nombre de host/alias.
- Asegurar que la variable de entorno que la app usa (`SPRING_AI_OLLAMA_BASE_URL`) está presente dentro del contenedor y
  no está vacía:

```bash
docker compose exec ms-suggestions-app printenv SPRING_AI_OLLAMA_BASE_URL
```

- Preferir definir variables en el `docker-compose.yml` como mapping (clave: valor) para evitar parseos raros:

```yaml
environment:
  SPRING_AI_OLLAMA_BASE_URL: "http://ollama-service:11434"
  SPRING_AI_OLLAMA_MODEL: "llama3:8b"
```

- Asegurarse de que la configuración de Spring está usando el prefijo correcto: `spring.ai.ollama.base-url` o una
  propiedad que su configuración realmente lea. En este proyecto recomendamos `SPRING_AI_OLLAMA_BASE_URL` y binding en
  `application-prod.yml`.

Curles útiles (ejemplos)

- Desde el host (si Ollama está publicado en `localhost:11435` en el host):

```bash
curl -sS -X POST "http://localhost:11435/api/chat" \
  -H "Content-Type: application/json" \
  --data-raw '{
    "model": "llama3:8b",
    "messages": [
      {"role": "system", "content": "You are a helpful assistant."},
      {"role": "user", "content": "Resume en 2 frases qué hace Docker Compose."}
    ]
  }'
```

- Desde la red interna (ejecutando curl en un contenedor que comparte la red):

```bash
docker run --rm --network spring-ai-ollama-demo_app_net curlimages/curl:8.1.2 -sS -X POST http://ollama-service:11434/api/chat \
  -H "Content-Type: application/json" \
  --data-raw '{"model":"llama3:8b","messages":[{"role":"system","content":"You are a helpful assistant."},{"role":"user","content":"Hola"}]}'
```

Cómo pasar `llama3:8b` en `docker-compose.yml`

- Use comillas si usa el formato mapa (recomendado):

```yaml
environment:
  SPRING_AI_OLLAMA_MODEL: "llama3:8b"
```

- Alternativamente coloque en un `.env` y use `env_file` si prefiere separar secretos/ajustes.

Cómo descargar un modelo automáticamente en el inicio del contenedor (opcional)

- Si desea que el contenedor Ollama intente descargar un modelo al arrancar, puede usar un `entrypoint` o `command` que
  lance `ollama serve` en background y luego ejecute `ollama pull`. Ejemplo (sólo si lo necesita):

```yaml
entrypoint: [ "/bin/sh", "-c" ]
command: -c "ollama serve & sleep 5 && ollama pull llama3:8b && wait"
```

(Usar con precaución: puede complicar el arranque y los healthchecks.)

Comandos útiles del proyecto

- Construir el proyecto:

```bash
./gradlew clean build
```

- Ejecutar el módulo `ms-suggestions` en modo desarrollo:

```bash
./gradlew :ms-suggestions:bootRun
```

- Ejecutar tests:

```bash
./gradlew test
```

- Construir imagen Docker del módulo `ms-suggestions`:

```bash
docker build -t ms-suggestions:latest -f ms-suggestions/Dockerfile ms-suggestions
```

Depuración y siguientes pasos

- Si vuelve a ver `Host is not specified`, inspeccione el valor de `SPRING_AI_OLLAMA_BASE_URL` dentro del contenedor,
  cambie el alias a uno que no use `_` y asegúrese que la app carga el perfil `prod` si usa `application-prod.yml`.
- Puedo añadir un script de espera (wait-for) para que la app solo intente conectar cuando Ollama esté sano, o añadir un
  health check más completo para Ollama.

Contribuir

1. Cree una rama a partir de `main`:

```bash
git checkout -b feature/mi-nueva-funcionalidad
```

2. Añada tests y ejecute el suite localmente:

```bash
./gradlew test
```

3. Haga commit, push y abra un Pull Request.

Contacto / siguientes pasos

- Puedo añadir ejemplos de curl adicionales, un Postman collection o automatizar la descarga del modelo al inicio si lo
  desea.

Licencia

- Consulte el fichero `LICENSE` en la raíz del repositorio.

Arquitectura y tecnologías

- Arquitectura: estilo hexagonal / puertos y adaptadores (capas claras: dominio, aplicación,
  adaptadores/infraestructura). El código organiza los paquetes por capas (`domain`, `application`,
  `infrastructure/adapters`).
- Tecnologías principales: Kotlin, Spring Boot (WebFlux), WebClient, Ollama integration, SSE streaming, Gradle, Docker.
