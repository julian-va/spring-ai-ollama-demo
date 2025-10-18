# Spring AI Ollama Demo

This repository demonstrates a small multi-module Java/Kotlin project intended to show how a Spring-based service can be
integrated with AI/LLM tooling (project name references "Ollama"). The project contains a core module (`ms-suggestions`)
that provides suggestion-related functionality and is packaged as an executable JAR and Docker image.

> Assumptions
> - The repository name suggests the project demonstrates integration with Ollama or an LLM; the codebase includes a
    > `ms-suggestions` module built with Kotlin and packaged as a Spring Boot application (inferred from existing
    > artifacts and a `Dockerfile`). If this assumption is incorrect, adjust the module description accordingly.

Main qualities

- Modular Gradle project: the repository uses Gradle (wrapper included) and organizes code into modules.
- Multi-language sources: Java and Kotlin sources are present across modules.
- Spring-based service (ms-suggestions): packaged as an executable JAR and Docker image.
- Reproducible builds: includes `gradlew` wrapper and a `Dockerfile` for containerized runs.
- Tests supported: Gradle tasks for running unit/integration tests.

Repository structure (high-level)

- `ms-suggestions/` — Kotlin-based module; builds an executable JAR and includes a `Dockerfile`.
- `buildSrc/` — Gradle build logic and helper code.
- `src/` — (older or sample code) top-level sources if present.
- `build.gradle.kts`, `settings.gradle.kts`, `gradlew`, `gradlew.bat` — project build configuration and wrappers.

Quick reference (extracted from the code)

- Main class (resolved): `jva.cloud.infrastructure.MsSuggestionsApplicationKt`
- Built jars (examples found in `ms-suggestions/build/libs`):
    - `ms-suggestions-0.0.1-SNAPSHOT.jar` (Spring Boot executable jar)
    - `ms-suggestions-0.0.1-SNAPSHOT-plain.jar` (plain jar)

- Default HTTP port: 8080 (Spring Boot default; if you have an `application.properties`/`application.yml` override, that
  will take precedence).

HTTP API (important endpoint)

- POST /ai/recommender
    - Consumes: application/json
    - Produces: text/event-stream (SSE)
    - Request body shape (JSON) — matches `MessageSuggestionEntity` in the code:

      {
      "systemMessage": "<system instructions for the model>",
      "userMessage": "<user text to get suggestions for>"
      }

    - Example: retrieve streaming recommendations (SSE)

      ```bash
      # Use curl with -N to disable buffering so SSE events stream to your terminal
      curl -N -H "Content-Type: application/json" \
        -X POST http://localhost:8080/ai/recommender \
        -d '{"systemMessage":"You are a helpful assistant.","userMessage":"Suggest reply options for: I need help with my order"}'
      ```

      The endpoint returns Server-Sent Events (SSE) with text payloads; each event will appear as it is produced.

Try it (quick examples)

- Using the helper script (recommended for convenience):

```bash
# Make sure the app is running (bootRun or jar) and then execute the script
ms-suggestions/scripts/stream_suggestions.sh "You are a helpful assistant." "Suggest reply options for: I need help with my order" sse_output.txt

# The script requires 'jq' to build the JSON payload. It will write streamed events to sse_output.txt
```

- Direct curl (SSE streaming):

```bash
curl -N -H "Content-Type: application/json" \
  -X POST http://localhost:8080/ai/recommender \
  -d '{"systemMessage":"You are a helpful assistant.","userMessage":"Suggest reply options for: I need help with my order"}'
```

- HTTPie (if you prefer HTTPie):

```bash
http --stream POST http://localhost:8080/ai/recommender Content-Type:application/json \
  systemMessage='You are a helpful assistant.' userMessage='Suggest reply options for: I need help with my order'
```

Run / Build

1. Build the project (from repository root):

```bash
./gradlew clean build
```

2. Run the `ms-suggestions` module directly with Gradle (recommended during development):

```bash
./gradlew :ms-suggestions:bootRun
```

This uses the Spring Boot plugin and will start the application (main class shown above).

3. Run the built jar after `./gradlew build`:

