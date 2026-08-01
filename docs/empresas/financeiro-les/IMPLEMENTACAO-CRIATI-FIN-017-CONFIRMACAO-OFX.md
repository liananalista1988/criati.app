# CRIATI-FIN-017 — Confirmação controlada de transações OFX

## Resultado

A prévia OFX permanece sem confirmação automática. Transações pendentes podem ser confirmadas em lote ou
ignoradas individualmente. Cada confirmação cria um único `LancamentoFinanceiro` liquidado, com origem
`IMPORTACAO`, data da transação e conta do lote. Valores positivos geram receita; valores negativos geram
despesa pelo valor absoluto.

## API protegida

- `GET /api/contexto/financeiro/importacoes-bancarias/{loteId}/pendencias`
- `POST /api/contexto/financeiro/importacoes-bancarias/{loteId}/confirmacoes`
- `POST /api/contexto/financeiro/importacoes-bancarias/{loteId}/transacoes/{transacaoId}/ignorar`
- `GET /api/contexto/financeiro/importacoes-bancarias/{loteId}/resumo`

A confirmação exige categoria ativa, do mesmo tenant e do mesmo tipo financeiro. Duplicidades sinalizadas
exigem `confirmarDuplicidade=true`. Escritas permanecem restritas a administrador ou gestor e protegidas por
CSRF; consultas derivam a empresa do contexto autenticado.

## Idempotência e concorrência

O serviço bloqueia primeiro o lote e depois as transações com `PESSIMISTIC_WRITE`. Reenvios de confirmação
já concluída retornam o vínculo existente. A V21 adiciona unicidade do lançamento por transação, FK composta
por empresa, constraints de coerência do estado e auditoria de confirmação/ignoração.

## Migration

`V21__confirmar_transacoes_importadas.sql` adiciona a situação `PENDENTE`, `CONFIRMADA` ou `IGNORADA`, o
vínculo com o lançamento, autor e instante da decisão, além do índice por empresa, lote, situação e sequência.
Foi validada com Flyway V1–V21 em banco vazio no PostgreSQL 18.4 e com Hibernate `ddl-auto=validate`.
