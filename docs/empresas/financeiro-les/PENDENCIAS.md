# Pendências — Financeiro LeS

Decisões e dados que **dependem de confirmação futura** antes ou durante a modelagem de dados e
das regras de negócio. Nenhum valor foi inventado neste documento nem em `VISAO-FUNCIONAL.md`,
`MVP.md` ou `GLOSSARIO.md` — onde o contexto da tarefa não trouxe um dado concreto, ele está
registrado aqui como pendente, não preenchido com um exemplo fictício apresentado como real.

## Dados dos cartões de crédito

- Limite bancário de cada um dos 4 cartões iniciais — não informado.
- Limite saudável desejado para cada cartão — não informado (a família definirá um valor por
  cartão; o exemplo "R$ 12.000 / R$ 2.500" usado no contexto da tarefa é ilustrativo, não um
  dado real de nenhum cartão específico).
- Dia de fechamento e dia de vencimento de cada cartão — não informado.
- Banco emissor, titular e bandeira de cada um dos 4 cartões — não informado.
- Quais dos 4 cartões (se algum) são virtuais vinculados a um cartão físico principal — não
  informado.
- Se e quando o número de cartões será reduzido (mencionado como possibilidade futura, sem
  cartão ou prazo definido).

## Dados das contas bancárias

- Saldo inicial de cada uma das 4 contas (Pessoa 1 — BB, Pessoa 1 — Inter, Pessoa 2 — BB,
  Pessoa 2 — Inter) — não informado.
- Se haverá conta em dinheiro em espécie cadastrada desde o início, mesmo não sendo prioridade.

## Dados dos usuários

- Nomes reais das duas pessoas (o contexto usa apenas "Pessoa 1" e "Pessoa 2") — não informado.
- E-mails que serão usados para o login de cada pessoa — não informado.

## Valores recorrentes

- Valor do PIX recorrente do pagamento do apartamento — não informado.
- Valor e periodicidade de cada conta a pagar recorrente conhecida (condomínio, energia, água,
  internet, plano de saúde etc.) — não informado.
- Valor e periodicidade de cada assinatura ativa hoje — não informado.
- Salário de cada pessoa, data de recebimento e conta de destino — não informado (mencionado
  apenas que cada pessoa recebe o salário na própria conta).

## Metas

- Se 7% da renda mensal é a única meta de economia inicial ou se já existem outras metas
  financeiras nomeadas (reduzir fatura, quitar dívida específica, formar reserva com valor-alvo)
  — não informado além do percentual de 7%.
- Prazo desejado para qualquer meta financeira além da meta de economia — não informado.

## Histórico financeiro disponível

- Se existe histórico de lançamentos, faturas ou extratos anteriores à criação do sistema que
  deverá ser importado/cadastrado retroativamente, e a partir de qual data — não informado.
- Empréstimos ou compromissos já em andamento hoje (fora do cartão) que precisarão ser
  cadastrados como saldo inicial — não informado além da previsão funcional de que o sistema deve
  suportar esse cadastro.

## Decisão de arquitetura — RESOLVIDA (LES-F1-003)

- **Reaproveitar o módulo `FINANCEIRO` genérico ou construir um módulo novo dedicado.** Recomendado
  na LES-F1-002 (Opção C) e **confirmado como decisão** na LES-F1-003: núcleo financeiro
  compartilhado (conta, categoria, lançamento simples, pessoa, favorecido, anexo, recorrência)
  com extensões específicas do Financeiro LeS (cartão, compra no crédito, fatura, parcela,
  orçamento, meta de economia, empréstimos, recebíveis, conciliação, simulador). Ver
  `ARQUITETURA-FUNCIONAL.md`, seção 1, e o registro correspondente em `docs/DECISOES.md`,
  seção "Financeiro LeS — núcleo compartilhado com extensões". Removida desta lista de pendências
  por já estar decidida (ainda não implementada — a implementação é escopo de uma etapa futura).

## Dúvidas de regra da LES-F1-002 — RESOLVIDAS na LES-F1-003

As seis dúvidas abaixo, registradas na LES-F1-002, foram decididas nesta tarefa
(`ARQUITETURA-FUNCIONAL.md`, seções 6, 7, 11, 12, 13 e 14) — mantidas aqui apenas como referência
histórica de que já têm decisão registrada, não mais como pendência aberta:

- **Antecipação de parcela com desconto de juros**: decidido — no MVP, permite informar o valor
  pago e um desconto manual, sem recálculo automático de contratos complexos
  (`ARQUITETURA-FUNCIONAL.md`, seção 13).
- **Prazo de retenção da lixeira**: decidido — sem prazo automático no MVP; exclusão definitiva é
  sempre manual (`ARQUITETURA-FUNCIONAL.md`, seção 11).
- **Recebimento/pagamento superior ao saldo**: decidido — exige classificação explícita do
  excedente (juros, multa, adiantamento ou valor não identificado), nunca aceito
  automaticamente (`ARQUITETURA-FUNCIONAL.md`, seção 12).
