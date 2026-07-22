# Implementação LES-F2-005 — Lançamentos financeiros básicos

## Diagnóstico e reaproveitamento

O Financeiro já possuía `LancamentoFinanceiro`, tabela `lancamento_financeiro`, repository,
service, saldo derivado, dashboard, API `/api/contexto/financeiro/lancamentos`, página
`/app/financeiro/lancamentos` e JavaScript. A entrega evolui esses elementos, preservando UUIDs,
rotas, lançamentos legados e os aliases `PAGO`, `dataPagamento`, `/pagar` e `/reabrir`. Não foi
criada entidade, tabela, página ou cálculo de saldo concorrente.

Antes da V9, o lançamento já possuía empresa, conta, categoria, tipo, descrição, valor,
`dataCompetencia`, `dataPagamento`, status, observação e datas. Conta e categoria já eram
tenant-aware e o saldo já era calculado como saldo inicial mais receitas pagas menos despesas
pagas. Não existiam pessoa responsável, contato, vencimento, forma, origem ou autores.

## Entidade, tipos e datas

`LancamentoFinanceiro` continua representando somente `RECEITA` ou `DESPESA`, sempre com valor
positivo `BigDecimal/NUMERIC(19,2)`; o tipo define o sinal. Transferência, cartão, fatura e parcela
não foram antecipados.

Os novos status são `PENDENTE`, `LIQUIDADO` e `CANCELADO`. `PAGO` permanece aceito exclusivamente
para compatibilidade de dados e APIs anteriores e tem o mesmo efeito de saldo de `LIQUIDADO`.
`VENCIDO` não é persistido: é calculado quando o lançamento está pendente, possui vencimento e a
data já passou.

- competência permanece armazenada na data representativa já existente; filtros e resumos usam
  seu `YearMonth`, com apoio do conceito `CompetenciaFinanceira` já documentado;
- vencimento é opcional e nunca altera saldo;
- liquidação reutiliza a coluna `data_pagamento`, preservando dados, e é exposta também como
  `dataLiquidacao`; somente lançamento liquidado/pago possui essa data e impacta saldo;
- liquidação pode ocorrer antes ou depois do vencimento.

## Relacionamentos e regras

- A conta permanece obrigatória também no pendente. Isso preserva o contrato atual, determina o
  saldo que será afetado e evita compromisso sem destino financeiro.
- Pessoa responsável é exigida pela interface e inferida do titular da conta quando omitida. A
  referência permanece anulável no banco e no contrato legado para lançamentos/contas anteriores
  à V6/V7 que ainda não possuem titular.
- Parte financeira (contato) é opcional e distinta da pessoa responsável.
- Conta, categoria, pessoa e parte são sempre localizadas por `id + empresa`. Novas referências
  devem estar ativas; uma referência inativa já existente pode permanecer no histórico.
- Categoria deve ter a mesma natureza do lançamento.
- Descrição é normalizada sem alterar caixa, valor usa `HALF_UP`, observação é opcional e origem
  produzida nesta etapa é sempre `MANUAL`.
- Forma é opcional: `PIX`, `DEBITO`, `DINHEIRO`, `BOLETO`, `TRANSFERENCIA` ou `OUTRA`. `CREDITO`
  fica aceito somente como valor legado, sem integração com cartões/faturas.

## Saldo, transições e auditoria

O saldo não é persistido nem incrementado. A fonte única continua sendo `SaldoFinanceiroService`:

```text
saldo atual = saldo inicial + receitas LIQUIDADO/PAGO - despesas LIQUIDADO/PAGO
```

Pendentes e cancelados não impactam. Editar valor, tipo ou conta de um liquidado recalcula
naturalmente a consulta, sem compensação duplicada. `liquidar` inclui o impacto;
`desliquidar` limpa a data e remove o impacto; cancelar pendente não afeta; cancelar liquidado
limpa a liquidação e remove o impacto na mesma transação. Não existe liquidação parcial nem
sistema contábil paralelo de estornos.

