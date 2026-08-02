# Financeiro — Criati

MVP do Gerenciador Financeiro (aplicação `FINANCEIRO`, produto real da Criati — ver `docs/DECISOES.md`, seção "Catálogo de aplicações e Clínica Vida Demo"). Este documento registra o modelo, as regras de negócio, o cálculo de saldo, os endpoints, as telas, o isolamento multiempresa e as limitações desta fase.

## Modelo

Três entidades, todas escopadas por `empresa_id`, todas com auditoria temporal (`criadoEm`/`atualizadoEm`) e todas sujeitas ao mesmo princípio: **nenhuma exclusão física**.

### ContaFinanceira (`conta_financeira`)

```text
id, empresa, nome, tipo, saldoInicial (BigDecimal, escala 2), status, criadoEm, atualizadoEm
```

`tipo` (`TipoContaFinanceira`): `CAIXA`, `CONTA_CORRENTE`, `POUPANCA`, `INVESTIMENTO`, `OUTRA`.

Regras: nome obrigatório; nome único por empresa entre contas **ativas** (verificado no serviço, não por constraint de banco — uma constraint `UNIQUE` incondicional impediria reaproveitar um nome depois de inativar a conta antiga); `saldoInicial` pode ser positivo, zero ou negativo; editar (nome/tipo/saldoInicial) só é permitido enquanto a conta não tiver nenhum lançamento (`ContaFinanceiraComLancamentosException`, `409`) — depois do primeiro lançamento, alterar `saldoInicial` invalidaria o histórico de saldo já calculado; inativar/reativar não são idempotentes (repetir a mesma transição responde `409`, mesmo padrão de `UsuarioEmpresa.suspender/reativar`); uma conta inativa nunca aceita novo lançamento.

### CategoriaFinanceira (`categoria_financeira`)

```text
id, empresa, nome, tipo, status, criadoEm, atualizadoEm
```

`tipo`: `TipoFinanceiro` (`RECEITA`/`DESPESA`) — o mesmo enum usado pelo tipo do lançamento (ver "Decisão: um único enum `TipoFinanceiro`" abaixo).

Regras: nome obrigatório; nome único por empresa **e tipo** entre categorias ativas (a mesma empresa pode ter uma categoria de receita e uma de despesa com o mesmo nome, mas não duas categorias de despesa "Aluguel"); editar segue a mesma regra de contas (bloqueado quando já existe lançamento); uma categoria inativa nunca aceita novo lançamento; uma categoria de receita nunca pode ser usada em um lançamento de despesa e vice-versa (`DadosInvalidosException`, `400`, verificado a cada criação/edição de lançamento).

### LancamentoFinanceiro (`lancamento_financeiro`)

```text
id, empresa, conta, categoria, tipo, descricao, valor (BigDecimal, escala 2),
dataCompetencia, dataPagamento (nula se PENDENTE), status, observacao (opcional),
criadoEm, atualizadoEm
```

`status` (`StatusLancamentoFinanceiro`): `PENDENTE`, `PAGO`, `CANCELADO`.

Regras: `valor` sempre maior que zero (`ck_lancamento_financeiro_valor_positivo` no banco **e** validação no serviço, defesa em profundidade); `tipo` deve ser igual ao `tipo` da categoria informada; `conta` e `categoria` sempre pertencem à mesma empresa do contexto ativo (nunca a outra empresa — verificado com `findByIdAndEmpresaId`, nunca `findById` isolado); `descricao` e `dataCompetencia` obrigatórias; `dataPagamento` obrigatória quando `status=PAGO`, nula quando `PENDENTE`; nunca é possível criar um lançamento já `CANCELADO`; cancelar preserva o registro (nunca há `DELETE` físico) — o histórico do lançamento cancelado continua consultável.

Transições de status, todas idempotentes-negativas (repetir responde `409`, `LancamentoStatusInvalidoException`):

```text
PENDENTE --pagar(dataPagamento)--> PAGO
PAGO     --reabrir()-------------> PENDENTE (limpa dataPagamento)
PENDENTE ou PAGO --cancelar()----> CANCELADO
```

