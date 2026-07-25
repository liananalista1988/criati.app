# Implementação CRIATI-FIN-013 — Compras para terceiros, valores a receber e ressarcimentos

> **Nota de revisão (CRIATI-FIN-013A, atualizada em CRIATI-FIN-015).** O desenho original abaixo
> previa que cada ressarcimento gerasse um `LancamentoFinanceiro` de receita com origem dedicada
> `RESSARCIMENTO_COMPRA_TERCEIRO`. Esse desenho **foi substituído antes de ir para produção** e
> **não corresponde ao código atual**: nenhum ressarcimento gera `LancamentoFinanceiro` — o valor
> entra diretamente no saldo da conta via `SaldoFinanceiroService` (soma dos
> `RessarcimentoParcelaCartao` com status `ATIVO`), fora da soma de receitas/despesas. O enum
> `OrigemLancamentoFinanceiro` **não tem** o valor `RESSARCIMENTO_COMPRA_TERCEIRO`. As seções
> "Regras financeiras", "Limitação patrimonial documentada" e "Impacto no cartão e no caixa"
> abaixo foram corrigidas para refletir esse comportamento; o restante do documento (diagnóstico,
> decisão de modelagem, migration, repositories, endpoints, testes) continua válido.

## Diagnóstico do modelo existente

Antes de criar qualquer estrutura, auditou-se o que já existe:

- `CompraCartao` já possui um campo `parteFinanceira` (`ParteFinanceira`, **opcional**, coluna
  `parte_financeira_id` nula). Nenhum código do módulo hoje lê esse campo para diferenciar "compra
  da residência" de "compra para terceiro" — nenhuma validação o exige, nenhum relatório o usa.
  `grep` por "terceiro" em todo `src/main/java` não retornou nenhuma ocorrência.
- `ParcelaCompraCartao` já modela cada parcela (número, valor, competência/vencimento, status)
  ligada 1:N a `CompraCartao`. Não existe `Fatura`/`PagamentoFatura` implementados ainda — o campo
  `fatura_id` em `parcela_compra_cartao` é um UUID solto, sem FK, reservado para uma tarefa futura.
- Como consequência direta do ponto anterior: **hoje nenhuma compra de cartão (para a residência ou
  para terceiro) gera `LancamentoFinanceiro`.** O impacto no caixa só nasceria quando a fatura fosse
  paga — mecanismo que ainda não existe. Isso simplifica esta tarefa: não há nenhum lançamento de
  despesa "da compra" para excluir, porque nenhum é gerado.
- O documento conceitual `MODELO-DE-DADOS.md` (seção `CompraCredito`/`Recebivel`) já havia previsto
  `e_para_terceiro` e um `Recebivel` genérico unificando empréstimos e compras para terceiros. Essa
  entidade genérica **não foi construída** — nem aqui, nem em CRIATI-FIN-010 (empréstimos concedidos
  usaram uma estrutura específica, `ParcelaEmprestimo`, não um `Recebivel` compartilhado). Manter essa
  mesma decisão aqui evita "criar uma arquitetura ampla de movimentações patrimoniais sem
  autorização", exatamente como a tarefa pede.
- `INVARIANTES.md` já documentava (item 4, escrito antes desta implementação) que uma compra para
  terceiro deve ser excluída do cálculo de gasto real da residência — este item já era coerente com
  o fato de a compra nunca gerar lançamento de despesa.

## Decisão de modelagem

Reaproveitamento máximo, responsabilidade nova mínima:

- **`CompraCartao` e `ParcelaCompraCartao` não foram alterados** (nenhuma coluna nova, nenhuma
  migration `ALTER` sobre eles). A "compra para terceiro" continua sendo, estruturalmente, a mesma
  `CompraCartao` de sempre — criada através do **mesmo** `CompraCartaoService.criar(...)` já usado
  por compras da residência (mesmo cálculo de limite, mesmo parcelamento, mesmo cartão virtual/
  principal). `CompraTerceiroService` é uma camada fina que **exige** `parteFinanceiraId` (opcional
  no fluxo normal) e, a partir do resultado, cria os registros de rastreio abaixo.
