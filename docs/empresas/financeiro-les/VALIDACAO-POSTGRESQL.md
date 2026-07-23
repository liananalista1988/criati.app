# Validação PostgreSQL — LES-TECH-001

## Objetivo

Fornecer um ambiente PostgreSQL local reproduzível (Docker Compose) para validar as migrations Flyway
`V1` até `V12` fora do H2 usado pelos testes automatizados, e documentar o resultado real dessa validação
neste ambiente de execução.

**Esta tarefa não implementa nenhuma funcionalidade de negócio.** Nenhum arquivo em
`src/main/java/br/app/criati/financeiro/**`, `src/main/resources/templates/app/financeiro/**`,
`src/main/resources/static/js/**` ou qualquer migration (`V1` a `V12`) foi alterado. Nenhuma migration nova
(`V13+`) foi criada — `V13` está reservada para outro fluxo de trabalho.

## Diagnóstico de ambiente (Docker)

Verificado neste ambiente de execução, antes de qualquer configuração:

```cmd
docker version
docker compose version
```

**Resultado: Docker não está instalado neste ambiente** (`docker: command not found`, confirmado tanto via
Git Bash quanto via PowerShell — `Get-Command docker` não encontra o executável). Não há Docker Desktop,
`docker.exe` nem `docker compose` disponíveis. Por isso, **a validação real contra um PostgreSQL vivo não
foi executada nesta tarefa** — os arquivos abaixo foram criados e revisados com cuidado, mas **não foram
comprovadamente executados** neste ambiente. Isso é registrado aqui de forma explícita, conforme exigido:
não se afirma que o PostgreSQL foi validado quando não foi.

O que **foi** possível fazer sem Docker:

- Validar a sintaxe YAML de `compose.yaml` com um parser real (`npx js-yaml compose.yaml`, Node.js já
  disponível no ambiente) — parseou sem erros e produziu a estrutura esperada (um serviço
  `postgres-validacao`, um volume nomeado). Isso confirma sintaxe válida, **não** substitui
  `docker compose config` (que também valida contra o schema do Compose) nem uma subida real do container.
- Revisar cuidadosamente cada arquivo criado contra a documentação oficial do Compose e contra as migrations
  `V1`-`V12` já existentes no repositório (lidas diretamente para garantir nomes de coluna e tabela corretos
  nas consultas de diagnóstico).
- Executar `mvnw.cmd clean test` e `mvnw.cmd clean verify` normalmente (usam H2, não dependem de Docker).

## Pré-requisitos

- **Docker Desktop** (Windows): baixar em https://www.docker.com/products/docker-desktop/, instalar e
  iniciar. Requer WSL2 habilitado no Windows (o instalador orienta esse passo).
- Após instalar, confirmar com:
  ```cmd
  docker version
  docker compose version
  ```
- Java 17 e Maven Wrapper (`mvnw.cmd`) já usados pelo restante do projeto — nenhuma dependência nova.

## Arquivos desta tarefa

| Arquivo | Papel |
|---|---|
| `compose.yaml` | Sobe um PostgreSQL 16 (`postgres:16-alpine`) isolado, porta e banco próprios (não conflita com o PostgreSQL de desenvolvimento pessoal já mencionado no `README.md`). |
| `src/main/resources/application-postgresql-validation.properties` | Perfil Spring isolado: aponta para o banco do `compose.yaml`, `ddl-auto=validate` (nunca `create`), `flyway.enabled=true`, `flyway.clean-disabled=true`. |
| `scripts/postgresql/subir.cmd` | Sobe o container e aguarda o healthcheck ficar `healthy`. |
| `scripts/postgresql/parar.cmd` | Para o container (`docker compose stop`) **sem** remover volume. |
| `scripts/postgresql/validar.cmd` | Orquestra: sobe o banco → compila o JAR → inicia a aplicação com o perfil de validação (1ª execução, aplica `V1`-`V12`) → encerra → inicia de novo (2ª execução, confirma que nada é reaplicado) → encerra → roda `scripts/postgresql/verificacoes.sql` via `psql`. |
| `scripts/postgresql/verificacoes.sql` | Consultas de diagnóstico (tabelas, índices, constraints) e 4 testes negativos, cada um dentro de `BEGIN...ROLLBACK` — nenhuma linha de teste permanece no banco. |

## Configuração e variáveis de ambiente

Todas sobrescrevíveis por variável de ambiente antes de rodar `docker compose up` ou os scripts:

| Variável | Padrão | Descrição |
|---|---|---|
| `POSTGRES_VALIDACAO_DB` | `criati_validacao` | Nome do banco de validação. |
| `POSTGRES_VALIDACAO_USER` | `criati_validacao` | Usuário do banco de validação. |
| `POSTGRES_VALIDACAO_PASSWORD` | `senha_apenas_desenvolvimento_local` | Senha — **apenas desenvolvimento local**, nunca reutilizar. |
| `POSTGRES_VALIDACAO_PORT` | `5433` | Porta exposta no host (diferente de `5432` de propósito, para não colidir com um PostgreSQL local já instalado). |
| `POSTGRES_VALIDACAO_URL` | `jdbc:postgresql://localhost:5433/criati_validacao` | URL JDBC completa usada pelo perfil Spring; sobrescreva se mudar host/porta/banco. |

