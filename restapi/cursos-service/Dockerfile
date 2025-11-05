FROM openjdk:21-jdk-slim

LABEL description="cursos-api"

ADD target/cursos-service-*.jar /opt/cursos-api.jar

ENTRYPOINT ["java", "-jar", "/opt/cursos-api.jar"]

EXPOSE 8080 9443
