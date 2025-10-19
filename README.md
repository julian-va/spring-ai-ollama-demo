# spring-ai-ollama-demo

Proyecto de ejemplo con Spring Boot (módulo `ms-suggestions`) que muestra integración con un backend de modelos (
Ollama).

Resumen rápido

- Documentación y ejemplos para integrar la aplicación con Ollama (local o remoto).

Requisitos mínimos

- JDK 17+.
- Gradle (se recomienda usar el wrapper incluido `./gradlew`).
- Ollama instalado y funcionando en su máquina de desarrollo, OPPOR prueba con un Ollama remoto (dominio/host accesible
  desde la app).

Ollama: formas de uso

Puede usar Ollama de dos maneras principales:

1) Ollama local (recomendado en desarrollo)

- Instalar Ollama siguiendo la documentación oficial: https://ollama.com
- Ejecutar el servicio localmente (según la guía de Ollama). Por convención la API local suele exponerse en
  `http://localhost:11434`, pero confirme según la versión de Ollama.
- Verificar que el servicio responde a la ruta de modelos:

```bash
curl http://localhost:11434/v1/models
```

2) Ollama alojado en un dominio remoto

- Si dispone de un Ollama en otro host (por ejemplo `https://mi-ollama.example.com`), configure la aplicación para
  apuntar a esa URL.
- Asegúrese de que la aplicación pueda acceder al dominio (certificados TLS, reglas de firewall, autenticación si
  aplica).

Configuración de la aplicación (properties / env)

La configuración de Ollama puede proporcionarse mediante `application.yml`/`application.properties` o mediante variables
de entorno (Spring Boot relaxed binding).

Ejemplo `application.properties` (colocar en `ms-suggestions/src/main/resources` o en la configuración activa):

```properties
# URL base del servicio Ollama
ollama.base-url=http://localhost:11434
# Identificador del modelo a usar (ajustar a su instalación)
ollama.llama.model=ollama/local-model
ollama.llama.temperature=0.7
ollama.llama.numPredict=128
ollama.llama.keepAlive=none
ollama.llama.numGPU=0
ollama.connect-timeout-ms=10000
ollama.response-timeout-s=60
```

Ejemplo de variables de entorno equivalentes (útil para Docker):

```bash
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_LLAMA_MODEL=ollama/local-model
export OLLAMA_LLAMA_TEMPERATURE=0.7
export OLLAMA_CONNECT_TIMEOUT_MS=10000
```

Nota: Spring Boot mapeará `OLLAMA_BASE_URL` a la propiedad `ollama.base-url` gracias al relaxed binding.

Endpoints importantes

- `GET /api/ping`
    - Comprobación básica de disponibilidad (devuelve 200 OK y un cuerpo simple, p.ej. `{ "status": "ok" }`).

- `POST /ai/recommender`
    - Endpoint principal para recomendaciones (SSE streaming) ya existente en el proyecto.
    - Usa un payload JSON con `systemMessage` y `userMessage`.

Diagrama de componentes

A continuación un diagrama sencillo que muestra la relación entre el cliente, el servicio `ms-suggestions` y Ollama.
Incluye un diagrama Mermaid (si tu plataforma lo soporta) y un diagrama ASCII de fallback.

Mermaid (visualización si el renderer lo soporta):

```mermaid
graph LR
  Client[Cliente\n(curl / script / UI)] -->|HTTP| MS[ms-suggestions\n(Spring Boot)]
  MS -->|HTTP (Ollama API)| Ollama[Ollama\n(local o remoto)]
  MS -->|SSE (stream)| Client
```

Diagrama ASCII (fallback):

```
Client (curl / script / UI)
    |
    | HTTP
    v
ms-suggestions (Spring Boot)
    |
    | HTTP -> Ollama API
    v
Ollama (local o remoto)
```

Y ms-suggestions puede enviar respuestas en streaming (SSE) de vuelta al cliente.

Comandos útiles

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

- Probar el `ping` (si la app está corriendo en localhost:8080):

```bash
curl -sS http://localhost:8080/api/ping
```

- Probar el recommender (SSE):

```bash
curl -N -H "Content-Type: application/json" -X POST http://localhost:8080/ai/recommender \
  -d '{"systemMessage":"You are a helpful assistant.","userMessage":"Suggest reply options for: I need help with my order"}'
```

Docker

- Construir imagen del módulo `ms-suggestions`:

```bash
docker build -t ms-suggestions:latest -f ms-suggestions/Dockerfile ms-suggestions
```

- Ejecutar contenedor y pasar configuración de Ollama si es necesario:

```bash
docker run --rm -p 8080:8080 \
  -e OLLAMA_BASE_URL=http://10.0.0.2:11434 \
  ms-suggestions:latest
```

Notas y recomendaciones

- Para desarrollo local se recomienda instalar Ollama en la misma máquina y usar `http://localhost:11434` como
  `ollama.base-url`.
- Para entornos de prueba/producción, puede apuntar a un Ollama desplegado en un dominio o servicio gestionado; en ese
  caso, documente las credenciales/seguridad necesarias y asegúrese de usar HTTPS.
- Si el Ollama remoto requiere autenticación adicional, ajuste la configuración del cliente HTTP/WebClient del proyecto
  para incluir los encabezados o mecanismos necesarios.

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

- Puedo añadir ejemplos de curl adicionales, o una pequeña colección para Postman/HTTPie.
- Si desea, agrego el código de `PingController` y su test (si aún no están) y ejecuto los tests para confirmar que
  pasan.

Licencia

- Consulte el fichero `LICENSE` en la raíz del repositorio.

Arquitectura y tecnologías

- Arquitectura: estilo hexagonal / puertos y adaptadores (capas claras: dominio, aplicación,
  adaptadores/infraestructura). El código organiza los paquetes por capas (`domain`, `application`,
  `infrastructure/adapters`), lo que facilita sustituir implementaciones (por ejemplo el cliente de Ollama) y escribir
  tests unitarios.

- Patrón de ejecución: aplicación modular (multi-módulo Gradle) con un módulo principal `ms-suggestions` empaquetado
  como aplicación Spring Boot.

- Tecnologías principales:
    - Kotlin (principalmente) y Java (en partes del proyecto).
    - Spring Boot (WebFlux / reactivo) para exponer la API HTTP y manejar SSE (streaming) en el endpoint
      `/ai/recommender`.
    - WebClient (cliente HTTP reactivo) para comunicarse con la API de Ollama.
    - Integración con Ollama (cliente HTTP hacia `ollama.base-url`) mediante librerías Spring AI/autoconfigure presentes
      en el proyecto.
    - Comunicación SSE (Server-Sent Events) para streaming de recomendaciones hacia clientes.
    - Gradle (con `gradlew` wrapper) para el build y manejo de dependencias.
    - Docker (Dockerfile en `ms-suggestions/`) para construir imágenes del servicio.
    - Pruebas: JUnit 5 (Jupiter) para tests unitarios e integración, con support de bibliotecas Kotlin/Mockito según
      necesidad.
    - OpenAPI / springdoc (documentación automática) — presente en las dependencias del módulo para exponer
      documentación de la API.

- Consideraciones operativas:
    - La app está pensada para entornos reactivos y para integrarse con un servicio de modelo externo (Ollama) a través
      de HTTP; por ello es importante configurar `ollama.base-url` y parámetros de timeout según entorno.
    - Para desarrollo local es conveniente instalar Ollama y apuntar `ollama.base-url` a `http://localhost:11434`.
