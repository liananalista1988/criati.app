# Processos — Financeiro LeS

Fluxos operacionais do Financeiro LeS, em nível de processo de negócio (não técnico). Complementa
`VISAO-FUNCIONAL.md` (o quê) e `MVP.md` (o que entra em cada fase) sem repetir o conteúdo de
nenhum dos dois — aqui documenta-se **como** cada fluxo acontece, passo a passo, e onde estão as
decisões e validações. Regras específicas citadas aqui em caixas "Regra obrigatória" estão
consolidadas, junto com as demais, em `REGRAS-DE-NEGOCIO.md`. Estados citados aqui remetem às
tabelas completas em `ESTADOS-E-TRANSICOES.md`. Fórmulas citadas remetem a `CALCULOS-E-INDICADORES.md`.

## 1. Cadastro inicial da empresa

Ordem de entrada de dados na primeira configuração:

| # | Etapa | Obrigatório para operar | Pode ser preenchido depois |
| --- | --- | --- | --- |
| 1 | Criação do tenant `Financeiro LeS` | Sim | — |
| 2 | Criação ou convite dos dois usuários | Sim (ao menos 1 para acessar; o 2º pode ser convidado depois) | O 2º usuário pode entrar depois |
| 3 | Vinculação dos usuários à empresa | Sim, para cada usuário que for operar | — |
| 4 | Definição do mesmo nível de acesso | Sim (é a regra do produto, não uma escolha por usuário) | — |
| 5 | Cadastro das pessoas da residência | Sim, ao menos como referência para o filtro "quem realizou" | Detalhes adicionais podem vir depois |
| 6 | Cadastro das contas bancárias | Sim, ao menos uma conta, para existir "quanto dinheiro existe" | Contas adicionais podem ser cadastradas depois |
| 7 | Cadastro dos cartões | Não bloqueia o uso do sistema, mas bloqueia qualquer funcionalidade de crédito | Pode ser feito depois, se a família não usar cartão de imediato |
| 8 | Cadastro das categorias | Recomendado desde o início (as sugeridas em `VISAO-FUNCIONAL.md` servem de ponto de partida) | Novas categorias podem ser criadas a qualquer momento |
| 9 | Configuração de renda | Recomendado desde o início, para o orçamento e a meta funcionarem | Pode ser ajustado mês a mês |
| 10 | Configuração da meta de economia | Não bloqueia o uso; sem ela, o cálculo de disponibilidade segura simplesmente não subtrai nenhuma meta | Pode ser configurada depois |
| 11 | Cadastro das contas recorrentes | Recomendado desde o início, para a agenda e o orçamento refletirem a realidade | Pode ser incrementado ao longo do uso |
| 12 | Cadastro de saldos e faturas iniciais | Sim, para o saldo consolidado e a fatura em aberto não começarem incorretos | — |
| 13 | Cadastro de empréstimos antigos | Opcional — só se já existirem empréstimos em andamento no momento do cadastro | Pode ser feito quando o empréstimo for lembrado |
| 14 | Validação do dashboard inicial | Sim — é o critério de que o cadastro inicial está correto antes do uso contínuo | — |

O cadastro inicial é considerado concluído quando o dashboard mostra números que a família
reconhece como corretos (saldo por conta, fatura em aberto de cada cartão, contas recorrentes
conhecidas) — esse reconhecimento manual é a validação do passo 14, não uma conferência
automática do sistema.

## 2. Receita

1. Registrar receita (manual ou por recorrência configurada, ex.: salário).
2. Informar conta de destino, origem da receita e, se recorrente, a periodicidade.
3. A receita nasce como **prevista**; passa a **realizada** quando o recebimento é confirmado
   (ver `ESTADOS-E-TRANSICOES.md`, tabela "Receita").
4. Alterações de valor só são permitidas enquanto a receita está prevista ou realizada — nunca
   depois de conciliada (mesma lógica de proteção já usada no módulo `FINANCEIRO` genérico para
   lançamentos pagos, ver `docs/FINANCEIRO.md`).
5. Cancelamento é possível a qualquer momento antes da conciliação; depois, o cancelamento deve
   passar por um estorno explícito, não por exclusão.
6. Conciliação com o extrato compara valor, data, conta e descrição (mesmos critérios do
   processo de conciliação, seção 12) e marca a receita como conciliada.
7. **Transferência entre contas próprias nunca é tratada como receita** (ver regra obrigatória
   na seção 3 de `VISAO-FUNCIONAL.md`, detalhada abaixo).

