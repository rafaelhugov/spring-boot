FROM openjdk:21-jdk-slim

LABEL description="cursos-api"

# Instalação das dependências de fontes (exatamente como na referência)
RUN apt-get update && apt-get install -y \
    fontconfig \
    libfreetype6 \
    libx11-6 \
    libxext6 \
    libxrender1 \
    fonts-dejavu && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /opt

# O padrão espera o JAR dentro da pasta target/ (build externo via Maven)
# O asterisco captura o JAR versionado gerado pelo maven package
ADD target/cursos-service-*.jar /opt/cursos-api.jar

# Porta de Produção (8080 conforme referência)
EXPOSE 8080 9443

ENTRYPOINT ["java", "-jar", "/opt/cursos-api.jar"]
