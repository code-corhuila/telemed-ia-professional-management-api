FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring --create-home spring

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

USER spring:spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