Edição (`PUT`) só é permitida quando o lançamento está `PENDENTE` — um lançamento `PAGO` precisa ser reaberto antes de editar (regra segura explícita: evita alterar valor/conta/categoria/competência de um lançamento que já compôs o saldo realizado); um lançamento `CANCELADO` nunca pode ser editado.

### Decisão: um único enum `TipoFinanceiro`

A tarefa original previa dois enums (`TipoCategoriaFinanceira` e `TipoLancamentoFinanceiro`), mas ambos teriam exatamente os mesmos dois valores (`RECEITA`/`DESPESA`) e precisam ser comparados por igualdade na regra de compatibilidade categoria↔lançamento. Manter dois enums idênticos duplicaria a mesma informação sem nenhum ganho de clareza; um único `TipoFinanceiro` (em `br.app.criati.shared.enums`) é usado tanto por `CategoriaFinanceira.tipo` quanto por `LancamentoFinanceiro.tipo`.

## Cálculo de saldo

Nunca persistido — sempre derivado por consulta, fonte única em `SaldoFinanceiroService`, reaproveitado tanto pela tela de contas quanto pelo dashboard (evita duas fórmulas divergentes para o mesmo número).

```text
saldo atual de uma conta = saldoInicial
                          + soma dos lançamentos RECEITA com status PAGO dessa conta
                          - soma dos lançamentos DESPESA com status PAGO dessa conta
```

`PENDENTE` nunca entra no saldo realizado; `CANCELADO` nunca entra em nenhum total monetário.

Saldo consolidado (dashboard) = soma do saldo atual de **todas** as contas da empresa, ativas e inativas — uma conta inativa preserva seu saldo histórico (decisão explícita, seguindo a preferência registrada na tarefa: "conta inativa mantém saldo histórico").

## Dashboard e competência

`GET /api/contexto/financeiro/dashboard?competencia=AAAA-MM` (padrão: mês corrente se omitido).

- `receitasPagas`, `despesasPagas`, `resultadoMes`, `resumoPorCategoria` e `quantidadeLancamentosPeriodo`: escopados à **competência** selecionada (campo `dataCompetencia`, não `dataPagamento`) — refletem o que foi realizado (`PAGO`) dentro daquele mês contábil.
- `totalPendenteReceber`/`totalPendentePagar`: **não** escopados à competência — representam tudo que ainda está em aberto agora, independentemente de quando foi lançado (um título pendente de um mês anterior continua aparecendo até ser pago ou cancelado). Decisão deliberada: um dashboard que escondesse pendências antigas dentro do filtro de mês esconderia o que mais importa operacionalmente.
- `saldoAtualConsolidado`: sempre o saldo real agora (todas as contas, todo o histórico), independente da competência selecionada.
- `ultimosLancamentos`: os 10 lançamentos mais recentes da empresa (qualquer competência, qualquer status, incluindo cancelados — mostrados com o selo de status correto), como feed de atividade.
- `CANCELADO` nunca soma em nenhum campo monetário.

## Permissões

O modelo de perfil por empresa já existente (`PerfilUsuario`: `ADMINISTRADOR`/`GESTOR`/`USUARIO`) foi reaproveitado sem nenhuma mudança arquitetural — nenhuma role nova foi criada (`ROLE_FINANCEIRO`/`ROLE_CLINICA` seguem inexistentes, ver `docs/DECISOES.md`).

| Ação | ADMINISTRADOR | GESTOR | USUARIO |
| --- | --- | --- | --- |
| Listar/consultar contas, categorias, lançamentos, dashboard | sim | sim | sim |
| Criar/editar/inativar/reativar conta ou categoria | sim | não | não |
| Criar/editar lançamento | sim | sim | não |
| Pagar lançamento | sim | sim | não |
| Cancelar lançamento | sim | sim | não |
| Reabrir lançamento (PAGO → PENDENTE) | sim | não | não |

`reabrir` é reservado ao ADMINISTRADOR porque desfaz um pagamento já registrado — decisão mais sensível que simplesmente marcar como pago. Um Superadministrador sem vínculo empresarial (`UsuarioEmpresa`) nunca acessa o módulo: `ContextoEmpresaService.exigirContextoAtivo` já falha antes de qualquer checagem de perfil ou de aplicação, pois não existe vínculo a validar.

## Controle de acesso ao módulo