- **`ValorAReceberParcelaCartao`** (nova, 1:1 com `ParcelaCompraCartao`, `parcela_id UNIQUE`): não
  repete número, valor ou vencimento da parcela — só acrescenta o que falta (valor recebido, data
  prometida, data efetiva de recebimento, status). O `vencimento` armazenado é uma cópia deliberada
  e imutável de `parcela.competencia` no momento da criação, só para permitir índice próprio por
  vencimento sem depender de `JOIN` — nunca atualizado depois, sem risco de divergência (a
  competência de uma parcela de cartão nunca muda).
- **`RessarcimentoParcelaCartao`** (nova, histórico de liquidações, mesmo desenho de
  `RecebimentoParcelaEmprestimo`): cada ressarcimento (integral ou parcial) é um registro imutável,
  nunca apagado; estorno marca status `ESTORNADO`, preservando o histórico.
- A pergunta "esta `CompraCartao` é uma compra para terceiro?" nunca é respondida checando
  `parteFinanceira != null` (esse campo continua genérico e pode, em tese, ser preenchido por outro
  motivo em compras normais). A resposta é sempre "existe pelo menos um
  `ValorAReceberParcelaCartao` apontando para as parcelas desta compra?" — fonte única de verdade,
  criada atomicamente dentro da mesma transação de `CompraTerceiroService.registrar`.
- Situações usam nomenclatura própria (`PENDENTE`, `PARCIALMENTE_RESSARCIDA`, `RESSARCIDA`,
  `ATRASADA`, `CANCELADA` — exatamente as pedidas), em vez de reaproveitar `StatusParcelaEmprestimo`
  (`PAGO`/`PARCIALMENTE_PAGO`): mesmo padrão de status persistido (4 valores) + situação computada
  (5 valores, com atraso derivado de `vencimento` × saldo pendente, nunca uma transição própria) já
  usado por `ParcelaEmprestimo`. Já `StatusRecebimentoParcelaEmprestimo`-como-nome não foi reutilizado
  para o histórico de ressarcimento (mesmo par `ATIVO`/`ESTORNADO`) — cada agregado no código já tem
  seu próprio enum de 2 valores (`StatusCompraCartao`, `StatusParcelaCartao`,
  `StatusPagamentoOcorrencia`, `StatusRecebimentoParcelaEmprestimo`); criar
  `StatusRessarcimentoParcelaCartao` segue esse padrão já estabelecido em vez de acoplar um nome de
  domínio de empréstimos a um agregado de cartão.

## Branch e base

- Base obrigatória: `fd2b34a` (branch `feat/integracao-fundacao-dominio`).
- Branch de trabalho: `feat/financeiro-compras-terceiros`, criada a partir de `fd2b34a` no worktree
  isolado `C:\Users\liann\workspace\criati-claude` (o diretório principal estava em uso concorrente
  por outro agente durante parte desta implementação).

## Entidades alteradas ou criadas

- **`LancamentoFinanceiro`/`OrigemLancamentoFinanceiro`: não alterados** (ver nota de revisão no
  topo deste documento — o desenho original previa alterá-los para o ressarcimento, substituído
  antes de ir para produção).
- **Novas**: `ValorAReceberParcelaCartao`, `RessarcimentoParcelaCartao`.
- **Novos enums**: `StatusValorAReceberCompraCartao`, `SituacaoValorAReceberCompraCartao`,
  `StatusRessarcimentoParcelaCartao`.
- **`CompraCartao`/`ParcelaCompraCartao`/`CartaoCredito`**: nenhuma alteração.

## Migration

`V15__criar_ressarcimentos_compras_terceiros.sql` — duas tabelas novas
(`valor_a_receber_parcela_cartao`, `ressarcimento_parcela_cartao`), seguindo exatamente os padrões
de auditoria, `CHECK`, `UNIQUE`, FK e índices por empresa já usados em V11/V13/V14. Nenhuma tabela
existente foi alterada (em particular, `ck_lancamento_financeiro_origem` não foi tocado — ver nota
de revisão no topo: o ressarcimento nunca gera `LancamentoFinanceiro`, então não precisa de uma
origem própria nesse constraint). Nenhuma migration anterior foi alterada.

