# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline          # capa cacheable de dependencias
COPY src ./src
RUN mvn -B -q package -DskipTests            # los tests ya corrieron en CI

# ---------- Etapa 2: imagen final, solo JRE ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/pokedex-api-*.jar app.jar
USER app
# Cloud Run inyecta PORT; Spring lo lee en application.yml (server.port: ${PORT:8080})
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
