FROM openjdk:21-jdk-slim

LABEL description="cobranca-api"

RUN apt-get update && apt-get install -y \
    fontconfig \
    libfreetype6 \
    libx11-6 \
    libxext6 \
    libxrender1 \
    fonts-dejavu && \
    rm -rf /var/lib/apt/lists/*

ADD target/cobranca-api-*.jar /opt/cobranca-api.jar

ENTRYPOINT ["java", "-jar", "/opt/cobranca-api.jar"]

EXPOSE 8080 9443