## Repositories

- `ValorAReceberParcelaCartaoRepository`: consultas por empresa/status/vencimento/compra, e
  **`findForUpdateByIdAndEmpresaId`** com `@Lock(LockModeType.PESSIMISTIC_WRITE)` — mesmo padrão de
  `ParcelaEmprestimoRepository`/`CartaoCreditoRepository`.
- `RessarcimentoParcelaCartaoRepository`: histórico por valor a receber, checagem de unicidade por
  lançamento.

## Services

- **`CompraTerceiroService`**: `registrar` (exige `parteId`, delega 100% da criação da compra para
  `CompraCartaoService.criar`, depois cria um `ValorAReceberParcelaCartao` por parcela), `listar`,
  `buscar` (detalhe = compra + valores a receber), `cancelar` (cascata para os valores a receber
  ainda não ressarcidos, nunca apaga nada, nunca mexe na `CompraCartao` em si).
- **`ValorAReceberParcelaCartaoService`**: consulta, vencidas, próximas do vencimento, resumo
  (total ressarcido, saldo a receber, contadores), data prometida.
- **`RessarcimentoParcelaCartaoService`**: `receberIntegral`/`receberParcial` (com lock pessimista,
  ver seção de concorrência) e `estornar` (cancela o lançamento vinculado, marca `ESTORNADO`, nunca
  exclui).

## Endpoints

- `POST/GET /api/contexto/financeiro/compras-terceiros`, `GET /{id}`, `POST /{id}/cancelar`.
- `GET /api/contexto/financeiro/valores-a-receber-cartao` (+ `/vencidas`, `/proximas-vencimento`,
  `/resumo`, `/{id}`), `PUT /{id}/data-prometida`, `GET /{id}/ressarcimentos`,
  `POST /{id}/receber-integral`, `POST /{id}/receber-parcial`,
  `POST /{id}/ressarcimentos/{ressarcimentoId}/estornar`.

Nenhuma tela, CSS, JavaScript ou fragmento foi tocado.

## Regras financeiras e tratamento do principal ressarcido

1. **A compra consome limite do cartão** — automático e sem código novo, porque `registrar` chama o
   mesmo `CompraCartaoService.criar` que já calcula `comprometido`/`limiteDisponivel`. Testado em
   `compraParaTerceiroImpactaLimiteDoCartaoNormalmente` (uma segunda compra que estouraria o limite
   é rejeitada, igual a uma compra normal).
2. **A parcela entra na fatura** — inalterado; `ParcelaCompraCartao` continua sendo gerada
   exatamente como antes.
3. **O pagamento da fatura reduz o caixa** — mecanismo ainda não implementado no projeto (nem para
   compras da residência). Fora do escopo desta tarefa; não inventado aqui.
4. **Direito a receber do terceiro** — nasce no mesmo instante em que a compra é registrada: um
   `ValorAReceberParcelaCartao` por parcela, status `PENDENTE`, saldo igual ao valor da parcela.
5. **O ressarcimento gera entrada de caixa sem passar por `LancamentoFinanceiro`** — via
   `RessarcimentoParcelaCartao` (registro imutável, conta/data/forma de pagamento escolhidos no
   momento do recebimento), somado diretamente ao saldo da conta por `SaldoFinanceiroService`
   (soma dos ressarcimentos com status `ATIVO`), fora da soma de receitas/despesas.
6. **O principal ressarcido não é interpretado como receita econômica da residência** — porque
   nunca vira `LancamentoFinanceiro` de tipo `RECEITA`: não há lançamento para excluir de nenhuma
   métrica futura de "resultado econômico"/"lucro", o valor simplesmente não entra nessa soma.
7. **Taxas/juros adicionais** — não implementados neste lote (fora do escopo, como o próprio
   enunciado antecipa); `RessarcimentoParcelaCartao.valor` cobre apenas o principal da parcela.

### Limitação patrimonial documentada (RECEITA/DESPESA)

