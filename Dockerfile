FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

RUN chmod +x ./gradlew

COPY src ./src

RUN ./gradlew clean bootJar --no-daemon -x test \
    && JAR_FILE=$(find build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*plain.jar" | head -n 1) \
    && cp "$JAR_FILE" /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy AS runner

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=docker

RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=builder /workspace/app.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
