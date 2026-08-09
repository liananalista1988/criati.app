# Validações reais de migration contra PostgreSQL

Registro append-only de evidências objetivas de validação de migrations Flyway
contra PostgreSQL real (nunca H2), conforme exigido por
[`CRIATI_PROTOCOL.md`](CRIATI_PROTOCOL.md), Camada 1: "Havendo migration,
valide V1..Vn em PostgreSQL real e banco confirmadamente seguro". Cada
entrada é de uma tarefa específica; entradas antigas não são reescritas, só
acrescentadas. Nunca registrar senha, token, segredo ou connection string com
credencial — apenas nome do banco/perfil, comandos/consultas e resultados
objetivos.

## CRIATI-IMP-FIX-009 — 2026-08-09

**Contexto:** evidência objetiva de `V26` (`permite conta opcional
importacao bancaria`, introduzida na `CRIATI-IMP-FEAT-004`), solicitada
explicitamente pela `CRIATI-IMP-AUDIT-008` após a auditoria anterior não
conseguir comprová-la localmente. Nenhuma migration nova foi criada ou
alterada nesta tarefa.

**PostgreSQL utilizado:** PostgreSQL 18.4, serviço nativo Windows
(`postgresql-x64-18`), `127.0.0.1:5432`.

**Banco descartável utilizado:** `criati_validacao_v25` — banco e usuário
exclusivos de validação técnica, criados manualmente pelo usuário fora do
fluxo Docker Compose oficial (Docker indisponível neste ambiente), nunca
usados como ambiente de desenvolvimento e confirmados vazios antes do
primeiro uso. Reaproveitado entre sessões desta mesma tarefa/branch,
conforme já registrado em sessões anteriores desta mesma iniciativa.

**Perfil Spring usado:** `postgresql-validation`
(`src/main/resources/application-postgresql-validation.properties`),
via `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgresql-validation`.
Credenciais fornecidas somente como variável de ambiente
(`POSTGRES_VALIDACAO_URL`/`POSTGRES_VALIDACAO_USER`/
`POSTGRES_VALIDACAO_PASSWORD`) na sessão do operador — nunca gravadas em
arquivo, log ou neste documento.

**Consulta executada:**

```sql
select installed_rank, version, description, success,
       checksum is not null as tem_checksum, installed_on
from flyway_schema_history
where version::int >= 25
order by installed_rank;
```

**Resultado objetivo:**

| installed_rank | version | description | success | tem_checksum | installed_on |
|---|---|---|---|---|---|
| 25 | 25 | criar processos tarefas empresariais | t | t | 2026-08-08 19:34:33 |
| 26 | 26 | permite conta opcional importacao bancaria | t | t | 2026-08-08 23:00:19 |

**Idempotência:** aplicação iniciada duas vezes consecutivas nesta tarefa
com o mesmo perfil. Ambas as execuções registraram:

```
Current version of schema "public": 26
Schema "public" is up to date. No migration necessary.
```

Nenhuma migration foi reaplicada em nenhuma das duas execuções.

**Hibernate `spring.jpa.hibernate.ddl-auto=validate`:** sem
`SchemaManagementException` em nenhuma das duas execuções; `Started
CriatiApplication` concluído com sucesso nas duas vezes (~15s cada).

**Suíte completa (H2, `mvnw.cmd test`):** `925 testes, 0 falhas, 0 erros,
0 ignorados`, `BUILD SUCCESS`, ~10min25s.

**Testes focados backend/MVC nesta tarefa:** `121 testes, 0 falhas, 0
erros` (parsers OFX/CSV/XLSX, controllers de importação/conta/regras,
lock pessimista, `PaginaFinanceiroSegurancaTests`).