### Identificação e conciliação das duas pontas de uma transferência entre contas próprias

Uma transferência entre contas da mesma família aparece no extrato de **duas** contas: como
saída em uma e entrada na outra. O processo deve:

1. reconhecer que ambas as pontas pertencem a contas da mesma empresa (não de terceiros);
2. comparar valor (idêntico ou muito próximo, considerando eventual tarifa) e data (mesmo dia ou
   1 dia de diferença, por causa de processamento bancário);
3. marcar as duas pontas como uma única transferência conciliada entre si, não como duas
   movimentações independentes;
4. excluir essa transferência de qualquer soma de receita consolidada ou despesa consolidada —
   ela move dinheiro entre contas da própria família, não altera o patrimônio total.

## 3. Despesa no débito

1. Cadastrar o gasto: conta, pessoa, categoria, estabelecimento, data.
2. Anexar comprovante, quando necessário.
3. Marcar como **previsto** (ex.: uma conta que ainda vai vencer) ou **realizado** (o gasto já
   ocorreu).
4. Conciliar posteriormente com o extrato bancário (mesmos critérios da seção 12).
5. Evitar duplicidade: antes de criar um novo lançamento a partir de uma importação, o processo
   de conciliação (seção 12) deve verificar se já existe um lançamento manual equivalente.

### Diferença entre os quatro estados do lançamento de débito

- **Previsto** — o gasto é esperado, mas ainda não ocorreu (ex.: conta que vence em breve).
- **Realizado** — o gasto já ocorreu, informado manualmente pela família, mas ainda não
  confirmado contra o extrato bancário.
- **Conciliado** — o gasto realizado foi confirmado contra uma movimentação bancária real
  (importada ou consultada), maior grau de confiança de que o valor está correto.
- **Cancelado** — o gasto foi registrado por engano ou não vai mais ocorrer; permanece no
  histórico, nunca soma em nenhum total monetário.

## 4. Compra no crédito (ciclo completo)

1. Cadastrar a compra: descrição, estabelecimento, data, valor total, categoria, cartão, pessoa.
2. Identificar se o cartão usado é principal ou um cartão virtual vinculado a um principal — se
   virtual, o impacto de limite e fatura recai sobre o cartão principal (ver `REGRAS-DE-NEGOCIO.md`).
3. Definir quantidade de parcelas (1 para compra à vista).
4. Definir a fatura inicial (a primeira fatura em que a compra ou a primeira parcela aparece,
   considerando a data de fechamento do cartão — ver seção 5).
