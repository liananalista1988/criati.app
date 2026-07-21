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

## Decisão de arquitetura (identificada na LES-F1-001, agora com recomendação)

- **Reaproveitar o módulo `FINANCEIRO` genérico já existente na plataforma (`docs/FINANCEIRO.md`)
  como base do Financeiro LeS, ou construir um módulo novo dedicado.** O módulo genérico atual
  cobre apenas contas, categorias, lançamentos e um dashboard simples; a visão funcional do
  Financeiro LeS exige cartões de crédito, faturas, parcelas, contas a pagar recorrentes,
  empréstimos, simulador, orçamento, meta de economia, conciliação e categorização automática —
  nenhum desses recursos existe hoje no módulo genérico (todos estão listados em
  `docs/FINANCEIRO.md`, seção "Limitações desta fase", como evolução futura). **Atualização
  (LES-F1-002)**: uma recomendação foi elaborada — Opção C, núcleo compartilhado (conta,
  categoria, lançamento simples) com extensões específicas para o uso residencial — ver
  `REGRAS-DE-NEGOCIO.md`, seção "Recomendação sobre o módulo `FINANCEIRO` existente". Continua
  registrada aqui, e não em `docs/DECISOES.md`, porque é uma **recomendação**, não uma decisão
  aprovada e implementada (`docs/DECISOES.md` registra apenas decisões já tomadas, ver sua
  própria "Regra de alteração"). Esta pendência só deve ser removida daqui e migrada para
  `docs/DECISOES.md` quando a recomendação for formalmente aprovada e, de fato, implementada.

## Dúvidas de regra identificadas durante o detalhamento de processos (LES-F1-002)

- **Antecipação de parcela com desconto de juros**: quando uma compra parcelada tiver juros do
  próprio parcelamento (não apenas juros por atraso), não está definido se antecipar uma parcela
  deve gerar desconto proporcional desses juros ou se o valor da parcela permanece fixo mesmo
  antecipado (`PROCESSOS.md`, seção 6).
- **Prazo de retenção da lixeira**: não está definido se existirá um prazo automático após o
  qual um item na lixeira é excluído definitivamente sem ação manual, ou se a exclusão definitiva
  será sempre uma ação explícita da família, sem prazo (`PROCESSOS.md`, seção 18).
- **Recebimento maior que o saldo a receber**: não está definido como o sistema deve tratar o
  valor excedente quando um recebimento (de empréstimo concedido ou de compra para terceiro) é
  maior que o saldo devido registrado — se deve virar um novo crédito, uma devolução, ou exigir
  correção manual antes de ser aceito (`REGRAS-DE-NEGOCIO.md`, casos extremos).
- **Janela da média histórica**: para o indicador de variação mensal comparado "contra a média
  histórica" (`VISAO-FUNCIONAL.md`), não está definido quantos meses entram nessa média (últimos
  3, 6, 12 meses, ou desde o início do uso do sistema) — `CALCULOS-E-INDICADORES.md`.
- **Margem de segurança do risco de pagamento parcial da fatura**: o critério proposto em
  `CALCULOS-E-INDICADORES.md` para classificar uma fatura como "atenção" depende de uma margem de
  segurança percentual ainda não validada com a família.
- **Ritmo de "meta em risco"**: não está definido a partir de que ponto do mês (ex.: metade do
  período decorrido) e com que critério de ritmo de gasto o sistema deve classificar a meta de
  economia como "em risco" antes do fechamento do período (`CALCULOS-E-INDICADORES.md`).
- **Economia como meta projetada ou transferência real**: não está definido se atingir a meta de
  economia deve ser apenas um cálculo/acompanhamento (a diferença permanece nas contas normais)
  ou se deve envolver a transferência de fato de um valor para uma conta específica de reserva,
  ou ambos em fases diferentes (`PROCESSOS.md`, seção 14, já registrado na LES-F1-001 e mantido
  aqui por continuar sem resposta).

## Documentos relacionados

- `VISAO-FUNCIONAL.md`, `MVP.md`, `GLOSSARIO.md` (LES-F1-001).
- `PROCESSOS.md`, `REGRAS-DE-NEGOCIO.md`, `ESTADOS-E-TRANSICOES.md`, `CALCULOS-E-INDICADORES.md`
  (LES-F1-002).
