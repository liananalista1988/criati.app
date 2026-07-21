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

## Decisão de arquitetura (identificada durante esta análise)

- **Reaproveitar o módulo `FINANCEIRO` genérico já existente na plataforma (`docs/FINANCEIRO.md`)
  como base do Financeiro LeS, ou construir um módulo novo dedicado.** O módulo genérico atual
  cobre apenas contas, categorias, lançamentos e um dashboard simples; a visão funcional do
  Financeiro LeS exige cartões de crédito, faturas, parcelas, contas a pagar recorrentes,
  empréstimos, simulador, orçamento, meta de economia, conciliação e categorização automática —
  nenhum desses recursos existe hoje no módulo genérico (todos estão listados em
  `docs/FINANCEIRO.md`, seção "Limitações desta fase", como evolução futura). Esta é uma decisão
  de arquitetura relevante que afeta diretamente a modelagem de dados da próxima etapa e **não
  foi tomada nesta tarefa** — está registrada aqui, não em `docs/DECISOES.md`, por ainda não ter
  sido decidida (`docs/DECISOES.md` registra decisões já tomadas, ver sua própria "Regra de
  alteração").

## Documentos relacionados

- `VISAO-FUNCIONAL.md`
- `MVP.md`
- `GLOSSARIO.md`
