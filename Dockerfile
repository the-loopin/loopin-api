# syntax=docker/dockerfile:1.7

FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S loopin && adduser -S loopin -G loopin
COPY --from=build /workspace/target/*.jar /app/loopin-api.jar

ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=production \
    JAVA_OPTS=""

EXPOSE 8080
USER loopin
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/loopin-api.jar"]
