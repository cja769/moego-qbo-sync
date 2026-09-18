# syntax=docker/dockerfile:1.7
FROM maven:3.9.11-eclipse-temurin-17 AS core
WORKDIR /core
COPY --from=qbo-core pom.xml .
COPY --from=qbo-core src src
RUN mvn --batch-mode -DskipTests install

FROM maven:3.9.11-eclipse-temurin-17 AS build
COPY --from=core /root/.m2 /root/.m2
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn --batch-mode package

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 app
USER app
COPY --from=build /app/target/moego-qbo-sync-*.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