Gestor e administrador podem criar, editar, liquidar e cancelar, preservando a autorização
anterior. Desliquidação/reabertura permanece restrita ao administrador por desfazer movimento já
realizado. Criação e toda alteração registram usuário e instante atuais. Não há histórico
versionado campo a campo; a limitação foi preservada sem criar auditoria paralela.

## Migration V9

`V9__evoluir_lancamentos_financeiros.sql` altera a tabela existente e adiciona pessoa, parte,
vencimento, forma, origem e autores. Também amplia a constraint de status, acrescenta coerência
entre status e liquidação, FKs e índices por empresa/pessoa/parte/vencimento/liquidação. Registros
anteriores recebem origem `MANUAL`; pessoa, parte e autores permanecem anuláveis apenas para
compatibilidade histórica. Nenhum dado real é inserido.

## Repositories, APIs e resumo

O repository oferece consultas tenant-aware por período, conta, categoria, pessoa, parte, tipo,
status, descrição e vencidos, além de somas por natureza/status e impacto por conta.

- `GET /api/contexto/financeiro/lancamentos` — filtros existentes mais `pessoaId`, `parteId` e
  `vencido`;
- `GET /api/contexto/financeiro/lancamentos/{id}`;
- `POST /api/contexto/financeiro/lancamentos`;
- `PUT /api/contexto/financeiro/lancamentos/{id}`;
- `POST /api/contexto/financeiro/lancamentos/{id}/liquidar`;
- `POST /api/contexto/financeiro/lancamentos/{id}/desliquidar`;
- `POST .../pagar` e `POST .../reabrir` — aliases legados preservados;
- `POST /api/contexto/financeiro/lancamentos/{id}/cancelar`;
- `GET /api/contexto/financeiro/lancamentos/resumo?competencia=AAAA-MM` — aceita filtros de pessoa
  e conta.

O resumo informa receitas/despesas liquidadas, resultado liquidado, receitas/despesas pendentes,
quantidade de vencidos, saldos atuais por conta e saldo consolidado. Filtro de pessoa restringe
os lançamentos e resultados do mês; saldo atual continua sendo o saldo real da conta, pois não é
um rateio por pessoa.

## Interface, segurança e testes

A página existente foi evoluída com pessoa, contato, vencidos, busca e filtros anteriores; cards
do resumo; tabela com competência/vencimento/liquidação; formulário completo; e ações explícitas
de liquidar, desliquidar e cancelar. Os textos explicam as três datas. Estados vazio, carregando,
erro, sucesso, vencido, pendente, liquidado e cancelado são tratados. Tema, estilos globais e
responsividade continuam fornecidos pelo layout Criati.

Todas as rotas exigem autenticação, empresa ativa, vínculo e Financeiro habilitado. Escritas
exigem perfil permitido e CSRF. Nenhum DTO aceita `empresaId`; identificadores manipulados de
outro tenant retornam recurso não encontrado sem revelar dados.

Testes JPA/MockMvc cobrem compatibilidade anterior, receitas/despesas pendentes e liquidadas,
valor, competência, categoria, conta, pessoa, parte, tenant, saldo inicial, liquidação,
desliquidação, cancelamento, edição de liquidado, ausência de duplicidade, vencido, filtros,
resumo, autenticação, autorização, CSRF, página e dashboard.

## Limitações e próxima etapa

- Pessoa nula continua possível somente para dados legados sem titular regularizado.
- `PAGO`, `dataPagamento`, `/pagar` e `/reabrir` permanecem temporariamente por compatibilidade.
- Não há auditoria versionada, liquidação parcial, estorno contábil separado ou competência
  armazenada diretamente como `YearMonth`.
- Não foram implementados cartão, fatura, parcela, recorrência, contas a pagar avançadas,
  conciliação, importação, orçamento, empréstimos, simulador ou notificações.
- A validação visual completa depende de navegador controlável no ambiente.

A base passa a oferecer fatos e compromissos simples com impacto de caixa confiável para as
próximas tarefas, sem misturar operações de cartão ou transferências futuras.
