#FROM openjdk:21-jdk-slim
FROM eclipse-temurin:21-jdk

LABEL description="cobranca-api"

RUN apt-get update && apt-get install -y \
    fontconfig \
    libfreetype6 \
    libx11-6 \
    libxext6 \
    libxrender1 \
    fonts-dejavu && \
    rm -rf /var/lib/apt/lists/*

ADD target/cobranca-service-*.jar /opt/cobranca-service.jar

ENTRYPOINT ["java", "-jar", "/opt/cobranca-service.jar"]

EXPOSE 8080 9443
