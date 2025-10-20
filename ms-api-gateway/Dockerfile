# syntax=docker/dockerfile:experimental
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

COPY ./gradlew /workspace/app
COPY ./gradle /workspace/app/gradle
COPY ./settings.gradle.kts /workspace/app/settings.gradle.kts
COPY ./buildSrc /workspace/app/buildSrc
COPY ./ms-api-gateway /workspace/app/ms-api-gateway

# Permite pasar el nombre del JAR en tiempo de build; si no se pasa, se detecta automáticamente
ARG APP_JAR=""

# Garantiza que el wrapper de Gradle sea ejecutable dentro del contenedor
RUN chmod +x /workspace/app/gradlew

# Usa cache de gradle y construye únicamente el artefacto necesario (bootJar)
RUN --mount=type=cache,target=/root/.gradle /workspace/app/gradlew clean bootJar -x test

# Extrae el contenido del jar para hacer una imagen runtime más pequeña
RUN mkdir -p build/dependency && \
    cd build/dependency && \
    if [ -n "$APP_JAR" ]; then \
      echo "Using provided APP_JAR=$APP_JAR" && jar -xf "/workspace/app/ms-api-gateway/build/libs/$APP_JAR"; \
    else \
      JAR=$(ls /workspace/app/ms-api-gateway/build/libs/ms-api-gateway*.jar 2>/dev/null | head -n 1 || true); \
      if [ -z "$JAR" ]; then \
        JAR=$(ls /workspace/app/ms-api-gateway/build/libs/*.jar 2>/dev/null | head -n 1 || true); \
      fi; \
      if [ -z "$JAR" ]; then \
        echo "No JAR found in /workspace/app/ms-api-gateway/build/libs" >&2; exit 1; \
      fi; \
      echo "Auto-detected JAR=$JAR" && jar -xf "$JAR"; \
    fi

# Runtime stage: usa JRE más ligero
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL org.opencontainers.image.title="ms-api-gateway" \
      org.opencontainers.image.description="Microservice API Gateway (Spring Boot)" \
      org.opencontainers.image.version="0.0.1" \
      org.opencontainers.image.authors="maintainer@example.com"

ARG DEPENDENCY=/workspace/app/build/dependency
WORKDIR /app

# Copia solo lo que se necesita para ejecutar la app
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib ./lib
COPY --from=build ${DEPENDENCY}/META-INF ./META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes ./

# Instala curl para el HEALTHCHECK (alpine)
RUN apk add --no-cache curl

# Crea un usuario no root y asigna permisos al directorio de la app
RUN addgroup -S app && adduser -S app -G app && chown -R app:app /app
USER app

# Permite inyectar opciones de JVM en tiempo de ejecución
ENV JAVA_OPTS=""

EXPOSE 8086

# ENTRYPOINT: usa la clase main Java de este módulo
# Usa shell para expandir JAVA_OPTS; clases están en /app y libs en /app/lib
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -cp .:lib/* infrastructure.ApiGatewayApplication"]

# HEALTHCHECK: intenta /actuator/health y como fallback /
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:8086/actuator/health || curl -f http://localhost:8086/ || exit 1