O modelo atual (`TipoFinanceiro`) só tem `RECEITA` e `DESPESA` — não existe uma natureza neutra de
"movimentação patrimonial" (nem para a concessão de empréstimos, nem para compras para terceiros).
Isso foi examinado explicitamente antes de integrar com o restante do módulo financeiro:

- A **compra** em si nunca gera lançamento (nem aqui, nem para compras da residência) — não há
  "despesa" a evitar classificar incorretamente, porque nenhuma é criada.
- O **ressarcimento** (entrada de caixa real) precisa ficar visível ao restante do módulo
  (dashboard, saldo de conta) sem ser confundido com receita real da residência — o valor ressarcido
  **não é lucro/renda**, é a devolução de um valor que ela adiantou.
- **Decisão final (CRIATI-FIN-013A), mais forte que a mitigação originalmente cogitada**: em vez de
  criar um `LancamentoFinanceiro` de `RECEITA` com origem dedicada para depois ter que excluí-lo de
  métricas de resultado, o ressarcimento **nunca entra em `LancamentoFinanceiro`**. Ele é somado
  diretamente ao saldo da conta por `SaldoFinanceiroService` — a mesma fonte única de verdade usada
  pelo dashboard (`DashboardFinanceiroService`) e pelo resumo de lançamentos
  (`LancamentoFinanceiroService.resumir`). Isso elimina por completo o risco de um relatório futuro
  que apenas some `RECEITA` menos `DESPESA` "inventar" lucro a partir de um reembolso — não há
  filtro de origem para lembrar de aplicar, porque a fonte de dados errada nunca é alimentada.
- Como consequência, o `EMPRESTIMO_CONCEDIDO` (`CRIATI-FIN-010`, que ainda gera `LancamentoFinanceiro`
  de receita com origem dedicada para o recebimento de parcela) **não** é mais o precedente seguido
  aqui — os dois módulos hoje resolvem o mesmo problema de formas diferentes; unificá-los (por
  exemplo migrando empréstimos para o mesmo padrão de "soma direta no saldo") é uma evolução futura,
  não decidida nem implementada nesta tarefa.