```bash
# Example path; adjust version if different
java -jar ms-suggestions/build/libs/ms-suggestions-0.0.1-SNAPSHOT.jar
```

Docker

To build and run the `ms-suggestions` Docker image (Docker must be installed and running):

```bash
# build image
docker build -t ms-suggestions:latest -f ms-suggestions/Dockerfile ms-suggestions

# run container (example)
docker run --rm -p 8080:8080 ms-suggestions:latest
```

Configuration and environment

- Check `ms-suggestions/src/main/resources` for `application.properties` or `application.yml` if you need to change the
  port or other settings.
- If the project integrates with an LLM backend (Ollama or similar), set any required API URL/credentials via
  environment variables before running. The project uses `spring-ai-starter-model-ollama` according to the module
  dependencies, so review the module documentation or code that configures the Ollama model for exact env vars.

Development notes

- Use the Gradle wrapper to ensure consistent builds across environments: `./gradlew`.
- IDEs like IntelliJ IDEA will import the Gradle project and detect Kotlin/Java sources automatically.

Troubleshooting

- Build fails: run `./gradlew clean build --stacktrace` and inspect the output.
- Missing JAR or wrong main class: verify `ms-suggestions` module's Gradle settings; the resolved main class is listed
  above.
- SSE not streaming: use `curl -N` (or an SSE-capable client) to receive events as they are produced; some HTTP clients
  buffer responses by default.

Contributing (short guide)

1. Fork the repository and create a feature branch off `main` (or the mainline branch you use):

```bash
git checkout -b feature/your-feature
```

2. Add tests for new behavior and run the test suite locally:

```bash
./gradlew test
```

3. Commit, push, and open a Pull Request describing your changes.

License

- See `LICENSE` in the repository root.

Contact / Next steps

- I can also:
    - Add example curl requests and small scripts to parse SSE output into files.
    - Inspect `ms-suggestions/src/main/resources` and any config classes to extract exact environment variables used for
      Ollama integration and add them to this README.
    - Add a tiny Postman/HTTPie collection for easier manual testing.

Ollama configuration (exact properties)

The module `ms-suggestions` contains an `OllamaConfig` class that reads the following Spring properties. You can set
them in `application.properties`/`application.yml`, or provide them via environment variables when starting the app (
Spring Boot will map environment variables using relaxed binding).

- `ollama.base-url` — Base URL of the Ollama API (e.g. `http://localhost:11434`).
- `ollama.llama.model` — Model identifier to use (example: `llama2` or a local model name).
- `ollama.llama.temperature` — Model temperature (double).
- `ollama.llama.numPredict` — Number of tokens to predict (int).
- `ollama.llama.keepAlive` — Keep-alive option for the model (string).
- `ollama.llama.numGPU` — Number of GPUs to use (int).
- `ollama.connect-timeout-ms` — Connection timeout in milliseconds (int).
- `ollama.response-timeout-s` — Response timeout in seconds (long).

Example `application.properties` snippet (place under `ms-suggestions/src/main/resources` or in your active
configuration):

```properties
# Ollama example config
ollama.base-url=http://localhost:11434
ollama.llama.model=ollama/local-model
ollama.llama.temperature=0.7
ollama.llama.numPredict=128
ollama.llama.keepAlive=none
ollama.llama.numGPU=0
ollama.connect-timeout-ms=10000
ollama.response-timeout-s=60
```

Example: pass the same settings as environment variables (Docker or runtime):

```bash
docker run --rm -p 8080:8080 \
  -e OLLAMA_BASE_URL=http://10.0.0.2:11434 \
  -e OLLAMA_LLAMA_MODEL=ollama/local-model \
  -e OLLAMA_LLAMA_TEMPERATURE=0.7 \
  -e OLLAMA_CONNECT_TIMEOUT_MS=10000 \
  ms-suggestions:latest
```

Note: Spring Boot relaxed binding maps environment variables like `OLLAMA_BASE_URL` to the property `ollama.base-url`.

Bean names used in the code

- The `OllamaConfig` class exposes a chat client bean named `"ollama"` (available by qualifier `@Qualifier("ollama")`).
- A separate WebClient builder is named `"ollama-webclient"`.