Toda rota de API sob `/api/contexto/financeiro/**` passa por `ContextoFinanceiroService.exigirAcesso`, que exige, nesta ordem: usuário autenticado; empresa ativa selecionada na sessão; vínculo (`UsuarioEmpresa`) ativo; aplicação `FINANCEIRO` habilitada (`EmpresaAplicacao`) para a empresa ativa. As páginas (`/app/financeiro/**`) repetem a mesma checagem no backend (`PaginaController`), nunca dependendo apenas do menu lateral — sem contexto válido ou sem `FINANCEIRO` habilitado, a página redireciona para `/app/aplicacoes` (mesma resposta genérica para os dois motivos, para não revelar qual condição falhou).

## Endpoints

Todos exigem sessão autenticada, contexto de empresa ativa e `FINANCEIRO` habilitado; nenhum recebe `empresaId` — a empresa vem exclusivamente do contexto da sessão. Toda operação que muda estado exige CSRF (padrão já existente no projeto).

```text
GET    /api/contexto/financeiro/contas                       (todos os perfis; filtros status, tipo)
POST   /api/contexto/financeiro/contas                       (ADMINISTRADOR)
GET    /api/contexto/financeiro/contas/{id}                  (todos os perfis)
PUT    /api/contexto/financeiro/contas/{id}                  (ADMINISTRADOR; bloqueado se houver lancamentos)
POST   /api/contexto/financeiro/contas/{id}/inativar         (ADMINISTRADOR)
POST   /api/contexto/financeiro/contas/{id}/reativar         (ADMINISTRADOR)

GET    /api/contexto/financeiro/categorias                   (todos os perfis; filtros status, tipo)
POST   /api/contexto/financeiro/categorias                   (ADMINISTRADOR)
GET    /api/contexto/financeiro/categorias/{id}               (todos os perfis)
PUT    /api/contexto/financeiro/categorias/{id}               (ADMINISTRADOR; bloqueado se houver lancamentos)
POST   /api/contexto/financeiro/categorias/{id}/inativar      (ADMINISTRADOR)
POST   /api/contexto/financeiro/categorias/{id}/reativar      (ADMINISTRADOR)

GET    /api/contexto/financeiro/lancamentos                   (todos os perfis; filtros data inicial/final, tipo, status, conta, categoria, busca)
GET    /api/contexto/financeiro/lancamentos/pagina            (todos os perfis; paginação, ordenação e filtros por coluna usados pela tela)
POST   /api/contexto/financeiro/lancamentos                   (ADMINISTRADOR, GESTOR)
GET    /api/contexto/financeiro/lancamentos/{id}               (todos os perfis)
PUT    /api/contexto/financeiro/lancamentos/{id}               (ADMINISTRADOR, GESTOR; somente se PENDENTE)
POST   /api/contexto/financeiro/lancamentos/{id}/pagar         (ADMINISTRADOR, GESTOR; PENDENTE->PAGO)
POST   /api/contexto/financeiro/lancamentos/{id}/reabrir       (ADMINISTRADOR; PAGO->PENDENTE)
POST   /api/contexto/financeiro/lancamentos/{id}/cancelar      (ADMINISTRADOR, GESTOR)

GET    /api/contexto/financeiro/dashboard                     (todos os perfis; parametro opcional competencia=AAAA-MM)
```

Nenhum endpoint `DELETE` existe neste módulo.

## Páginas

```text
GET /app/financeiro              dashboard financeiro (cards, resumo por categoria, ultimos lancamentos)
GET /app/financeiro/contas        listar/criar/editar/inativar/reativar contas
GET /app/financeiro/categorias     listar/criar/editar/inativar/reativar categorias (receitas e despesas separadas)
GET /app/financeiro/lancamentos    listar com filtros por coluna, paginação e ordenação; criar receita/despesa; editar; pagar; reabrir; cancelar
```

Todas autenticadas, exigem empresa ativa e `FINANCEIRO` habilitado (redirecionam para `/app/aplicacoes` caso contrário). Interface em Thymeleaf + CSS/JS puro (mesmo padrão do restante do projeto, sem framework frontend e sem biblioteca de gráficos): o resumo por categoria usa barras de progresso CSS simples (`width` proporcional), não uma biblioteca de gráficos.