Nenhuma credencial real está commitada — os valores acima são todos placeholders de desenvolvimento local,
usados por um container Docker descartável.

## Como usar (quando Docker estiver disponível)

```cmd
rem 1. Subir o PostgreSQL de validacao e aguardar o healthcheck
scripts\postgresql\subir.cmd

rem 2. Rodar a validacao completa (compila, aplica migrations, reinicia, roda diagnosticos)
scripts\postgresql\validar.cmd

rem 3. Parar o container quando terminar (volume preservado)
scripts\postgresql\parar.cmd
```

Para inspecionar o banco manualmente a qualquer momento:

```cmd
docker compose exec postgres-validacao psql -U criati_validacao -d criati_validacao
```

Para consultar o histórico do Flyway diretamente:

```sql
SELECT version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank;
```

## Como limpar o banco de teste

- **Parar preservando dados**: `scripts\postgresql\parar.cmd` (equivalente a `docker compose stop`).
- **Remover o container preservando o volume**: `docker compose down`.
- **Remover também os dados (apagar o volume)** — ação explícita, nunca automática nesta tarefa:
  ```cmd
  docker compose down -v
  ```

## Validações obrigatórias — o que foi e o que não foi confirmado

| Item | Status neste ambiente |
|---|---|
| Subir PostgreSQL | **Não executado** — Docker indisponível. |
| Aguardar healthcheck | **Não executado**. |
| Aplicar `V1` a `V12` | **Não executado contra Postgres real.** Já confirmado indiretamente: os testes JPA do projeto (H2, schema gerado pelas anotações Hibernate das entidades) passam, e as migrations foram lidas e comparadas manualmente com as entidades — mas isso **não substitui** rodar o Flyway real contra Postgres. |
| Confirmar `flyway_schema_history` na versão `12` | **Não executado**. |
| Reiniciar e confirmar que nada é reaplicado | **Não executado**. |
| `mvnw.cmd clean test` / `clean verify` | **Executado normalmente** (não depende de Docker) — resultado na seção seguinte. |
| Confirmar criação do JAR | O `validar.cmd` inclui esse passo (`mvnw.cmd -DskipTests package`), mas não foi executado nesta tarefa por depender do fluxo completo do script, que depende de Docker. `mvnw.cmd clean verify` (executado) já empacota o JAR como parte do ciclo de vida padrão do Maven, o que **foi** confirmado. |
| Testes negativos de constraint (`verificacoes.sql`) | **Não executados contra Postgres real.** O script foi escrito com base na leitura direta de `V1` a `V12` (nomes de tabela/coluna conferidos manualmente), mas não foi rodado — pode conter erros não detectados sem uma execução real. |

## Resultado de `mvnw.cmd clean test` e `mvnw.cmd clean verify`

Ver números exatos no relatório final desta tarefa. Ambos executados neste worktree isolado
(`C:\Users\liann\workspace\criati-claude`, branch `feat/les-tech-001-postgresql`), sem nenhuma alteração de
código de produção — apenas confirmam que a base herdada de `feat/integracao-fundacao-dominio` (commit
`988a2e3`) continua íntegra.

## Problemas encontrados

Nenhum problema de migration foi encontrado, porque nenhuma migration foi de fato executada contra
PostgreSQL real nesta tarefa (bloqueio de ambiente, não um defeito descoberto). O único "achado" é o próprio
bloqueio de ambiente (Docker ausente), registrado em `docs/empresas/financeiro-les/PENDENCIAS.md`.

## Limitações

- Ambiente sem Docker: toda a cadeia `subir → validar → parar` é entregue mas não comprovada.
- `verificacoes.sql` não foi executado; seus 4 cenários de teste negativo (limite saudável > total, virtual
  sem principal, FK inexistente, duplicidade de ocorrência) foram escritos cuidadosamente a partir da leitura
  das migrations, mas apenas uma execução real pode confirmar que não há erros de sintaxe ou de referência.
- `validar.cmd` usa `findstr`/`start`/`taskkill` (batch puro do Windows) para orquestrar duas execuções da
  aplicação sem depender de ferramentas adicionais; não foi possível testar esse fluxo de fim a fim neste
  ambiente.
- Este documento não afirma, em nenhum ponto, que a validação real contra PostgreSQL foi concluída —
  apenas que a infraestrutura para fazê-la está pronta.

## Procedimento futuro para migrations novas

1. Nunca editar uma migration já commitada (`V1` a `V12` são definitivas).
2. Criar a próxima migration como `V13__descricao.sql` **apenas na tarefa/fluxo de trabalho responsável**
   (fora do escopo desta tarefa — `V13` está reservada).
3. Antes de commitar uma nova migration, rodar `scripts\postgresql\validar.cmd` (com Docker disponível) para
   confirmar que ela aplica corretamente em um banco vazio e que a reinicialização não a reaplica.
4. Adicionar ao final de `scripts/postgresql/verificacoes.sql` (ou a um arquivo irmão) as consultas de
   diagnóstico específicas da nova migration, seguindo o mesmo padrão `BEGIN...ROLLBACK` para testes
   negativos.
5. Registrar em `docs/DECISOES.md`/`IMPLEMENTACAO-*.md` da tarefa correspondente qualquer decisão de schema,
   como já é feito para as migrations existentes.
