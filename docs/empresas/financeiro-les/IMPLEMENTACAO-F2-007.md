# Implementação LES-F2-007 — Contas a pagar e ocorrências de compromisso

## Diagnóstico e reaproveitamento

O commit de referência (`8762c0b`, LES-F2-006) já entrega `RecorrenciaFinanceira` completa (periodicidade
mensal/anual, geração idempotente de `LancamentoFinanceiro`, pausa/retomada/encerramento) e
`LancamentoFinanceiro` completo (competência, vencimento, liquidação, status, saldo derivado). Inventário
realizado antes de qualquer alteração:

- **Nenhum conceito de "conta a pagar"/"compromisso"/"parcela"/"comprovante"/"pagamento parcial" existia** em
  código (`br.app.criati.**`) ou em migrations — apenas propostas em `MODELO-DE-DADOS.md`. `vencimento` só
  existia como campo (`dataVencimento`) em `LancamentoFinanceiro`/`RecorrenciaFinanceira`, nunca como conceito
  próprio.
- `LancamentoFinanceiro` não suporta pagamento parcial: `valor` é único, `status` é uma bandeira do registro
  inteiro (`PENDENTE`/`LIQUIDADO`/`PAGO`/`CANCELADO`), sem `valorPago`/`saldoRestante`. Reaproveitável como o
  destino final de todo pagamento (via nova origem `CONTA_A_PAGAR`), não como o modelo de conta a pagar em si.
