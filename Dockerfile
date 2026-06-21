FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 smartcloset \
    && useradd --system --uid 10001 --gid smartcloset --home-dir /app --shell /usr/sbin/nologin smartcloset \
    && mkdir -p /data/smartcloset/clothing-images \
    && chown -R smartcloset:smartcloset /app /data/smartcloset

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0" \
    SMARTCLOSET_HEALTHCHECK_URL="http://127.0.0.1:8080/actuator/health"

COPY --from=build --chown=smartcloset:smartcloset /workspace/build/libs/*.jar app.jar

EXPOSE 8080

USER 10001:10001

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS "$SMARTCLOSET_HEALTHCHECK_URL" | grep -q '"status":"UP"'

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
