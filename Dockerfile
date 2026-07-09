# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache the dependency tree separately from the sources
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
# Tests need a local MySQL, so they run outside the image build
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --home /app app \
    && mkdir /app/uploads \
    && chown -R app:app /app

COPY --from=build /build/target/*.jar app.jar

USER app
EXPOSE 8081

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
