FROM eclipse-temurin:21-jdk

LABEL description="mobile-service-2"

ADD target/mobile-service-*.jar /opt/mobile-service.jar

# Instala fontes necessárias para geração do PDF do JasperReports
RUN apt-get update && apt-get install -y fontconfig libfreetype6

ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "/opt/mobile-service.jar"]

EXPOSE 8086