- **Evolução futura recomendada, não implementada aqui** (fora de escopo — "não crie uma
  arquitetura ampla de movimentações patrimoniais sem autorização"): introduzir uma terceira
  natureza em `TipoFinanceiro` (ex.: `MOVIMENTACAO_PATRIMONIAL`) que represente esse tipo de entrada
  de caixa de forma explícita no próprio modelo de lançamentos, em vez de ficar fora dele.

## Impacto no cartão e no caixa

- **Cartão**: idêntico ao de uma compra normal (mesmo `criar`, mesmo `comprometido`/limite). Testado.
  A partir da CRIATI-FIN-015, o resumo de compras (`ResumoComprasCartao`) e o resumo de cartões
  (`ResumoCartoesCredito`) também separam explicitamente quanto do total/limite comprometido vem de
  compras da residência vs. de terceiros — sem remover terceiros do cálculo em nenhum dos dois.
- **Caixa**: só é afetado pelo **ressarcimento** (entrada direta no saldo da conta via
  `RessarcimentoParcelaCartao`/`SaldoFinanceiroService`, nunca via `LancamentoFinanceiro` — ver
  regra 5 acima). A partir da CRIATI-FIN-015, o dashboard (`DashboardFinanceiroService`) expõe um
  indicador `totalPendenteReceberTerceiros` (saldo ainda não ressarcido), separado de
  `totalPendenteReceber`/`receitasPagas`/`resultadoMes`.

## Proteção de concorrência

`ValorAReceberParcelaCartaoRepository.findForUpdateByIdAndEmpresaId` (`@Lock(PESSIMISTIC_WRITE)`),
único ponto de leitura em `RessarcimentoParcelaCartaoService.prepararParaRecebimento` — mesmo
desenho já auditado em CRIATI-FIN-010A. `ValorAReceberParcelaCartaoLockPessimistaTests` prova, com
duas transações reais em threads distintas (não mocks), que uma segunda transação fica bloqueada até
a primeira liberar a linha (espera medida ≥ 250ms para um `sleep` de 400ms na primeira). Mesma
limitação conhecida documentada em CRIATI-FIN-010A: H2 (mesmo em `MODE=PostgreSQL`) não é
garantidamente idêntico ao MVCC do PostgreSQL real — a garantia definitiva vem da validação em
PostgreSQL real (feita nesta tarefa, ver abaixo).

## Isolamento multiempresa

Toda consulta por ID em todos os repositories novos exige `empresaId`
(`findByIdAndEmpresaId`/`findForUpdateByIdAndEmpresaId`). `CompraTerceiroService` reaproveita
`CompraCartaoService.buscar`, que já valida pertencimento à empresa (compra de outro tenant é
rejeitada — `DadosInvalidosException`, 400, mesma convenção já estabelecida por esse serviço, não um
404 dedicado). `ParteFinanceira`, `ContaFinanceira` e `CategoriaFinanceira` de outro tenant são
rejeitadas pelas mesmas validações já existentes em `CompraCartaoService`/`RessarcimentoParcelaCartaoService`.
Testado em `parteFinanceiraDeOutroTenantERejeitada` e `compraParaTerceiroDeOutroTenantERejeitada`.

## Testes

- **Domínio puro** (`ValorAReceberParcelaCartaoTests`, 13 testes): saldos, status, situação
  computada (incluindo `ATRASADA`), estorno, cancelamento, data prometida.
- **Concorrência** (`ValorAReceberParcelaCartaoLockPessimistaTests`, 3 testes): existência/uso do
  lock, isolamento por empresa no método com lock, bloqueio real medido entre threads.
- **JPA** (`RessarcimentosComprasTerceirosJpaTests`, 4 testes): auditoria/UUID, unicidade
  `(parcela_id)` bloqueada pelo banco, persistência do ressarcimento sem gerar
  `LancamentoFinanceiro`, consulta por conta usada pelo cálculo de saldo restrita ao tenant.
- **HTTP** (`ComprasTerceirosControllerTests`, testes crescentes a cada tarefa — ver arquivo para o
  total atual): cadastro (com e sem terceiro, parcelamento com arredondamento verificado, tenant
  cruzado), ressarcimento parcial/integral/rejeições, data prometida, vencidas/próximas do
  vencimento, resumo, cancelamento e estorno preservando histórico, ausência de lançamento de
  despesa/receita em nenhum dos dois fluxos, impacto no limite do cartão, segurança
  (CSRF/autenticação/perfil).

## Resultado da suíte completa

```
mvnw.cmd clean test
Tests run: 693, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS — 08:36 min
```
(652 anteriores + 13 domínio + 4 JPA + 3 concorrência + 21 HTTP = 693; nenhuma regressão.)

## Validação PostgreSQL real

PostgreSQL 18.4 nativo, banco/role temporários (`criati_validacao_fin013`), Flyway a partir de
banco vazio, `ddl-auto=validate` (Hibernate nunca criou/alterou schema). Todas as 15 migrations
aplicadas com sucesso (`Successfully applied 15 migrations ... now at version v15`); `ck_lancamento_financeiro_origem`
confirmado com `RESSARCIMENTO_COMPRA_TERCEIRO` incluído; as duas tabelas novas inspecionadas via
`\d+` e `pg_constraint`/`pg_indexes` — tipos, escalas (`numeric(19,2)`), `NOT NULL`, `CHECK`, FKs e
índices por empresa/status/vencimento conferem exatamente com o projetado. Banco e role temporários
removidos ao final; `criati_db` nunca foi tocado; nenhuma senha registrada neste documento.

## Documentos relacionados

- `IMPLEMENTACAO-CRIATI-FIN-010.md` — mesma limitação RECEITA/DESPESA para empréstimos concedidos.
- `INVARIANTES.md` — item 4 (compra para terceiro excluída do gasto real).
- `MODELO-DE-DADOS.md` — desenho conceitual original (`Recebivel` genérico), não adotado; ver
  decisão de modelagem acima.
