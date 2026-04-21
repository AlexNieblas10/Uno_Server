# Multi-stage build for smaller final image
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar y construir shared library
COPY uno-shared /app/uno-shared
WORKDIR /app/uno-shared
RUN mvn clean install -DskipTests

# Copiar y construir servidor
WORKDIR /app
COPY uno-server /app/uno-server
WORKDIR /app/uno-server
RUN mvn clean package -DskipTests

# Runtime stage - imagen más pequeña
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar JAR compilado desde build stage
COPY --from=build /app/uno-server/target/uno-server-1.0-SNAPSHOT.jar /app/server.jar

# Exponer puerto (Railway lo configura dinámicamente)
EXPOSE ${PORT:-12345}

# Ejecutar servidor
# Railway inyecta la variable PORT automáticamente
CMD ["sh", "-c", "java -jar server.jar"]