- **Janela da média histórica**: decidido — últimos três meses completos, nunca o mês corrente
  (`ARQUITETURA-FUNCIONAL.md`, seção 6).
- **Margem de segurança do risco de pagamento parcial da fatura** e **ritmo de "meta em risco"**:
  critérios objetivos propostos e confirmados em `ARQUITETURA-FUNCIONAL.md`, seção 7 — os
  percentuais exatos (80%/100%) ficam registrados como configuráveis no futuro, não fixos no
  código, mas **não são mais uma pendência de definição de critério**, apenas um detalhe de
  ajuste fino a validar com o uso real.
- **Economia como meta projetada ou transferência real**: decidido — meta projetada no MVP, sem
  transferência obrigatória (`ARQUITETURA-FUNCIONAL.md`, seção 14).

## Pendências que permanecem (dados concretos, ainda não informados)

Nenhuma pendência de dado concreto foi resolvida nesta tarefa — são decisões que só a família
pode fornecer, não decisões de arquitetura ou de regra. Mantidas integralmente:

### Dados dos cartões de crédito

- Limite bancário de cada um dos 4 cartões iniciais — não informado.
- Limite saudável desejado para cada cartão — não informado (a família definirá um valor por
  cartão; o exemplo "R$ 12.000 / R$ 2.500" usado no contexto da tarefa é ilustrativo, não um
  dado real de nenhum cartão específico).
- Dia de fechamento e dia de vencimento de cada cartão — não informado.
- Banco emissor, titular e bandeira de cada um dos 4 cartões — não informado.
- Quais dos 4 cartões (se algum) são virtuais vinculados a um cartão físico principal — não
  informado.
- Se e quando o número de cartões será reduzido (mencionado como possibilidade futura, sem
  cartão ou prazo definido).

### Dados das contas bancárias

- Saldo inicial de cada uma das 4 contas (Pessoa 1 — BB, Pessoa 1 — Inter, Pessoa 2 — BB,
  Pessoa 2 — Inter) — não informado.
- Se haverá conta em dinheiro em espécie cadastrada desde o início, mesmo não sendo prioridade.

### Dados dos usuários

- Nomes reais das duas pessoas (o contexto usa apenas "Pessoa 1" e "Pessoa 2") — não informado.
- E-mails que serão usados para o login de cada pessoa — não informado.

### Valores recorrentes

- Valor do PIX recorrente do pagamento do apartamento — não informado.
- Valor e periodicidade de cada conta a pagar recorrente conhecida (condomínio, energia, água,
  internet, plano de saúde etc.) — não informado.
- Valor e periodicidade de cada assinatura ativa hoje — não informado.
- Salário de cada pessoa, data de recebimento e conta de destino — não informado (mencionado
  apenas que cada pessoa recebe o salário na própria conta).

### Metas

- Se 7% da renda mensal é a única meta de economia inicial ou se já existem outras metas
  financeiras nomeadas (reduzir fatura, quitar dívida específica, formar reserva com valor-alvo)
  — não informado além do percentual de 7%.
- Prazo desejado para qualquer meta financeira além da meta de economia — não informado.

### Histórico financeiro disponível

- Se existe histórico de lançamentos, faturas ou extratos anteriores à criação do sistema que
  deverá ser importado/cadastrado retroativamente, e a partir de qual data — não informado.
- Empréstimos ou compromissos já em andamento hoje (fora do cartão) que precisarão ser
  cadastrados como saldo inicial — não informado além da previsão funcional de que o sistema deve
  suportar esse cadastro.

## Novas pendências identificadas na modelagem (LES-F1-003)

- **Nome exato do código de aplicação da extensão** no catálogo (`EmpresaAplicacao`) — proposto
  como `FINANCEIRO_RESIDENCIAL` em `ARQUITETURA-FUNCIONAL.md`, mas o nome definitivo é uma
  decisão técnica da etapa de modelagem física, não desta tarefa.
- **Conciliação entre `TipoContaFinanceira` (enum já existente) e os novos tipos de conta**
  propostos (`CONTA_PAGAMENTO`, `DINHEIRO`, `CARTEIRA`) — `MODELO-DE-DADOS.md` registra que é uma
  decisão de modelagem técnica futura, não resolvida aqui.
- **Critério de "meses sem o dia configurado"** em uma recorrência (ex.: dia 31 em mês de 30
  dias) — proposto usar o último dia válido do mês (`MODELO-DE-DADOS.md`, seção "Recorrência"),
  mas ainda a confirmar com a família na modelagem técnica.

## Documentos relacionados

- `VISAO-FUNCIONAL.md`, `MVP.md`, `GLOSSARIO.md` (LES-F1-001).
- `PROCESSOS.md`, `REGRAS-DE-NEGOCIO.md`, `ESTADOS-E-TRANSICOES.md`, `CALCULOS-E-INDICADORES.md`
  (LES-F1-002).
- `MODELO-DE-DADOS.md`, `ARQUITETURA-FUNCIONAL.md`, `INVARIANTES.md`,
  `MATRIZ-ENTIDADES-PROCESSOS.md` (LES-F1-003).
