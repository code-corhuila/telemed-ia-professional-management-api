FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY professional-management-core ./professional-management-core
COPY professional-management-adapters ./professional-management-adapters
COPY professional-management-app ./professional-management-app

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring --create-home spring

COPY --from=build /workspace/professional-management-app/target/professional-management-api.jar /app/app.jar

EXPOSE 8080

USER spring:spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
