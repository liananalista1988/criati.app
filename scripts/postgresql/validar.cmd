@echo off
setlocal enabledelayedexpansion
rem LES-TECH-001 - valida as migrations Flyway V1-V12 contra um PostgreSQL real.
rem Fluxo: sobe o banco -> compila -> sobe a aplicacao 1a vez (aplica migrations)
rem -> encerra -> sobe a aplicacao 2a vez (confirma que nada e reaplicado) ->
rem encerra -> roda consultas de diagnostico via psql.
rem Nao insere dados reais. Nao apaga volumes. Nao cria migration nova.

cd /d "%~dp0..\.."
if not exist target mkdir target

set "LOG1=target\postgresql-validacao-1a-execucao.log"
set "LOG2=target\postgresql-validacao-2a-execucao.log"
set "TITULO=criati-postgresql-validacao"

echo [validar] ================================================================
echo [validar] LES-TECH-001: validacao de migrations contra PostgreSQL real
echo [validar] ================================================================

call "%~dp0subir.cmd"
if errorlevel 1 (
    echo [validar] ERRO: nao foi possivel subir o PostgreSQL de validacao. Abortando.
    exit /b 1
)

echo [validar] Compilando o projeto (empacotando o JAR, sem executar os testes aqui)...
call mvnw.cmd -q -DskipTests package
if errorlevel 1 (
    echo [validar] ERRO: falha ao compilar/empacotar o projeto.
    exit /b 1
)
echo [validar] JAR gerado com sucesso em target\*.jar.

echo [validar] ----------------------------------------------------------------
echo [validar] 1a execucao: espera-se que o Flyway aplique V1 ate V12.
echo [validar] ----------------------------------------------------------------
if exist "%LOG1%" del /q "%LOG1%"
start "%TITULO%" cmd /c "mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgresql-validation > "%LOG1%" 2>&1"
call :esperar_aplicacao "%LOG1%"
if errorlevel 1 (
    echo [validar] ERRO: a 1a execucao falhou ao iniciar. Veja %LOG1%.
    call :encerrar_aplicacao
    exit /b 1
)
echo [validar] 1a execucao OK: aplicacao iniciou, Flyway aplicou as migrations e o Hibernate validou o schema.
call :encerrar_aplicacao
timeout /t 3 >nul

echo [validar] ----------------------------------------------------------------
echo [validar] 2a execucao: espera-se que nenhuma migration seja reaplicada.
echo [validar] ----------------------------------------------------------------
if exist "%LOG2%" del /q "%LOG2%"
start "%TITULO%" cmd /c "mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgresql-validation > "%LOG2%" 2>&1"
call :esperar_aplicacao "%LOG2%"
if errorlevel 1 (
    echo [validar] ERRO: a 2a execucao falhou ao iniciar. Veja %LOG2%.
    call :encerrar_aplicacao
    exit /b 1
)
echo [validar] 2a execucao OK: reinicializacao nao reaplicou nenhuma migration.
call :encerrar_aplicacao

echo [validar] ----------------------------------------------------------------
echo [validar] Executando consultas de diagnostico (tabelas, indices, constraints,
echo [validar] testes negativos sempre desfeitos com ROLLBACK)...
echo [validar] ----------------------------------------------------------------
docker compose exec -T postgres-validacao psql -U criati_validacao -d criati_validacao ^
    < scripts\postgresql\verificacoes.sql > target\postgresql-validacao-verificacoes.log 2>&1
if errorlevel 1 (
    echo [validar] AVISO: 'docker compose exec' retornou erro. Veja target\postgresql-validacao-verificacoes.log.
) else (
    echo [validar] Consultas de diagnostico concluidas. Veja target\postgresql-validacao-verificacoes.log.
)

echo [validar] ================================================================
echo [validar] Validacao concluida. Logs em target\postgresql-validacao-*.log
echo [validar] O container do PostgreSQL de validacao continua rodando.
echo [validar] Use scripts\postgresql\parar.cmd para para-lo quando terminar.
echo [validar] ================================================================
endlocal
exit /b 0

:esperar_aplicacao
set "ARQUIVO_LOG=%~1"
set /a TENTATIVAS=0
:esperar_loop
set /a TENTATIVAS+=1
findstr /c:"Started CriatiApplication" "%ARQUIVO_LOG%" >nul 2>&1
if not errorlevel 1 exit /b 0
findstr /c:"APPLICATION FAILED TO START" "%ARQUIVO_LOG%" >nul 2>&1
if not errorlevel 1 exit /b 1
findstr /i /c:"FlywayException" "%ARQUIVO_LOG%" >nul 2>&1
if not errorlevel 1 exit /b 1
findstr /i /c:"SchemaManagementException" "%ARQUIVO_LOG%" >nul 2>&1
if not errorlevel 1 exit /b 1
if !TENTATIVAS! GEQ 60 exit /b 1
timeout /t 2 >nul
goto esperar_loop

:encerrar_aplicacao
taskkill /fi "WINDOWTITLE eq %TITULO%*" /t /f >nul 2>&1
exit /b 0
