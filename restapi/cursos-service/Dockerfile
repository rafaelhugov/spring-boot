# Stage 1: Build com Maven + Java 21
FROM maven:3.9-eclipse-temurin-21 AS build

ARG http_proxy
ARG https_proxy
ARG no_proxy=localhost,127.0.0.1

ENV http_proxy=$http_proxy
ENV https_proxy=$https_proxy
ENV no_proxy=$no_proxy

WORKDIR /app

# Copia o pom.xml primeiro para aproveitar cache de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código-fonte e compila
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -B

# Stage 2: Runtime leve com JRE
FROM eclipse-temurin:21-jre-alpine

LABEL description="cursos-api"

WORKDIR /opt

COPY --from=build /app/target/cursos-service-*.jar cursos-api.jar

EXPOSE 9002

ENTRYPOINT ["java", "-jar", "cursos-api.jar"]