JavaScript, todo em `src/main/resources/static/js/`, reaproveitando `criati-api.js` (cliente HTTP e CSRF já existentes, nunca duplicado):

```text
financeiro-api.js          monta as URLs do modulo sobre CriatiApi.get/post/request
financeiro-formatacao.js   moeda BRL (Intl.NumberFormat) e datas pt-BR
financeiro-dashboard.js    cards, resumo por categoria, ultimos lancamentos
financeiro-contas.js       tela de contas
financeiro-categorias.js   tela de categorias
financeiro-lancamentos.js  tela de lancamentos (filtros, criar, editar, pagar, reabrir, cancelar)
```

## Tratamento monetário

`BigDecimal` em toda a pilha (entidade, serviço, JSON de resposta — nunca `double`/`float`); coluna `NUMERIC(19,2)` no banco; normalização de escala centralizada em `MoedaUtils.normalizar` (escala 2, `RoundingMode.HALF_UP`) aplicada em toda entrada monetária antes de persistir. A formatação BRL (`R$ 1.234,56`) acontece exclusivamente no JavaScript da interface — a API sempre trafega números puros.

## Isolamento multiempresa

Toda consulta financeira é obrigatoriamente filtrada por `empresa_id` do contexto ativo da sessão — nunca por um identificador isolado. Um UUID de conta/categoria/lançamento pertencente a outra empresa responde exatamente como um UUID inexistente (`404`, mesma mensagem), sem revelar a existência do recurso em outra empresa. Nenhuma regra do módulo depende do nome ou do CNPJ da empresa.

## Limitações desta fase (fora do escopo do MVP)

Registradas como evoluções futuras, não implementadas nesta tarefa: conciliação bancária, integração bancária automática, boleto, Pix, contas a pagar recorrentes complexas, centro de custo, fluxo de caixa projetado, cobrança, emissão fiscal, importação de planilha, anexos e aprovação em múltiplas etapas. A fundação de importação manual OFX existe apenas como área de preparação segura, descrita em `docs/empresas/financeiro-les/IMPLEMENTACAO-CRIATI-FIN-014-OFX.md`; ela não categoriza, concilia ou gera lançamentos.

## Evolução LES-F2-005 — lançamentos financeiros básicos

A LES-F2-005 evolui o lançamento existente sem criar entidade, tabela, rota-base ou página paralela. Os conceitos de competência, vencimento e liquidação permanecem separados; somente lançamentos `LIQUIDADO` (ou `PAGO`, preservado para compatibilidade legada) compõem o saldo derivado. `VENCIDO` é calculado para lançamentos pendentes com vencimento anterior à data atual e não é persistido.

Novos lançamentos vinculam pessoa responsável, admitem parte financeira opcional e têm origem `MANUAL`. A conta permanece obrigatória também no estado pendente, decisão conservadora que mantém o contrato anterior e garante que toda liquidação tenha destino definido. A forma de pagamento é opcional; `CREDITO` existe apenas para leitura de dados legados e não implementa cartão ou fatura.

Além dos endpoints legados de pagamento e reabertura, a API oferece as ações explícitas `liquidar` e `desliquidar` e um resumo da competência com valores liquidados, pendentes, vencidos, saldos por conta e consolidado. Cancelar um lançamento liquidado limpa os dados de liquidação na mesma transação; como o saldo é calculado a partir dos lançamentos efetivos, o impacto é revertido sem saldo mutável ou lançamento contábil paralelo.

O inventário completo, as regras de compatibilidade, a migration `V9` e as limitações estão registrados em `docs/empresas/financeiro-les/IMPLEMENTACAO-F2-005.md`.

## Evolução LES-F3-004 — faturas de cartão

Faturas são únicas por empresa, cartão físico principal e competência. Parcelas de compras feitas
no principal ou em seus cartões virtuais são associadas idempotentemente à mesma fatura e somadas
com `BigDecimal`. Abertura, recomposição e fechamento usam locks pessimistas; fatura fechada é
imutável. Pagamentos, estornos, cancelamentos e tela permanecem fora desta entrega.

O contrato completo e a migration `V18` estão registrados em
`docs/empresas/financeiro-les/IMPLEMENTACAO-F3-004.md`.
