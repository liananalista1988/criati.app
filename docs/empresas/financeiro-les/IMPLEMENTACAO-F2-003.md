# Implementação LES-F2-003 — Contas financeiras

## Diagnóstico e reaproveitamento

O módulo Financeiro já possuía `ContaFinanceira`, tabela `conta_financeira`, repository, service,
API em `/api/contexto/financeiro/contas`, página `/app/financeiro/contas` e referências de
`LancamentoFinanceiro`. Criar uma segunda entidade ou tela concorrente quebraria esse contrato.
Por isso, a entrega evolui o núcleo existente e preserva a rota, os UUIDs e as relações já usadas.

Os cálculos de saldo atual e lançamentos encontrados são anteriores a esta tarefa. Nenhum cálculo
ou lançamento foi criado pela LES-F2-003. A tela de contas passou a apresentar explicitamente
**saldo inicial**, e o campo legado `saldoAtual` foi mantido apenas no contrato da API para não
romper consumidores existentes.

## ContaFinanceira

Além dos campos anteriores, a conta passa a conter:

- titular `PessoaFinanceira`;
- instituição financeira opcional conforme o tipo;
- moeda armazenada como `BRL`;
- data de referência do saldo inicial;
- configuração `permiteConciliacao`;
- usuário de criação e atualização.

Toda conta nova ou editada pela API deve ter titular ativo da empresa atual, moeda BRL, saldo
inicial e data não futura. Saldo positivo, zero ou negativo é aceito e não cria lançamento.

Para preservar eventuais registros anteriores à V7, titular, instituição e usuários de auditoria
permanecem anuláveis no banco. O service torna titular e auditor obrigatórios nas novas operações.
A data de saldo dos registros legados é retropreenchida com a data de criação.

## Instituição financeira

`InstituicaoFinanceira` é um catálogo persistente, não um enum. Registros com `empresa_id` nulo
são globais; registros com empresa são complementos privados daquele tenant. A migration inclui
Banco do Brasil e Banco Inter como dados de referência globais, sem cadastrar contas ou dados de
usuários. Administradores podem cadastrar uma instituição adicional pela própria tela de contas.

Instituições globais e locais ativas podem ser vinculadas. Uma instituição pertencente a outra
empresa é tratada como não encontrada. Nomes já disponíveis globalmente ou na empresa não podem
ser duplicados no catálogo local.

## Tipos e moeda

Os tipos oferecidos para novas contas são `CONTA_CORRENTE`, `CONTA_PAGAMENTO`, `POUPANCA`,
`DINHEIRO`, `CARTEIRA` e `OUTRA`. `CAIXA` e `INVESTIMENTO` foram preservados no enum e na constraint
como tipos legados, evitando invalidar registros existentes.

Nesta etapa a única moeda aceita é `BRL`, armazenada explicitamente. Valores usam `BigDecimal` e
`NUMERIC(19,2)`; não há suporte multimoeda completo.

## Regras

- A empresa vem exclusivamente do contexto autenticado.
- Conta, titular e instituição são consultados no escopo permitido do tenant.
- Instituição é obrigatória para conta corrente, conta de pagamento, poupança e investimento.
- Instituição é opcional para dinheiro, carteira, caixa legado e outra.
- Dinheiro e carteira não podem habilitar conciliação.
- Datas futuras são rejeitadas, sem confirmação implícita.
- Contas inativas não aparecem na listagem padrão e podem ser reativadas.
- Não existe exclusão física comum.
- Nomes repetidos são permitidos; a resposta e a interface alertam sobre possível duplicidade.
- Alterar tipo, saldo inicial ou data de uma conta que já possui lançamentos continua bloqueado
  para preservar o histórico anterior.

Transferências futuras não deverão alterar o total consolidado da residência nem duplicar contas.
Essas regras foram apenas documentadas; transferências não fazem parte desta entrega.

## Conciliação futura

`permiteConciliacao` é somente uma configuração. Contas bancárias iniciam marcadas na interface;
dinheiro e carteira usam `false`. Não foram implementados OFX, CSV, planilha, PDF, importação ou
conciliação manual.

## Migration

`V7__evoluir_contas_financeiras.sql`:

- cria `instituicao_financeira` e seus catálogos globais/locais;
- acrescenta titular, instituição, moeda, data do saldo, conciliação e auditoria à conta existente;
- amplia a constraint de tipos sem remover valores legados;
- cria FKs e índices por empresa, titular, instituição, nome e conciliação;
- preserva registros existentes e não insere contas reais.

## APIs

- `GET /api/contexto/financeiro/contas` — filtros `status`, `tipo`, `titularId`, `instituicaoId` e `busca`;
- `GET /api/contexto/financeiro/contas/{id}`;
- `POST /api/contexto/financeiro/contas`;
- `PUT /api/contexto/financeiro/contas/{id}`;
- `POST /api/contexto/financeiro/contas/{id}/inativar`;
- `POST /api/contexto/financeiro/contas/{id}/reativar`;
- `GET /api/contexto/financeiro/contas/titulares`;
- `GET /api/contexto/financeiro/contas/instituicoes`;
- `POST /api/contexto/financeiro/contas/instituicoes`;
- `GET /api/contexto/financeiro/contas/resumo`.

Leitura reutiliza a autorização existente do Financeiro. Escrita exige `ADMINISTRADOR` e CSRF.

## Tela

A página existente `/app/financeiro/contas` foi evoluída. Ela apresenta filtros, detalhes,
cadastro/edição, instituição local, ativação/desativação, estado vazio e resumo de quantidade de
contas ativas, soma dos saldos iniciais e saldo inicial por titular. O texto deixa explícito que
saldo inicial não considera movimentações futuras.

A navegação já apontava para essa página e não foi duplicada. Tema, sidebar, topbar, responsividade
e estilos Criati/Windows/Compacto continuam fornecidos pelos componentes globais existentes.

## Segurança, auditoria e testes

Services validam empresa ativa, aplicação Financeiro, perfil, titular, instituição e recurso por
`id + empresa`. UUID de outro tenant não revela a existência do registro. Criação, edição e mudança
de status atualizam datas e usuário responsável. Como a auditoria completa ainda não existe, não
foi criado sistema paralelo para armazenar versões de cada campo.

Testes MockMvc e JPA cobrem valores positivos/zero/negativos, validações, tipos, moeda, titular e
instituição, isolamento, filtros, resumo inicial, duplicidade não bloqueante, auditoria, status,
autenticação, autorização, CSRF, página e compatibilidade com lançamentos existentes.

## Limitações e próximas etapas

- Não há homologação visual completa sem navegador real.
- Registros legados podem continuar sem titular/auditor até serem regularizados por edição.
- Não há histórico versionado dos valores anteriores, apenas auditoria de responsável e instante.
- Não há conta conjunta, multimoeda, saldo por movimentação novo, transferência ou conciliação.
- A base está pronta para referências de conta e titular nas próximas tarefas, sem antecipar
  lançamentos, cartões, contas a pagar ou transferências.