- **Nenhuma infraestrutura de anexos/upload existe** em nenhum domínio do projeto (sem `MultipartFile`, sem
  storage service, sem validação de MIME/tamanho/extensão). Não é uma lacuna desta tarefa: é uma ausência de
  base a construir do zero, e `PENDENCIAS.md` já reserva essa construção para `LES-F2-010` ("Formatos, tamanho
  máximo e visualização de anexos, antes de `LES-F2-010`"). Ver seção "Comprovantes" abaixo.
- **Nenhuma auditoria JPA (`@CreatedBy`/`@EnableJpaAuditing`) existe.** O padrão do módulo (usado por
  `LancamentoFinanceiro`/`RecorrenciaFinanceira`) é manual: `criadoEm`/`atualizadoEm` setados no construtor,
  `criadoPor`/`atualizadoPor` como `@ManyToOne Usuario` resolvidos no service via
  `usuarioRepository.findById(contexto.usuarioId())` e um `registrarAlteracao(Usuario autor)` privado chamado
  por todo método mutador. As três entidades novas seguem exatamente esse padrão.
- `CategoriaFinanceira`, `PessoaFinanceira`, `ParteFinanceira`, `ContaFinanceira` foram reaproveitadas
  integralmente sem alteração de contrato; `LancamentoFinanceiro` ganhou apenas uma nova origem de enum e um
  novo factory method (`gerarDeContaAPagar`), sem alterar nenhum construtor ou método existente.

Nada foi duplicado: as três novas entidades (`CompromissoFinanceiro`, `OcorrenciaCompromisso`,
`PagamentoOcorrenciaCompromisso`) vivem em `br.app.criati.financeiro.model`/`.repository`/`.service`/`.web`,
lado a lado com `LancamentoFinanceiro`/`RecorrenciaFinanceira` — não em `br.app.criati.financeiro.les`, pacote
reservado às extensões residenciais específicas (cartão, fatura, empréstimo), que esta tarefa explicitamente
não implementa.

## Compromisso, ocorrência e pagamento

Três conceitos deliberadamente distintos, análogos à separação já estabelecida na LES-F2-006 entre
recorrência/ocorrência/lançamento:

- **Compromisso** (`CompromissoFinanceiro`) — a regra ou origem da obrigação (energia, condomínio, mensalidade,
  manutenção avulsa). Nunca movimenta saldo, nunca tem valor "pago". Pode ser avulso (sem recorrência) ou
  recorrente (vinculado a uma `RecorrenciaFinanceira` existente).
- **Ocorrência** (`OcorrenciaCompromisso`) — a obrigação concreta de uma competência específica, com
  vencimento, valores (previsto/principal/juros/multa/desconto/total/pago/saldo) e status. Pode existir sem
  compromisso (obrigação avulsa pura) e sem recorrência.
- **Pagamento** (`PagamentoOcorrenciaCompromisso`) — a liquidação integral ou parcial de uma ocorrência. Uma
  ocorrência pode ter zero, um (integral) ou vários (parciais) pagamentos ativos.

## Integração com recorrências — fluxo escolhido

**Decisão registrada em `docs/DECISOES.md`.** O enunciado apresentava duas opções: "recorrência gera
ocorrência de compromisso" ou "recorrência gera lançamento vinculado à ocorrência". Adotada a primeira, com um
detalhe importante de implementação:

```
Compromisso avulso (sem recorrência)
    → ocorrências criadas manualmente uma a uma (POST /ocorrencias-compromisso)

Compromisso recorrente (compromisso.recorrencia = uma RecorrenciaFinanceira já existente)
    → RecorrenciaFinanceira (regra, motor de calculo de competencia/vencimento, ja testado na LES-F2-006)
    → OcorrenciaCompromissoService.gerarPorRecorrencia(compromissoId)
        - reaproveita RecorrenciaFinanceira.getProximaCompetencia()/calcularVencimento()/dentroDoPeriodo()/
          avancarProximaCompetencia() SEM REIMPLEMENTAR periodicidade/calculo de dia invalido
        - cria uma OcorrenciaCompromisso (nao um LancamentoFinanceiro)
        - idempotente: verifica OcorrenciaCompromissoRepository.findByRecorrenciaIdAndCompetencia antes de
          criar, exatamente como RecorrenciaFinanceiraService.gerarOuRetornarExistente ja fazia para
          LancamentoFinanceiro
    → Pagamento (integral ou parcial) da ocorrencia
    → LancamentoFinanceiro.gerarDeContaAPagar(...) — DESPESA, LIQUIDADO, origem CONTA_A_PAGAR
```

Isto **não cria um segundo mecanismo de repetição**: a única fonte de verdade para periodicidade, intervalo,
dia de referência, mês de referência e cálculo de "último dia do mês" continua sendo `RecorrenciaFinanceira`
(entidade e testes da LES-F2-006, intocados). `OcorrenciaCompromissoService` apenas chama os métodos públicos
já existentes dessa entidade; nenhuma lógica de competência/vencimento foi duplicada.

**Compatibilidade com o fluxo antigo.** Uma `RecorrenciaFinanceira` que já gerava `LancamentoFinanceiro`
diretamente (uso pré-existente, sem compromisso vinculado) continua funcionando de forma 100% inalterada —
zero regressão nos 501 testes anteriores. Uma `RecorrenciaFinanceira` recém-vinculada a um `CompromissoFinanceiro`
passa a ser bloqueada no fluxo antigo (`RecorrenciaFinanceiraService.exigirNaoVinculadaACompromisso`, `400`
em `POST /recorrencias/{id}/gerar`, `/gerar-competencia` e ignorada silenciosamente em
`/gerar-automaticas`), para impedir que a mesma competência seja consumida duas vezes por dois caminhos
diferentes (um gerando `OcorrenciaCompromisso`, outro gerando `LancamentoFinanceiro` direto). Um compromisso
não pode ser criado apontando para uma recorrência já vinculada a outro compromisso
(`uq_compromisso_financeiro_recorrencia`, único por `recorrencia_id`, validada em app e em banco).

**Por que o `LancamentoFinanceiro` gerado pelo pagamento não referencia `recorrencia_id`.** O enum
`OrigemLancamentoFinanceiro` ganhou o valor `CONTA_A_PAGAR` e `LancamentoFinanceiro.gerarDeContaAPagar(...)`
(novo factory, análogo a `gerarDeRecorrencia`) cria o lançamento de despesa liquidado — mas deliberadamente
**sem** setar `recorrencia_id`. Se setasse, dois pagamentos parciais da mesma ocorrência (mesma
`recorrencia_id` + mesma `data_competencia`) colidiriam com `uq_lancamento_financeiro_recorrencia_competencia`
(constraint da LES-F2-006, pensada para o fluxo "um lançamento por competência", que não é o caso aqui, onde
uma competência pode ter N pagamentos). A rastreabilidade fica a cargo de
`PagamentoOcorrenciaCompromisso.ocorrencia` → `OcorrenciaCompromisso.compromisso`/`recorrencia`/`competencia`.

Edição de uma recorrência (`RecorrenciaFinanceiraService.editar`) não altera ocorrências já geradas — o
mesmo comportamento já garantido para lançamentos na LES-F2-006, agora também válido para ocorrências (a
ocorrência copia os dados no momento da geração, não referencia campos mutáveis da recorrência para exibição).

## Compromisso — campos e tipos de valor

`CompromissoFinanceiro`: `empresa`, `descricao`, `categoria` (deve ser `DESPESA`), `pessoaFinanceira`,
`parteFinanceira` opcional, `contaPadrao` opcional, `recorrencia` opcional (única por compromisso),
`tipoValor` (`FIXO`/`VARIAVEL`), `valorPadrao` opcional, `diaVencimentoPadrao` opcional (1–31),
`formaPagamentoPadrao` opcional, `ativo` (boolean), `observacao`, auditoria (padrão manual do módulo).

`tipoValor` é informativo/orientador de UI (ex.: condomínio = FIXO, energia = VARIAVEL) — não impede ajuste
manual do `valorPrincipal` em cada ocorrência gerada, exatamente como pedido.

## Ocorrência — campos, status e cálculos

`OcorrenciaCompromisso`: `empresa`, `compromisso` opcional, `recorrencia` opcional, `competencia`
(armazenada como `YearMonth`/dia 1, mesmo padrão de `RecorrenciaFinanceira.proximaCompetencia`), `descricao`,
`categoria`, `pessoaFinanceira`, `parteFinanceira` opcional, `contaPrevista` opcional, `valorPrevisto`
opcional, `valorPrincipal`, `vencimento` (obrigatório), `dataRecebimentoCobranca` opcional (não confundido com
competência/vencimento/pagamento — apenas informativo), `status`, `juros`/`multa`/`desconto` (padrão zero),
`valorPago` (mantido pela própria entidade a cada pagamento/estorno), `observacao`, auditoria.

**Status persistidos**: `PENDENTE`, `PARCIALMENTE_PAGA`, `PAGA`, `CANCELADA`. **Vencido é sempre calculado**
(`estaVencida(LocalDate hoje)`), nunca persistido como estado — depende de status ≠ CANCELADA, saldo pendente
> 0 e vencimento anterior a hoje; `diasEmAtraso(hoje)` é `0` quando não vencida.

**Cálculos** (`BigDecimal`, escala 2, `HALF_UP`, sempre normalizados na entidade):

```
valorTotal   = valorPrincipal + juros + multa - desconto        (nunca negativo — validado na escrita)
saldoPendente = valorTotal - valorPago                          (nunca negativo — clamada em zero)
```

Regras de integridade aplicadas em `OcorrenciaCompromisso` (dominio) e reforçadas em `V11` (banco):
componentes monetários nunca negativos; desconto não pode superar principal+juros+multa; pagamento maior que
o saldo pendente é rejeitado (`DadosInvalidosException`, 400); reduzir o valor total abaixo do que já foi
pago é rejeitado (`atualizarValores` exige `novoTotal >= valorPago`); ocorrência cancelada não aceita novos
pagamentos nem edições de valor/vencimento.

`status` é recalculado (nunca persistido "manualmente" por fora) a cada pagamento/estorno/alteração de valor:
`PENDENTE` se `valorPago == 0`; `PAGA` se `saldoPendente <= 0`; senão `PARCIALMENTE_PAGA`; `CANCELADA` é
terminal e nunca recalculada automaticamente.

## Pagamento — integral, parcial e estorno

`PagamentoOcorrenciaCompromisso`: `empresa`, `ocorrencia`, `conta` (mesma empresa, ativa), `valor` (> 0, ≤
saldo pendente no momento do pagamento), `dataPagamento`, `formaPagamento` opcional, `observacao` opcional,
`lancamentoFinanceiro` (referência única — `UNIQUE(lancamento_financeiro_id)` — um pagamento nunca compartilha
lançamento com outro), `status` (`ATIVO`/`ESTORNADO`), `estornadoEm`/`estornadoPor`/`motivoEstorno`,
`criadoEm`/`criadoPor`. Campos são imutáveis após criação (`updatable = false`) exceto os campos de estorno —
**exclusão física nunca é permitida**, apenas o marcador `ESTORNADO`.

**Pagar integralmente** (`PagamentoOcorrenciaCompromissoService.pagarIntegral`): usa exatamente o saldo
pendente da ocorrência no momento da chamada, exige conta ativa da mesma empresa e data de pagamento. **Pagar
parcialmente** (`pagarParcial`): mesma validação, valor explícito do chamador, rejeitado se exceder o saldo.
Em ambos os casos, o serviço:

1. cria e salva um `LancamentoFinanceiro` via `gerarDeContaAPagar` (DESPESA, `LIQUIDADO`, origem
   `CONTA_A_PAGAR`, `dataCompetencia` = competência da ocorrência, `dataVencimento` = vencimento da ocorrência,
   `dataPagamento` = data do pagamento) — **o único ponto onde o saldo das contas é impactado**, reaproveitando
   o mecanismo de saldo já existente (`SaldoFinanceiroService`/`compoeSaldoRealizado`), sem lógica paralela;
2. cria o `PagamentoOcorrenciaCompromisso` referenciando esse lançamento (nunca dois pagamentos apontam para o
   mesmo lançamento — `UNIQUE`);
3. chama `OcorrenciaCompromisso.registrarPagamento(valor, autor)`, que soma ao `valorPago` e recalcula status.

**Estornar** (`estornar`, exige perfil `ADMINISTRADOR` — mesmo precedente de
`LancamentoFinanceiroService.desliquidar`): rejeita pagamento já estornado (409); chama
`LancamentoFinanceiro.cancelar(autor)` (método já existente, reaproveitado — não foi criado um mecanismo de
reversão paralelo) sobre o lançamento do pagamento, o que zera seu impacto no saldo; marca o pagamento como
`ESTORNADO` com motivo/usuário/instante; chama `OcorrenciaCompromisso.estornarPagamento(valor, autor)`, que
subtrai do `valorPago` e recalcula status (nunca reabre uma ocorrência `CANCELADA`).

## Vencimento e atraso

Vencimento é obrigatório em toda ocorrência (`@NotNull` no DTO, `NOT NULL` no banco). Atraso é sempre
calculado a partir de `vencimento`, `status` e a data atual — nunca persistido. Pagamento parcial não encerra
o atraso enquanto houver saldo; pagamento integral encerra porque zera o saldo. Juros não são calculados
automaticamente por atraso nesta entrega (fora de escopo, conforme enunciado) — `juros`/`multa`/`desconto`
são sempre lançamento manual do usuário, com padrão zero.

## Comprovantes — limitação documentada

**Não implementado nesta tarefa.** Diagnóstico confirmou que não existe nenhuma infraestrutura de upload de
arquivos em nenhum domínio do projeto (sem `MultipartFile`, sem storage service, sem validação de
MIME/tamanho/extensão, sem geração seguro de nome físico). Construir isso agora, de forma segura
(isolamento por tenant, allowlist de extensão/MIME, nomes físicos não confiáveis do original, sem
storage inseguro improvisado) é um esforço de base própria, já **explicitamente reservado para `LES-F2-010`**
em `docs/empresas/financeiro-les/PENDENCIAS.md` ("Formatos, tamanho máximo e visualização de anexos, antes de
`LES-F2-010`"). Implementar um upload improvisado agora violaria a instrução explícita de não improvisar
armazenamento inseguro. Os campos "comprovante" pedidos no enunciado (ocorrência/pagamento) **não foram
adicionados** ao modelo de dados desta entrega — quando `LES-F2-010` (ou tarefa equivalente) entregar a
infraestrutura de anexos, ela deve ser associável a `OcorrenciaCompromisso`/`PagamentoOcorrenciaCompromisso`
por FK, sem exigir migração destrutiva das tabelas criadas aqui.

## Calendário, alertas e resumo

`GET /ocorrencias-compromisso/calendario?mes=AAAA-MM`: lista simples das ocorrências cujo vencimento cai no
mês informado, ordenada por vencimento — lista cronológica, sem biblioteca de calendário externa, conforme
pedido. `GET /ocorrencias-compromisso/resumo`: agregados do período (total previsto/principal/juros/
multas/descontos/pago/saldo pendente, quantidades por status, vencidas, vencendo em breve).

**Alerta interno de "vencendo em breve"**: constante `OcorrenciaCompromissoService.ALERTA_DIAS_ANTECEDENCIA = 3`
(dias), conforme padrão inicial pedido no enunciado — não persistido/configurável nesta entrega, apenas uma
constante documentada no código. Nenhuma notificação externa (e-mail/WhatsApp/push/Google Calendar) foi
implementada — apenas os indicadores internos already citados (contagem no resumo, badge de status/vencida na
listagem e no calendário).

## Multiempresa

Todas as consultas usam `empresaId` do `ContextoEmpresaAtual` da sessão (nunca aceito do cliente).
Repositórios expõem `findByIdAndEmpresaId`/`findAllByEmpresaId*`; um ID de outro tenant (compromisso,
ocorrência, pagamento, conta usada no pagamento) resulta em `404`/`AcessoNegadoException`, nunca revela a
existência do recurso em outra empresa — mesmo padrão já usado em `RecorrenciaFinanceiraService`/
`LancamentoFinanceiroService`. Testado em `ContasAPagarControllerTests` (compromisso, ocorrência e conta de
outro tenant).

## Migration `V11__criar_contas_a_pagar.sql`

Cria `compromisso_financeiro`, `ocorrencia_compromisso`, `pagamento_ocorrencia_compromisso` com FKs, checks
(tipo de valor, status, valores não negativos, desconto limitado, forma de pagamento) e índices por empresa,
empresa+status, empresa+vencimento, empresa+pessoa, empresa+categoria, empresa+parte, empresa+compromisso,
empresa+competência. Adiciona o valor `CONTA_A_PAGAR` ao `CHECK ck_lancamento_financeiro_origem` (drop +
re-add do mesmo constraint, mesmo padrão já usado por `V9` ao evoluir o check de status — nenhuma migration
anterior foi editada, apenas uma nova evolução em `V11`). `V1`–`V10` permanecem intocadas.

**As mesmas constraints/índices foram replicados nas anotações JPA** (`@UniqueConstraint` em
`CompromissoFinanceiro`/`OcorrenciaCompromisso`/`PagamentoOcorrenciaCompromisso`) porque os testes usam
`ddl-auto=create-drop` a partir das entidades (Flyway desabilitado em teste, decisão pré-existente do
projeto) — sem isso, os testes de unicidade rodariam contra um schema mais permissivo que a migration real.
Essa divergência já existia antes desta tarefa (mesma observação vale para `V9`/`V10`) e foi confirmada na
prática: o primeiro rascunho desta entrega omitiu o `@UniqueConstraint` de `recorrencia_id` em
`CompromissoFinanceiro` e um teste de integridade (`ContasAPagarJpaTests`) falhou até a anotação ser
adicionada — evidência de que os testes de constraint são úteis exatamente para pegar esse tipo de
divergência antes de produção.

## Endpoints

- `GET/POST/PUT /api/contexto/financeiro/compromissos`, `GET /{id}`, `POST /{id}/ativar`, `POST /{id}/desativar`.
- `GET/POST/PUT /api/contexto/financeiro/ocorrencias-compromisso`, `GET /{id}`, `POST /gerar-por-compromisso/{compromissoId}`,
  `POST /{id}/cancelar`, `GET /resumo`, `GET /calendario`.
- `GET /{id}/pagamentos`, `POST /{id}/pagar-integral`, `POST /{id}/pagar-parcial`,
  `POST /{id}/pagamentos/{pagamentoId}/estornar`.

Todos exigem sessão autenticada + empresa ativa + aplicação `FINANCEIRO` habilitada
(`ContextoFinanceiroService.exigirAcesso`, o mesmo portão único já usado por todo o módulo). Escrita exige
perfil `ADMINISTRADOR`/`GESTOR` (`exigirEscrita`); estorno exige `ADMINISTRADOR` (mesmo precedente de
`LancamentoFinanceiroService.desliquidar`). CSRF via meta tag + `criati-api.js`, igual ao resto da aplicação.

## Interface

`GET /app/financeiro/contas-a-pagar` (`PaginaController.financeiroContasAPagar`, mesmo guard de aplicação
habilitada que as demais páginas financeiras). Três seções (Ocorrências/Compromissos/Calendário) alternadas
por botões simples (sem biblioteca de abas). Ocorrências: filtros, cards de resumo, tabela com ações
(Pagar/Pagamentos/Editar/Cancelar), modal de pagamento (checkbox "pagamento integral" pré-preenche o saldo
pendente e desabilita o campo de valor; validação de excedente antes de confirmar) e modal de histórico com
estorno. Compromissos: tabela com Editar/Gerar ocorrência (quando recorrente)/Ativar/Desativar. Estado vazio:
"Nenhuma conta a pagar encontrada. Cadastre obrigações avulsas ou recorrentes para acompanhar vencimentos e
pagamentos." — exatamente o texto sugerido no enunciado, testado em `PaginaFinanceiroSegurancaTests`.

## Compatibilidade

Nenhum lançamento, recorrência, migration ou endpoint existente foi alterado de forma incompatível.
`LancamentoFinanceiro` ganhou apenas um novo factory method e um novo valor de enum (aditivo).
`RecorrenciaFinanceiraService` ganhou um guard adicional que só se aplica a recorrências recém-vinculadas a um
compromisso (nenhuma recorrência pré-existente nos 501 testes anteriores tinha esse vínculo, logo nenhum
teste existente foi afetado — confirmado pela suíte completa passando sem alteração). Dashboard financeiro
não precisou de nenhuma mudança: como todo pagamento gera um `LancamentoFinanceiro` real com status
`LIQUIDADO`, ele já flui automaticamente para `despesasPagas`/`resumoPorCategoria`/`ultimosLancamentos`.

## Testes

- **Domínio puro** (`OcorrenciaCompromissoTests`, 18 testes): cálculo de valor total/saldo, validação de
  desconto/negativos na criação, vencimento obrigatório, vencida/dias em atraso (inclusive quitada e
  cancelada), pagamento parcial e integral, pagamento excedente/zero/negativo rejeitado, pagamento em
  ocorrência cancelada rejeitado, estorno recalculando valor pago e status, edição de valores preservando
  pagamentos já feitos (rejeita novo total menor que valor pago), edição bloqueada após cancelamento.
- **JPA/persistência** (`ContasAPagarJpaTests`, 6 testes): persistência das três entidades, unicidade de
  compromisso por recorrência, unicidade de ocorrência por recorrência+competência, unicidade de pagamento
  por lançamento — todas as quatro validadas como violação real de constraint do banco (H2), não apenas
  verificação de aplicação.
- **Web/controller** (`ContasAPagarControllerTests`, 18 testes): criação de compromisso fixo/variável,
  categoria incompatível rejeitada, ativação/desativação, isolamento de tenant (404), criação de ocorrência
  avulsa com juros/multa/desconto, vencimento ausente rejeitado, cancelamento bloqueando pagamento,
  geração por recorrência avançando competência sem duplicar, bloqueio do fluxo antigo de geração direta,
  pagamento integral gerando lançamento liquidado e quitando a ocorrência, pagamentos parciais múltiplos até
  quitar, pagamento parcial excedente rejeitado, conta de outro tenant rejeitada, estorno revertendo
  impacto/saldo/status, estorno duplicado rejeitado, estorno exigindo `ADMINISTRADOR`, resumo, calendário,
  autenticação obrigatória, CSRF obrigatório, perfil `USUARIO` sem permissão de escrita.
- **Página** (`PaginaFinanceiroSegurancaTests`, ajustado): rota `/app/financeiro/contas-a-pagar` incluída nos
  três cenários já existentes (anônimo redireciona, sem aplicação habilitada redireciona, com aplicação
  habilitada renderiza e mostra o estado vazio esperado).

Total: **42 testes novos** (18 domínio + 6 JPA + 18 web), nenhum teste dos 501 anteriores alterado ou removido.

## Limitações

- **Comprovantes**: não implementados (ver seção dedicada acima) — reservado para `LES-F2-010`.
- **Validação em PostgreSQL real**: não realizada. Nem `psql`, `pg_ctl` nem `docker` estão disponíveis neste
  ambiente de execução (confirmado por tentativa direta). A migration `V11` segue rigorosamente a sintaxe e
  as convenções já usadas e validadas em `V5`–`V10` (mesmos tipos, mesmo padrão de `FOREIGN KEY`/`CHECK`,
  mesmo padrão de `DROP CONSTRAINT`/`ADD CONSTRAINT` já usado em `V9`), mas isso não substitui uma execução
  real. Não afirmamos validação real do banco de produção — apenas validação via schema Hibernate equivalente
  em H2 (`ContasAPagarJpaTests`), que já capturou uma divergência real entre entidade e migration (ver seção
  da migration).
- **Alerta "vencendo em breve"**: constante fixa no código (`ALERTA_DIAS_ANTECEDENCIA = 3`), não configurável
  por empresa nesta entrega.
- **Sem cálculo automático de juros/multa por atraso**: sempre lançamento manual, conforme enunciado.
- **Compromisso.diaVencimentoPadrao** é apenas informativo (auxílio de preenchimento na UI ao criar uma
  ocorrência avulsa a partir de um compromisso) — não há wiring automático que aplique esse dia ao criar uma
  ocorrência; o usuário informa o vencimento explicitamente em cada ocorrência avulsa.

## Impacto na próxima tarefa

- Qualquer tarefa futura de anexos (`LES-F2-010` ou equivalente) deve adicionar uma FK opcional em
  `ocorrencia_compromisso`/`pagamento_ocorrencia_compromisso` (nova migration, não alterar `V11`).
- Extensões residenciais específicas (cartão, fatura, empréstimo, recebível — fora de escopo aqui) devem
  continuar em `br.app.criati.financeiro.les`, reaproveitando `CompromissoFinanceiro`/`OcorrenciaCompromisso`
  como base para obrigações de cartão/fatura quando essas tarefas chegarem, em vez de recriar o conceito de
  ocorrência/pagamento.
- Se uma tarefa futura quiser configurar o número de dias do alerta "vencendo em breve" por empresa, o ponto
  de extensão é `OcorrenciaCompromissoService.ALERTA_DIAS_ANTECEDENCIA`.