5. Gerar as parcelas futuras, todas vinculadas à compra original e herdando sua categoria.
6. A compra ocupa o limite do cartão no momento da compra, pelo **valor total** (ver a "visão da
   decisão de consumo" em `VISAO-FUNCIONAL.md`) — não apenas pelo valor da primeira parcela.
7. Cada parcela entra na fatura do mês correspondente.
8. A fatura é paga (seção 5).
9. Estorno ou cancelamento, quando aplicável (seção "Estornos", regras em `REGRAS-DE-NEGOCIO.md`).
10. Histórico da compra e de todas as suas parcelas permanece consultável mesmo após a quitação.

### Quando cada evento acontece

- **A compra entra no gasto real** no momento em que é registrada (data da compra), pelo valor
  total — é uma decisão já tomada, mesmo que o pagamento se estenda por meses.
- **A parcela entra no orçamento mensal** apenas no mês em que ela é cobrada na fatura (a "visão
  mensal" de `VISAO-FUNCIONAL.md`) — é o impacto recorrente no fluxo de caixa daquele mês.
- **A saída efetiva da conta** só ocorre quando a fatura correspondente é paga, não quando a
  compra é feita nem quando a parcela "vence" dentro da fatura.

### Regra obrigatória — compra × pagamento da fatura

> O pagamento da fatura é saída de caixa, mas não deve ser contado novamente como consumo,
> porque o consumo já foi reconhecido nas compras.

Isso significa que o gasto real do mês **não soma** o valor da fatura paga; ele já contabilizou
cada compra (ou cada parcela, na visão mensal) no momento correto. O pagamento da fatura afeta
apenas a saída efetiva da conta usada para pagamento — nunca deve ser lançado como uma despesa
adicional de categoria "cartão de crédito" ou similar, sob risco de contar o mesmo gasto duas
vezes (ver caso extremo correspondente em `REGRAS-DE-NEGOCIO.md`).

## 5. Fechamento e pagamento da fatura

1. A fatura nasce **aberta** quando o ciclo começa (dia seguinte ao fechamento anterior).
2. No dia de fechamento do cartão, a fatura passa a **fechada** — nenhuma nova compra entra
   nela a partir desse ponto; compras feitas a partir do fechamento entram na próxima fatura.
3. Até o vencimento, a família decide a forma de pagamento: integral (regra normal da família),
   mínimo ou parcial.
4. Pagamento integral até o vencimento → fatura **paga**.
5. Pagamento parcial ou mínimo → fatura **parcialmente paga**; o saldo não pago pode ser
   financiado pelo banco (com juros e encargos) e refletir na próxima fatura.
6. Pagamento após o vencimento sem ter sido feito antes → fatura **atrasada**; ao ser paga,
   passa a paga ou parcialmente paga, conforme o valor quitado.
7. Uma fatura com saldo financiado ao longo de vários ciclos pode ser tratada como
   **refinanciada** (ver `ESTADOS-E-TRANSICOES.md`).

### Efeito de uma fatura paga parcialmente

- **Saldo da conta**: sai apenas o valor efetivamente pago.
- **Saldo do cartão (limite disponível)**: o valor não pago da fatura **não libera limite** —
  o limite ocupado pelas compras originais permanece ocupado até a quitação total, e os juros/
  encargos gerados por não pagar integralmente também passam a ocupar limite.
- **Orçamento**: o valor não pago não desaparece do comprometimento futuro; ele se soma à
  próxima fatura, junto com os encargos.
- **Próximas faturas**: recebem o saldo financiado mais juros e encargos, aumentando o valor
  devido nos meses seguintes.
- **Risco financeiro**: o Simulador de Gastos e o dashboard devem refletir esse comprometimento
  adicional ao calcular disponibilidade segura e ao classificar o risco (ver
  `CALCULOS-E-INDICADORES.md`).
- **Dashboard**: o indicador "risco de pagamento parcial da fatura" deve acender a partir do
  momento em que a família não paga integralmente, não apenas quando o valor está muito alto.

## 6. Parcelamento

Tipos diferentes de "parcela" que não devem ser confundidos:

| Tipo | Onde nasce | Afeta limite de cartão | Afeta fatura |
| --- | --- | --- | --- |
| Compra parcelada no cartão | Compra no crédito (seção 4) | Sim (valor total no momento da compra) | Sim, uma parcela por fatura |
| Conta parcelada fora do cartão | Conta a pagar (seção 7) | Não | Não |
| Empréstimo recebido | Compromisso a pagar (seção 8) | Não | Não |
| Compra feita no cartão de outra pessoa | Compromisso a pagar (a família é a devedora) | Não (o limite ocupado é do cartão de terceiro) | Não |
| Compra para terceiro no cartão da família | Compra no crédito, marcada como "para terceiro" (seção 10) | Sim (é a família quem ocupa o próprio limite) | Sim |
| Renegociação | Qualquer um dos anteriores, quando o valor original é substituído por novas condições | Depende do tipo original | Depende do tipo original |

Ações comuns a qualquer parcela, independentemente do tipo: criação, alteração, antecipação
(pagar antes do vencimento), atraso, quitação, cancelamento, estorno, mudança de vencimento,
consulta de saldo restante.

### Alteração de uma parcela afeta apenas ela ou toda a série?

- Alterar **valor, categoria ou vencimento de uma única parcela já gerada** afeta apenas aquela
  parcela — as demais permanecem como estavam.
- Cancelar ou renegociar a **compra original** (antes de qualquer parcela ter sido paga) deve
  poder recalcular toda a série de parcelas futuras.
- Uma parcela já paga nunca é alterada retroativamente por uma mudança na compra original.
- Se a antecipação de uma parcela deve ou não gerar desconto proporcional de juros (quando a
  compra tiver juros do parcelamento) **não está definido** — registrado em `PENDENCIAS.md`.

## 7. Contas a pagar

1. Cadastrar como única ou recorrente (com periodicidade: semanal, quinzenal, mensal,
   trimestral, semestral, anual ou personalizada).
2. Para contas recorrentes, o sistema gera automaticamente a próxima ocorrência a partir da
   periodicidade configurada.
3. Informar se o valor é fixo, variável ou estimado.
4. Acompanhar até o pagamento: integral, parcial, com juros e multa em caso de atraso.
5. Anexar comprovante quando disponível.
6. Cancelamento é possível antes do pagamento (ex.: uma conta recorrente que deixou de existir).

### Contas recorrentes com valor variável

1. A ocorrência é gerada com um **valor estimado** (o último valor conhecido ou um valor
   configurado manualmente).
2. Quando a conta real chega (boleto, fatura de consumo etc.), o valor é **atualizado para o
   valor real**.
3. O sistema deve manter visível a comparação entre o valor estimado usado no orçamento e o
   valor realmente pago.
4. A diferença entre estimado e realizado deve ser refletida no orçamento do mês em que a conta
   foi paga, nunca retroativamente em meses já fechados.

## 8. Compromissos a pagar

Diferente de contas a pagar (seção 7), um compromisso a pagar representa uma obrigação **fora do
cartão de crédito e fora das contas domésticas recorrentes** — dinheiro que a família recebeu ou
uma compra feita em nome da família por outra pessoa.

Tipos: empréstimo recebido, dívida informal, compra no cartão de terceiro, parcelamento direto
com um credor.

Fluxo: registrar credor, valor original, juros opcionais, multa, parcelas e vencimentos →
acompanhar valor pago e saldo → quitar ou renegociar.

### Regra obrigatória — compromisso não é consumo

> Receber R$ 1.000 emprestados aumenta o caixa, mas cria uma obrigação e não representa renda.

O valor recebido de um empréstimo (quando entra na conta da família) **não deve** ser somado à
renda do mês nem ao gasto real — ele é caixa disponível temporariamente, mas contrabalançado por
uma obrigação de mesmo valor que deve aparecer no comprometimento futuro e na disponibilidade
segura.

## 9. Empréstimos concedidos

1. Registrar devedor, valor principal, conta de origem e data.
2. Definir juros opcionais, multa por atraso e, se aplicável, parcelamento com datas prometidas.
3. Registrar recebimentos parciais ou totais conforme ocorrem.
4. Acompanhar atraso e, se necessário, gerar mensagem de cobrança (fora do MVP o envio
   automático, ver `MVP.md`).
5. Permitir renegociação (nova data, novo valor, novas condições) sem perder o histórico do
   acordo original.
6. Quitação encerra o compromisso; permanece consultável no histórico.
7. Permitir cadastrar empréstimos antigos já em andamento (cadastro inicial, seção 1).

### Regra obrigatória — empréstimo concedido não é consumo

> Empréstimo concedido reduz o caixa disponível, mas não é consumo da residência.

O valor emprestado sai da conta de origem (reduz o saldo consolidado), mas não deve ser somado
ao gasto real nem à categoria de despesas — ele vira um valor a receber.

### Onde o valor a receber aparece

- **Fluxo de caixa**: como saída no momento do empréstimo; como entrada no momento de cada
  recebimento.
- **Patrimônio**: como um ativo (valor a receber), distinto do saldo em conta.
- **Dashboard**: no indicador "quanto terceiros ainda devem à família".
- **Simulador**: um novo empréstimo concedido é um dos tipos de operação simuláveis (impacto
  imediato no caixa disponível).
- **Atrasados**: quando a data prometida passa sem recebimento, aparece na agenda financeira
  como atrasado.

## 10. Compra para terceiro

1. Ao cadastrar uma compra no cartão da família, marcar explicitamente que é para um terceiro
   (não para consumo residencial) e identificar esse terceiro.
2. A compra segue o fluxo normal de compra no crédito (seção 4): ocupa limite, entra na fatura,
   pode ser parcelada.
3. A diferença entre a **parcela da fatura** (o que a família paga ao banco todo mês) e a
   **parcela do terceiro** (o que o terceiro deve pagar de volta à família) deve ser
   acompanhada separadamente — elas podem ter prazos e valores diferentes se a família decidir
   cobrar o terceiro em condições diferentes das da fatura.
4. Recebimentos do terceiro reduzem o valor a receber dessa compra especificamente.
5. Atraso do terceiro não altera a obrigação da família com o banco — a fatura continua vencendo
   normalmente, independente de o terceiro ter pagado ou não.
6. Quitação, cancelamento e estorno seguem as mesmas regras de uma compra normal (seção 4 e
   seção "Estornos" em `REGRAS-DE-NEGOCIO.md`), com o cuidado adicional de também encerrar o
   valor a receber correspondente.

### Regra obrigatória — compra para terceiro

> Compra para terceiro não entra no consumo residencial, mas ocupa limite, entra na fatura e
> gera exposição financeira.

O cálculo da exposição financeira a terceiros está em `CALCULOS-E-INDICADORES.md`.

## 11. Conciliação bancária

1. Entrada de dados: OFX, CSV, Excel, PDF ou lançamento manual já existente no sistema.
2. Para cada movimentação importada, comparar contra os lançamentos manuais existentes usando
   os critérios da seção 12 abaixo.
3. Classificar cada movimentação conforme o grau de correspondência encontrado (ver estados na
   seção "Conciliação" em `ESTADOS-E-TRANSICOES.md`).
4. A família confirma manualmente cada correspondência sugerida antes de ela virar conciliação
   definitiva — **nenhuma conciliação é automática e irreversível no MVP**.
5. Casos especiais que o processo deve reconhecer: transferência entre contas próprias (seção 2),
   pagamento de fatura (não deve virar um novo lançamento de despesa, ver seção 4), PIX
   recorrente do apartamento (deve casar com o lançamento já recorrente, ver `REGRAS-DE-NEGOCIO.md`),
   recebimento de empréstimo concedido (deve casar com o compromisso correspondente, seção 9).

### Critérios de comparação

Valor, data (com tolerância de poucos dias, para cobrir atraso de processamento bancário),
conta ou cartão de origem, descrição/estabelecimento, identificador bancário (quando disponível
no arquivo importado), tipo de movimentação, e proximidade temporal geral entre o lançamento
manual e a movimentação importada.

## 12. Categorização

1. Ao cadastrar uma compra ou despesa manualmente, a categoria é escolhida pela família.
2. Ao importar uma movimentação, o sistema pode sugerir uma categoria com base em regras já
   criadas (ver ordem de prioridade abaixo).
3. Se nenhuma regra corresponder, a movimentação fica sem categoria sugerida, aguardando
   categorização manual.
4. Ao categorizar manualmente uma movimentação que se repete, o sistema pode perguntar se a
   família deseja criar uma regra permanente a partir desse exemplo.
5. Regras podem ser ativadas, desativadas e editadas a qualquer momento; a aplicação retroativa
   (recategorizar lançamentos antigos ao criar uma regra nova) deve ser uma ação explícita da
   família, nunca automática.

### Ordem de prioridade das regras

1. Regra específica por estabelecimento (mais precisa).
2. Regra por descrição exata.
3. Regra por texto parcial (menos precisa, mais abrangente).
4. Categoria sugerida por histórico semelhante, quando não houver regra.
5. Categorização manual, quando nada acima se aplica.

Quando duas regras do mesmo nível de prioridade puderem se aplicar ao mesmo lançamento (ex.:
duas regras de texto parcial que casam com a mesma descrição), a regra criada mais recentemente
prevalece — critério simples e prevísivel, mas que pode ser revisado quando a modelagem de
dados definir como as regras são armazenadas e avaliadas (registrado como ponto de atenção, não
uma pendência crítica).

## 13. Orçamento

1. Definir limite geral mensal e limites por categoria.
2. Definir o limite saudável de cada cartão (distinto do limite bancário).
3. Ao longo do mês, o sistema acumula valor previsto e valor realizado por categoria e no total.
4. Calcular percentual consumido e emitir alerta ao se aproximar ou ultrapassar o limite — nunca
   bloquear o lançamento (mesmo princípio do limite saudável do cartão).
5. Compras parceladas impactam o orçamento de duas formas simultâneas e não conflitantes: o
   **valor total da decisão** compõe o gasto real do mês da compra (visão da decisão de consumo);
   **cada parcela** compõe o orçamento do mês em que a fatura correspondente é cobrada (visão
   mensal) — ver seção 4 e `VISAO-FUNCIONAL.md`.

## 14. Meta de economia

1. Configurar percentual (referência inicial: 7%) ou valor fixo, por competência, com regra
   padrão e possibilidade de exceção em um mês específico.
2. Calcular a disponibilidade segura (fórmula em `CALCULOS-E-INDICADORES.md`) subtraindo a meta
   da renda prevista, junto com despesas, faturas, parcelas e compromissos.
3. Acompanhar ao longo do mês se a meta está a caminho de ser atingida, em risco ou já não
   atingida (critérios em `CALCULOS-E-INDICADORES.md`).
4. **Se a economia deve ser apenas uma meta projetada (cálculo), ou se deve envolver transferir
   de fato um valor para uma conta específica, ou ambos em fases diferentes, não está definido**
   — registrado em `PENDENCIAS.md`.

## 15. Simulador de Gastos

1. Selecionar o tipo de operação a simular: débito, crédito à vista, crédito parcelado, conta
   recorrente, assinatura, empréstimo concedido ou compra para terceiro.
2. Informar valor, forma de pagamento, cartão ou conta, parcelas (se aplicável), categoria, data
   e o terceiro envolvido (quando aplicável).
3. O sistema calcula os cenários usando os mesmos dados e fórmulas do restante do sistema
   (`CALCULOS-E-INDICADORES.md`) — nunca uma fórmula paralela só para simulação.
4. Apresentar projeções para o mês atual, 3, 6 e 12 meses, o impacto na fatura, no limite, na
   categoria, na meta de economia e o risco de não conseguir pagar a fatura integralmente.
5. Classificar o resultado (saudável, atenção, alto risco, crítico — critérios em
   `CALCULOS-E-INDICADORES.md`) e explicar a recomendação com os números usados.
6. A família pode salvar a simulação como um lançamento real (transformando-a no processo
   correspondente: compra no crédito, despesa no débito etc.) ou descartá-la sem nenhum efeito
   nos dados reais.

## 16. Assinaturas

Mesmo tratadas como conta a pagar recorrente no MVP (ver `MVP.md`), o mínimo funcional é:

1. Cadastro com valor, periodicidade, cartão ou conta de cobrança e categoria.
2. Geração automática da próxima cobrança, como qualquer conta recorrente (seção 7).
3. Reajuste de valor: quando o valor muda, a nova cobrança usa o novo valor; o histórico de
   valores anteriores fica preservado (mesmo mecanismo de histórico de contas a pagar).
4. Cancelamento encerra as próximas gerações sem apagar o histórico já cobrado.
5. Projeção anual (fase seguinte, per `MVP.md`) é uma soma das cobranças mensais previstas —
   não exige um cálculo diferente do de qualquer conta recorrente mensal.

## 17. Agenda e alertas

1. A agenda reúne, em uma única visão, tudo que tem vencimento: contas, faturas, parcelas,
   empréstimos a receber, assinaturas e despesas anuais.
2. O alerta padrão é de 3 dias antes do vencimento, configurável globalmente e também por
   compromisso individual (uma configuração específica sempre prevalece sobre a global).
3. A integração com Google Calendar é evolução futura (`MVP.md`); o comportamento interno de
   geração de vencimentos e alertas deve funcionar de forma completa e independente de qualquer
   integração externa.

## 18. Lixeira

1. Excluir um item envia-o para a lixeira, preservando todos os seus dados.
2. **Um item na lixeira não participa de nenhum cálculo ativo** (saldo, orçamento, dashboard,
   simulador) — confirmado como regra obrigatória (ver `REGRAS-DE-NEGOCIO.md`).
3. Restaurar devolve o item ao estado em que estava antes da exclusão, voltando a participar dos
   cálculos.
4. Excluir definitivamente remove o item da lixeira de forma irreversível.
5. Anexos vinculados a um item acompanham o mesmo ciclo (vão para a lixeira junto e são
   restaurados ou removidos junto).
6. **Se existirá um prazo automático de retenção na lixeira antes da exclusão definitiva
   obrigatória não está definido** — registrado em `PENDENCIAS.md`.

## 19. Auditoria

Para cada evento auditável (criação, alteração, exclusão, restauração, conciliação,
desconciliação, pagamento, cancelamento, alteração de valor/categoria/vencimento/status), o
mínimo a registrar é: usuário responsável, data e hora, entidade afetada, ação realizada, valor
anterior e valor novo (quando aplicável). Este é o mesmo princípio mínimo já usado no restante da
plataforma (ver `docs/SEGURANCA.MD`, seção de auditoria) — o Financeiro LeS não precisa de um
mecanismo de auditoria diferente do já estabelecido, apenas aplicado às suas próprias entidades.

## Documentos relacionados

- `VISAO-FUNCIONAL.md`, `MVP.md`, `GLOSSARIO.md` (LES-F1-001).
- `REGRAS-DE-NEGOCIO.md` — regras extraídas destes processos, organizadas por tema, casos
  extremos e recomendação sobre o módulo `FINANCEIRO` existente.
- `ESTADOS-E-TRANSICOES.md` — tabelas completas de estado para cada entidade citada aqui.
- `CALCULOS-E-INDICADORES.md` — fórmulas citadas ao longo deste documento.
- `PENDENCIAS.md` — pontos deste documento ainda sem definição.
