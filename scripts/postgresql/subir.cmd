@echo off
setlocal enabledelayedexpansion
rem LES-TECH-001 - sobe o PostgreSQL de validacao (compose.yaml) e aguarda o healthcheck.
rem Nao apaga volumes. Nao usa credenciais reais.

cd /d "%~dp0..\.."

echo [subir] Verificando Docker...
docker version >nul 2>&1
if errorlevel 1 (
    echo [subir] ERRO: Docker nao esta disponivel ou o Docker Desktop nao esta em execucao.
    echo [subir] Instale/inicie o Docker Desktop. Veja docs\empresas\financeiro-les\VALIDACAO-POSTGRESQL.md
    exit /b 1
)

docker compose version >nul 2>&1
if errorlevel 1 (
    echo [subir] ERRO: 'docker compose' nao esta disponivel nesta instalacao do Docker.
    exit /b 1
)

echo [subir] Subindo PostgreSQL de validacao (compose.yaml)...
docker compose up -d
if errorlevel 1 (
    echo [subir] ERRO: falha ao executar 'docker compose up -d'.
    exit /b 1
)

echo [subir] Aguardando o healthcheck do PostgreSQL de validacao...
set /a TENTATIVAS=0

:esperar
set /a TENTATIVAS+=1
set "STATUS="
for /f "tokens=* usebackq" %%S in (`docker compose ps --format "{{.Health}}" postgres-validacao 2^>nul`) do set "STATUS=%%S"

if "!STATUS!"=="healthy" goto pronto

if !TENTATIVAS! GEQ 30 (
    echo [subir] ERRO: tempo limite excedido aguardando o healthcheck ^(status atual: "!STATUS!"^).
    echo [subir] Verifique os logs com: docker compose logs postgres-validacao
    exit /b 1
)

timeout /t 2 >nul
goto esperar

:pronto
echo [subir] PostgreSQL de validacao pronto (healthy).
docker compose ps postgres-validacao
endlocal
exit /b 0
