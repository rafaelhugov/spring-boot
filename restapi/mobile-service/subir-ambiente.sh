#!/bin/bash

# Configurações de cores para logs organizados
COLOR_RESET="\e[0m"
COLOR_GREEN="\e[32m"
COLOR_YELLOW="\e[33m"
COLOR_RED="\e[31m"
COLOR_CYAN="\e[36m"

# Tenta configurar o Java 21 (SDKMAN ou JVM local)
if [ -d "$HOME/.sdkman/candidates/java/21.0.11-tem" ]; then
    export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.11-tem"
    export PATH=$JAVA_HOME/bin:$PATH
elif [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
    export PATH=$JAVA_HOME/bin:$PATH
fi

# Tenta configurar o NVM para carregar o Node.js e NPM
export NVM_DIR="$HOME/.nvm"
if [ -s "$NVM_DIR/nvm.sh" ]; then
    . "$NVM_DIR/nvm.sh"
fi

echo -e "${COLOR_CYAN}=== Iniciando Ambiente de Desenvolvimento: Aplicativo MPPB ===${COLOR_RESET}"

# Caminhos dos projetos relativos ao script
DIR_SCRIPT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
DIR_BACKEND_ROOT="$( cd "$DIR_SCRIPT/.." && pwd )"
DIR_FRONTEND="/home/rafaelhugo/projetosMPPB/mppb-vue/mobile-web"

# 0. Liberar portas do Backend (8086) se estiverem em uso para evitar "Endereço já em uso"
echo -e "${COLOR_YELLOW}--- 0. Verificando e liberando portas do sistema ---${COLOR_RESET}"
PORT_PID=$(lsof -t -i:8086)
if [ -n "$PORT_PID" ]; then
    echo -e "${COLOR_YELLOW}Finalizando processo antigo rodando na porta 8086 (PID: $PORT_PID)...${COLOR_RESET}"
    kill -9 $PORT_PID 2>/dev/null
    sleep 1
fi

# 1. Compilação do Backend com dependências
echo -e "\n${COLOR_YELLOW}--- 1. Compilando Backend (mobile-service e dependências) ---${COLOR_RESET}"
cd "$DIR_BACKEND_ROOT" || { echo -e "${COLOR_RED}Diretório raiz do restapi não encontrado.${COLOR_RESET}"; exit 1; }

# Compila parent, common e mobile-service
mvn install -N -Dmaven.test.skip=true
mvn install -f restapi-common/pom.xml -Dmaven.test.skip=true
mvn clean package -f mobile-service/pom.xml -Dmaven.test.skip=true

if [ $? -ne 0 ]; then
    echo -e "${COLOR_RED}Erro na compilação do Backend. Abortando inicialização.${COLOR_RESET}"
    exit 1
fi
echo -e "${COLOR_GREEN}Backend compilado com sucesso!${COLOR_RESET}"

# 2. Inicialização em paralelo
echo -e "\n${COLOR_YELLOW}--- 2. Iniciando Backend e Frontend em Paralelo ---${COLOR_RESET}"

ENV_FILE="$DIR_SCRIPT/.env.dev"
if [ -f "$ENV_FILE" ]; then
    echo -e "${COLOR_CYAN}Carregando variáveis locais de $ENV_FILE...${COLOR_RESET}"
    set -a
    . "$ENV_FILE"
    set +a

    if [ -n "$PG_URL" ]; then
        export SPRING_DATASOURCE_URL="jdbc:$PG_URL"
    fi
fi

# Configurando Túnel Cloudflare para o Google Wallet
CLOUDFLARED_CMD=""
if [ -f "/tmp/cloudflared" ]; then
    CLOUDFLARED_CMD="/tmp/cloudflared"
elif command -v cloudflared &> /dev/null; then
    CLOUDFLARED_CMD="cloudflared"
fi

TUNNEL_PID=""
if [ -n "$CLOUDFLARED_CMD" ]; then
    echo -e "${COLOR_YELLOW}--- Configurando Túnel Cloudflare para o Google Wallet ---${COLOR_RESET}"
    OLD_TUNNEL_PIDS=$(pgrep -f "cloudflared tunnel")
    if [ -n "$OLD_TUNNEL_PIDS" ]; then
        echo -e "${COLOR_CYAN}Finalizando instâncias antigas de túneis Cloudflare...${COLOR_RESET}"
        kill -9 $OLD_TUNNEL_PIDS 2>/dev/null
    fi
    
    rm -f /tmp/cloudflared.log
    $CLOUDFLARED_CMD tunnel --url http://localhost:8086 > /tmp/cloudflared.log 2>&1 &
    TUNNEL_PID=$!
    
    echo -e "${COLOR_CYAN}Aguardando obtenção da URL pública do túnel...${COLOR_RESET}"
    for i in {1..8}; do
        sleep 1.5
        PUBLIC_URL=$(grep -oE 'https://[a-zA-Z0-9.-]+\.trycloudflare\.com' /tmp/cloudflared.log | head -n 1)
        if [ -n "$PUBLIC_URL" ]; then
            break
        fi
    done

    if [ -n "$PUBLIC_URL" ]; then
        echo -e "${COLOR_GREEN}Túnel estabelecido com sucesso! URL: ${PUBLIC_URL}${COLOR_RESET}"
        export GOOGLE_WALLET_PUBLIC_BASE_URL="$PUBLIC_URL"
        export GOOGLE_WALLET_ORIGINS="http://localhost:9003,http://localhost:9004,http://localhost:8086,${PUBLIC_URL}"
    else
        echo -e "${COLOR_RED}Aviso: Não foi possível obter a URL do túnel Cloudflare. As imagens locais não carregarão no Google Wallet.${COLOR_RESET}"
    fi
fi

# Função para garantir o encerramento limpo de todos os processos filhos (Ctrl+C)
cleanup() {
    echo -e "\n${COLOR_YELLOW}--- Finalizando processos do Backend e Frontend... ---${COLOR_RESET}"
    
    if [ -n "$BACKEND_PID" ]; then
        kill "$BACKEND_PID" 2>/dev/null
    fi
    if [ -n "$FRONTEND_PID" ]; then
        kill "$FRONTEND_PID" 2>/dev/null
    fi
    if [ -n "$TUNNEL_PID" ]; then
        kill "$TUNNEL_PID" 2>/dev/null
    fi
    
    echo -e "${COLOR_GREEN}Ambiente encerrado com sucesso.${COLOR_RESET}"
    exit 0
}

# Captura sinais de interrupção (Ctrl+C, etc) e término do script
trap cleanup SIGINT SIGTERM EXIT

# Iniciar o Backend
echo -e "${COLOR_CYAN}Iniciando Backend (Porta 8086)...${COLOR_RESET}"
cd "$DIR_SCRIPT"
java -jar target/mobile-service-*.jar &
BACKEND_PID=$!

# Iniciar o Frontend
echo -e "${COLOR_CYAN}Iniciando Frontend (Quasar Dev)...${COLOR_RESET}"
cd "$DIR_FRONTEND" || { echo -e "${COLOR_RED}Diretório do frontend não encontrado.${COLOR_RESET}"; kill "$BACKEND_PID"; exit 1; }

# Rodar o Quasar Dev em paralelo
npm run dev &
FRONTEND_PID=$!

# Aguarda que ambos os processos finalizem (ou até o usuário pressionar Ctrl+C)
wait
