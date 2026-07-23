@echo off
setlocal
rem LES-TECH-001 - para o container do PostgreSQL de validacao SEM apagar o volume.
rem Remocao de volume (dados) exige acao explicita e manual, nunca automatica.

cd /d "%~dp0..\.."

echo [parar] Verificando Docker...
docker version >nul 2>&1
if errorlevel 1 (
    echo [parar] ERRO: Docker nao esta disponivel ou o Docker Desktop nao esta em execucao.
    exit /b 1
)

echo [parar] Parando o container do PostgreSQL de validacao (container e volume preservados)...
docker compose stop
if errorlevel 1 (
    echo [parar] ERRO: falha ao executar 'docker compose stop'.
    exit /b 1
)

echo [parar] PostgreSQL de validacao parado.
echo [parar] Para remover o container (mantendo o volume): docker compose down
echo [parar] Para remover TAMBEM o volume e apagar os dados: docker compose down -v
endlocal
exit /b 0
