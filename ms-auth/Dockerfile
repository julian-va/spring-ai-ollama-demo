# Dockerfile optimizado para ms-auth (multistage, cache-friendly)
# Stage 1: build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Variables para mejora de caching
ENV GRADLE_USER_HOME=/workspace/.gradle
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

# 1) Copiamos solo lo necesario para resolver dependencias y configurar gradle
# Esto ayuda a que la capa que descarga dependencias se cachee mientras no cambien los scripts de build
COPY gradlew gradlew.bat ./
COPY gradle/ ./gradle/
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY buildSrc/ ./buildSrc/
# Copiamos el build.gradle del submódulo para que gradle calcule dependencias del subproyecto
COPY ms-auth/build.gradle.kts ./ms-auth/

# Permisos del wrapper
RUN chmod +x ./gradlew

# Descargamos dependencias (no construimos el jar completo aún)
RUN ./gradlew :ms-auth:assemble --no-daemon -x test --parallel

# 2) Ahora copiamos el código fuente del módulo
COPY ms-auth/src ./ms-auth/src
COPY ms-auth/src/main/resources ./ms-auth/src/main/resources

# Construimos el jar final (bootJar)
RUN ./gradlew :ms-auth:bootJar --no-daemon -x test --parallel

# Copiamos el jar a una ruta fija dentro de la etapa de build
RUN mkdir -p /artifact \
    && cp ms-auth/build/libs/*.jar /artifact/ \
    && latest=$(ls -t /artifact/*.jar | head -n1) \
    && mv "$latest" /artifact/app.jar

# Stage 2: runtime (ligero)
FROM eclipse-temurin:21-jre-jammy AS runtime
ARG APP_PORT=8082
ENV APP_PORT=${APP_PORT}
EXPOSE ${APP_PORT}

# Instalar curl para el healthcheck (se limpia la cache de apt para no dejar archivos temporales)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# HEALTHCHECK: verifica el endpoint /actuator/health (espera 200)
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -fsS http://localhost:${APP_PORT}/actuator/health || exit 1

# Crear usuario no privilegiado
RUN useradd --create-home --shell /bin/bash appuser
WORKDIR /home/appuser

# Copiamos el jar desde la etapa de build
COPY --from=build /artifact/app.jar ./app.jar
RUN chown appuser:appuser ./app.jar
USER appuser

# JVM options: se pueden sobrescribir con la variable de entorno JAVA_OPTS
ENV JAVA_OPTS="-Xms128m -Xmx512m"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /home/appuser/app.jar"]
