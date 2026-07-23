# Implementação CRIATI-FIN-010 — Empréstimos concedidos e recebimentos parcelados

## Diagnóstico e reaproveitamento

Fundação funcional para registrar dinheiro emprestado pela residência a amigos, familiares ou
outras pessoas, e o acompanhamento dos recebimentos (pagamento único ou parcelado) até a
quitação. O desenho segue de perto o par `CompromissoFinanceiro` / `OcorrenciaCompromisso` /
`PagamentoOcorrenciaCompromisso` implementado em LES-F2-007 (`IMPLEMENTACAO-F2-007.md`) — a
mesma separação entre "regra" (`EmprestimoConcedido`), "parcela concreta" (`ParcelaEmprestimo`) e
"liquidação" (`RecebimentoParcelaEmprestimo`), invertida de despesa para receita.

Reaproveitamentos deliberados, sem duplicar estrutura já existente:

- **Pessoa devedora**: `ParteFinanceira` (não uma nova entidade de pessoas). O modelo conceitual
  de `MODELO-DE-DADOS.md` (seção `EmprestimoConcedido`/`Recebivel`) já previa `devedor_id
  (ParteFinanceira)` — o mesmo cadastro hoje usado como credor em `CompromissoFinanceiro` e como
  contato em `ParteFinanceiraController`.
- **Divisão de parcelas e vencimentos mensais**: `ParcelamentoCartaoService.dividir` (arredonda a
  última parcela para fechar exatamente o principal) e `.competencias` (gera vencimentos mensais
  preservando o dia). É a mesma lógica genérica já usada por compras parceladas no cartão de
  crédito — reutilizada aqui via injeção de `ParcelamentoCartaoService`, sem reescrever
  divisão/arredondamento de valores.
- **Fluxo financeiro**: cada recebimento gera exatamente um `LancamentoFinanceiro` de receita
  liquidado (`LancamentoFinanceiro.gerarDeEmprestimoConcedido`, nova origem
  `EMPRESTIMO_CONCEDIDO`), reaproveitando a mesma estrutura de contas/lançamentos usada por todo
  o módulo — nenhum saldo é calculado ou persistido em paralelo.

## Empréstimo, parcela e recebimento

- `EmprestimoConcedido`: a regra do empréstimo — devedor (`ParteFinanceira`, obrigatório),
  categoria (`CategoriaFinanceira` do tipo `RECEITA`, obrigatória — é ela que classifica os
  lançamentos gerados pelos recebimentos), descrição/finalidade opcional, valor principal, data de
  concessão, configuração de cobrança e forma de pagamento (única ou parcelada). Nunca movimenta
  saldo por si só.
- `ParcelaEmprestimo`: cada parcela concreta (inclusive a única, no caso de pagamento não
  parcelado — sempre `numero=1/totalParcelas=1`, sem um segundo mecanismo para esse caso). Contém
  número, valor principal, vencimento, data prometida (editável isoladamente, sem alterar o
  vencimento original), data efetiva de pagamento (a mais recente entre os recebimentos
  registrados), juros e multa (persistidos, recalculados a cada recebimento — nunca acumulados
  entre chamadas), valor efetivamente recebido (acumulado) e status.
- `RecebimentoParcelaEmprestimo`: liquidação integral ou parcial de uma parcela. Cada recebimento
  gera exatamente um `LancamentoFinanceiro`; exclusão física não é permitida — o estorno cancela
  esse mesmo lançamento e marca o recebimento como `ESTORNADO`, preservando o registro.

## Situações mínimas e o "status" persistido

`StatusParcelaEmprestimo` (persistido) tem quatro valores: `PENDENTE`, `PARCIALMENTE_PAGO`,
`PAGO`, `CANCELADO`. **Não existe um quinto status `ATRASADO` persistido** — atraso é sempre
derivado do vencimento e do saldo pendente no momento da consulta
(`ParcelaEmprestimo#estaAtrasada(LocalDate)`), o mesmo princípio já usado por
`OcorrenciaCompromisso#estaVencida`. A leitura HTTP expõe a situação efetiva de cinco valores
(`SituacaoParcelaEmprestimo`, incluindo `ATRASADO`) via `ParcelaEmprestimo#getSituacao(LocalDate)`,
que substitui `PENDENTE`/`PARCIALMENTE_PAGO` por `ATRASADO` quando o vencimento já passou e ainda
há saldo — sem exigir um job para transicionar status ao vencer (fora de escopo: "não iniciar o
simulador financeiro").

## Cálculo de juros e multa — `EncargosEmprestimoService`

Serviço de domínio isolado (não em controller, não nos services de aplicação) responsável por
calcular juros e multa de uma parcela, de acordo com `TipoCobrancaEmprestimo`:

| Configuração         | Juros                                                          | Multa                        |
|-----------------------|-----------------------------------------------------------------|-------------------------------|
| `SEM_JUROS`           | nunca                                                           | nunca                         |
| `COM_JUROS`           | pro-rata diária desde a **concessão**, mesmo antes do vencimento | nunca                          |
| `ALERTA_ATRASO`       | nunca (apenas sinaliza atraso via `estaAtrasada`)               | nunca                          |
| `MULTA_ATRASO`        | nunca                                                           | percentual único sobre o principal, só após o vencimento |
| `JUROS_MORA_ATRASO`   | pro-rata diária apenas sobre os dias **após o vencimento**       | nunca                          |

Taxa pro-rata: percentual mensal configurado, dividido por 30 dias, multiplicado pelos dias
aplicáveis — sempre em `BigDecimal`, arredondamento `HALF_UP`, escala final 2. O par
percentual/tipo é validado na própria entidade `EmprestimoConcedido` (não é possível configurar
`percentualJuros` fora de `COM_JUROS`/`JUROS_MORA_ATRASO`, nem `percentualMulta` fora de
`MULTA_ATRASO`), garantindo o critério "juros e multa só são calculados quando configurados" na
origem, não apenas no serviço de cálculo.

`RecebimentoParcelaEmprestimoService` recalcula (substitui, nunca acumula) os encargos vigentes
da parcela toda vez que um recebimento é registrado, usando a data do recebimento como
referência — dois recebimentos parciais em datas diferentes refletem, cada um, o encargo devido
até aquele momento.

## Migration `V14__criar_emprestimos_concedidos.sql`

Três tabelas novas (`emprestimo_concedido`, `parcela_emprestimo`,
`recebimento_parcela_emprestimo`), seguindo exatamente os padrões de auditoria, `CHECK` e índices
por `empresa_id` já usados nas migrations V11/V13. Único ajuste em tabela existente: o `CHECK
ck_lancamento_financeiro_origem` de `lancamento_financeiro` é recriado (drop + add, mesmo padrão
da V11) para aceitar a nova origem `EMPRESTIMO_CONCEDIDO`, preservando todas as origens
anteriores.

## Endpoints

- `POST /api/contexto/financeiro/emprestimos-concedidos` — cria o empréstimo e gera as parcelas.
- `GET /api/contexto/financeiro/emprestimos-concedidos[/{id}]` — lista/busca.
- `GET /api/contexto/financeiro/emprestimos-concedidos/{id}/parcelas` — parcelas do empréstimo.
- `POST /api/contexto/financeiro/emprestimos-concedidos/{id}/cancelar` — cancela o empréstimo e
  cascateia cancelamento para as parcelas ainda não pagas.
- `GET /api/contexto/financeiro/parcelas-emprestimo[/{id}]` — lista (filtros: `emprestimoId`,
  `status`, `atrasadas`)/busca.
- `GET /api/contexto/financeiro/parcelas-emprestimo/vencidas` — parcelas em atraso.
- `GET /api/contexto/financeiro/parcelas-emprestimo/proximas-vencimento?dias=` — parcelas a vencer.
- `GET /api/contexto/financeiro/parcelas-emprestimo/resumo` — saldo a receber, total recebido,
  total em aberto e contadores por situação.
- `PUT /api/contexto/financeiro/parcelas-emprestimo/{id}/data-prometida` — registra data
  prometida sem alterar o vencimento original.
- `GET .../{id}/recebimentos`, `POST .../{id}/receber-integral`, `POST .../{id}/receber-parcial`,
  `POST .../{id}/recebimentos/{recebimentoId}/estornar` — espelham exatamente os endpoints de
  pagamento de `OcorrenciaCompromissoController` (mesmas regras de perfil: escrita exige
  `ADMINISTRADOR`/`GESTOR`; estorno exige `ADMINISTRADOR`).

## Fora de escopo (deliberado)

- **Telas, CSS, JavaScript, fragmentos**: não alterados.
- **Simulador financeiro**: não iniciado.
- **Empréstimos tomados** (dívida da residência, não a favor dela): fora deste lote — o desenho
  conceitual (`MODELO-DE-DADOS.md`) já reserva isso para `CompromissoAPagar`/origem
  `EMPRESTIMO_RECEBIDO`, não tratado aqui.
- **Lançamento na concessão**: `EmprestimoConcedido` não gera `LancamentoFinanceiro` no momento em
  que o dinheiro é emprestado. O modelo conceitual (`INVARIANTES.md`, item 5) prevê que a
  concessão reduza o saldo da conta de origem sem contar como despesa — o que exigiria uma
  natureza de lançamento neutra (tipo "transferência") que `TipoFinanceiro` (hoje apenas
  `RECEITA`/`DESPESA`) ainda não modela. Registrar a concessão como `DESPESA` cometeria exatamente
  o erro que o invariante proíbe (distorcer o gasto real da residência). Como o escopo desta
  tarefa não pede explicitamente esse lançamento — apenas "registro dos recebimentos no fluxo
  financeiro" —, a concessão fica só nos campos `valorPrincipal`/`dataConcessao` da entidade;
  introduzir a natureza neutra de lançamento é trabalho de um lote futuro.

## Testes

- `EmprestimoConcedidoTests`, `ParcelaEmprestimoTests` (domínio puro, sem Spring): status,
  saldos, encargos, atraso, cancelamento, data prometida.
- `EncargosEmprestimoServiceTests` (domínio puro): as cinco configurações de cobrança, incluindo
  os casos de fronteira (no dia do vencimento vs. um dia depois).
- `EmprestimosConcedidosJpaTests` (`@DataJpaTest`): auditoria/UUID, cascata de parcelas,
  unicidade `(emprestimo_id, numero)` e `(lancamento_financeiro_id)`.
- `EmprestimosConcedidosControllerTests` (`@SpringBootTest` + `MockMvc`): cadastro único e
  parcelado (com verificação de arredondamento e vencimentos), rejeição de categoria de despesa,
  validação de percentuais por configuração de cobrança, cancelamento em cascata, recebimento
  integral/parcial, juros/multa aplicados apenas quando configurados, data prometida, parcelas
  vencidas, estorno (com reabertura do empréstimo quitado), perfis/CSRF/multiempresa.
- Suíte completa do projeto executada após a implementação: 644 testes, 0 falhas.
