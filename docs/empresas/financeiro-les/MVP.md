# MVP — Financeiro LeS

Separação entre o que é essencial para o primeiro uso real (MVP), o que é importante mas pode
esperar a validação do núcleo (fase seguinte) e o que é integração/automação avançada (evolução
futura). Nenhum destes itens foi implementado nesta tarefa — este documento orienta o
planejamento das próximas etapas (modelagem, regras, dados, telas, roadmap, divisão de tarefas).

## Critério usado para decidir o corte do MVP

Um item entra no MVP obrigatório quando, sem ele, a família **não consegue** responder às
perguntas centrais listadas em `VISAO-FUNCIONAL.md` (quanto dinheiro existe, quanto foi gasto,
quanto está comprometido, se uma compra é segura) usando dados reais e atuais. Um item vai para a
fase seguinte quando é importante para o uso contínuo, mas o núcleo funciona sem ele no primeiro
mês de uso (com lançamento manual como alternativa). Um item vai para evolução futura quando
depende de integração externa, automação avançada ou de o núcleo já estar validado com uso real.

## MVP obrigatório

### Empresa e usuários

Cadastro da empresa `Financeiro LeS`, duas pessoas com login próprio, mesmo nível de acesso,
visão compartilhada, filtro por pessoa nos lançamentos. Justificativa: é a fundação — sem isso
nada mais funciona. Depende da fundação multiempresa já existente na plataforma.

### Contas bancárias

As 4 contas iniciais (Pessoa 1/2 × Banco do Brasil/Inter), saldo por conta e saldo consolidado
sem dupla contabilização, transferência entre contas próprias não classificada como
receita/despesa. Justificativa: é a base de "quanto dinheiro existe agora". Dinheiro em espécie é
suportado, mas não é prioridade de implementação dentro do MVP.

### Categorias

As categorias iniciais listadas em `VISAO-FUNCIONAL.md` (Moradia, Alimentação, Saúde, Transporte,
Financeiro, Compromissos e doações, Outras), com criação, edição e desativação. Agrupamento de
categorias e uso em limites de orçamento entram no MVP porque o orçamento por categoria também é
MVP (ver abaixo). Categorização aplicada à compra inteira, herdada pelas parcelas.

### Receitas

Salários fixos com recorrência e conta de destino, pequenas rendas extras, comparação entre
previsto e recebido. Justificativa: sem receita não há "quanto entrou no mês", base do gasto real
e do orçamento.

### Despesas e contas a pagar

Contas únicas e recorrentes (mensal, semanal, quinzenal, trimestral, semestral, anual,
personalizada), geração automática das próximas ocorrências, estados (pendente, parcialmente
paga, paga, atrasada, cancelada), pagamento integral e parcial, juros e multa por atraso. Inclui
a regra especial do pagamento do apartamento (PIX recorrente, despesa de moradia, favorecido,
conciliação). Justificativa: é o maior volume de compromissos fixos da família e alimenta
diretamente a disponibilidade segura do orçamento.

### Cartões de crédito

Os 4 cartões iniciais (com possibilidade de redução futura), cadastro completo (banco, titular,
bandeira, físico/virtual, cartão principal quando virtual, limite bancário, limite saudável,
fechamento, vencimento, conta de pagamento, status, melhor dia de compra calculado). Cartões
virtuais compartilham limite, fatura, vencimento e pagamento com o cartão principal. Bandeiras
cadastráveis (Elo, Mastercard, Visa, outras). Justificativa: o problema central do produto é o
descontrole por cartão de crédito — sem o cadastro completo do cartão, o restante do módulo de
crédito não tem como funcionar corretamente.

### Compras no crédito, parcelas e as duas visões

Cadastro completo da compra (descrição, estabelecimento, data, valor total, categoria, cartão,
pessoa, parcelas, valor da parcela, fatura inicial, observação, comprovante, indicação de compra
residencial ou para terceiro), separação entre data da compra, mês de consumo, fatura de cobrança
e data de pagamento. **As duas visões do crédito (decisão de consumo × visão mensal) são MVP
obrigatório** — são a base para o usuário entender o problema real que o produto resolve.
Geração das parcelas futuras vinculadas à compra original, com prevenção de duplicação.

### Faturas

Cartão, competência, período, fechamento, vencimento, valor total, composição (compras à vista,
parcelas, assinaturas, encargos, estornos, pagamentos), saldo pendente, estados (aberta, fechada,
paga, paga parcialmente, atrasada). Suporte a pagamento integral (regra normal da família),
mínimo e parcial, com alerta de risco. Justificativa: é o ponto onde o consumo do cartão vira
compromisso de caixa — não é possível calcular "quanto está comprometido" sem faturas.

### Estornos e cancelamentos

Diferenciação entre compra cancelada, estorno integral, estorno parcial e estorno recebido em
fatura futura, sempre vinculados à compra original, nunca classificados como receita comum.
Justificativa: sem isso, o gasto real fica distorcido por lançamentos que na prática não
representam consumo.

### Contas a pagar (compromissos fora do cartão) e valores a receber (empréstimos concedidos)

Cadastro completo de compromissos a pagar (credor, valor, juros opcionais, multa, parcelas,
vencimentos, saldo, comprovantes, situação) e de empréstimos concedidos (devedor, valor
principal, juros opcionais, multa, parcelas, datas prometidas, recebimentos, saldo a receber,
comprovantes, renegociação), incluindo a possibilidade de cadastrar empréstimos antigos já em
andamento. Justificativa: "quanto terceiros devem à família" e "quanto a família deve a
terceiros" são perguntas explícitas do objetivo do produto.

### Compras para terceiros e exposição financeira a terceiros

Marcação de compra como residencial ou para terceiro; quando para terceiro, gera valor a
receber, é considerada no Simulador de Gastos e aparece separadamente nos relatórios/dashboard
como **exposição financeira a terceiros** (ver `GLOSSARIO.md`). Justificativa: sem essa
separação, o gasto real residencial fica distorcido.

### Orçamento e limites

Limite geral mensal, limites por categoria, limite saudável dos cartões (separado do limite
bancário), valor realizado, valor previsto, percentual consumido, alertas de aproximação e
ultrapassagem — apenas alerta, nunca bloqueio. Justificativa: é a base numérica da
"disponibilidade segura" e de boa parte do Simulador de Gastos.

### Meta de economia

Percentual inicial de 7% da renda mensal (configurável — por percentual, valor fixo, competência,
regra padrão e exceção mensal; **não fixado no código**), cálculo de disponibilidade segura:

```text
Renda prevista
− despesas
− faturas
− parcelas
− compromissos
− meta de economia
= disponibilidade segura
```

Justificativa: alimenta diretamente o Simulador de Gastos e é uma pergunta explícita do objetivo
do produto ("se a meta de economia será atingida").

### Simulador de Gastos (funcionalidade central)

Simulação de compra no débito, crédito à vista, compra parcelada, nova assinatura, nova conta
recorrente, empréstimo concedido e compra no cartão para terceiro. Resultado com valor total da
decisão, impacto no mês atual, nas contas, na fatura, no limite do cartão, por parcela,
projeções de 3/6/12 meses, impacto na categoria e na meta de economia, saldo projetado,
comprometimento futuro, risco de não pagar a fatura integral e recomendação explicada (com os
números usados — nunca uma recomendação opaca). Classificações: saudável, atenção, alto risco,
crítico. Sempre alerta, nunca bloqueia. **Justificativa: é a funcionalidade central do produto**
(explícito no contexto da tarefa) — entra no MVP mesmo em uma versão inicial mais simples do que
a visão completa, que pode evoluir na fase seguinte.

A sugestão de forma de pagamento (comparar débito, crédito à vista, parcelamento, aguardar
fechamento, adiar a compra) existe apenas dentro do Simulador e faz parte do mesmo MVP.

### Dashboard

Tela inicial com os indicadores: saldo consolidado e por conta, renda do mês, gasto real total,
gasto no débito, compras no crédito, saída efetiva das contas, faturas abertas, parcelas
futuras, contas a pagar, valores a receber, exposição a terceiros, meta de economia, orçamento
disponível, gasto por categoria, gasto por pessoa, cartão mais utilizado, cartão com maior gasto,
aumento ou redução dos gastos, risco de pagamento parcial da fatura; comparações mês atual ×
anterior, mês atual × média histórica, realizado × orçamento, projetado × disponível.
Justificativa: é a tela inicial e a validação visual de que o gasto real está sendo calculado
corretamente — critério de sucesso explícito do produto.

### Lixeira e histórico básico

Exclusão reversível (restaurar/excluir definitivamente, quem e quando excluiu), histórico de
alterações importantes (valor, categoria, vencimento, data, status, conta, cartão, pessoa
responsável). Sem necessidade de aviso à outra pessoa após alteração. Justificativa: item de
segurança operacional básica, de baixo custo de implementação frente ao risco de perda de dado
em um sistema usado por duas pessoas com o mesmo nível de acesso.

## Fase seguinte

Itens importantes, mas que podem esperar a validação do núcleo:

- **Assinaturas como módulo dedicado** (com histórico de reajustes, projeção anual detalhada e
  indicadores próprios) — o cadastro básico de uma assinatura como conta a pagar recorrente já
  cobre o essencial no MVP; o detalhamento (aumentos de preço, quantidade por cartão) pode vir
  depois.
- **Metas financeiras além da meta de economia** (reduzir fatura, cancelar cartão, quitar
  dívidas, reduzir gasto por categoria, formar reserva com acompanhamento de progresso dedicado)
  — a meta de economia (percentual da renda) é MVP; as demais metas nomeadas são uma camada de
  acompanhamento adicional sobre dados que o MVP já produz.
- **Agenda financeira com visão de calendário mensal** — uma visão em lista de próximos
  dias/atrasados pode cobrir o essencial no MVP; o calendário mensal completo é um refinamento
  de apresentação sobre dados que já existem.
- **Mensagens de cobrança configuráveis** — depende de valores a receber já existirem (MVP), mas
  o modelo/edição/tom de mensagem é um refinamento que não bloqueia o uso do núcleo.
- **Conciliação com sugestão avançada de correspondência** (proximidade entre registros,
  múltiplos critérios simultâneos) — uma conciliação manual simples (comparar valor, data,
  conta/cartão) pode ser suficiente no início; o refinamento de sugestão é evolução natural após
  observar os dados reais de conciliação.
- **Categorização automática por regras** — no MVP a categorização é manual; regras automáticas
  (texto exato/parcial, por estabelecimento/conta/cartão/pessoa, com sugestão de regra permanente
  ao categorizar manualmente) reduzem esforço repetitivo, mas dependem de volume real de dados
  categorizados manualmente para serem úteis e bem calibradas.
- **Exportação e relatórios em PDF/Excel** — a consulta pelo dashboard e pelas telas cobre o
  essencial no MVP; a exportação formatada é um refinamento de conveniência.

## Evoluções futuras

Integrações, automações avançadas e inteligência adicional — dependem do núcleo já validado com
uso real:

- **Importação de extratos/faturas em PDF** (e eventual OCR) — Banco Inter pode disponibilizar
  algumas faturas apenas em PDF, o que exigiria parser ou OCR, tecnicamente mais custoso que OFX
  ou CSV/Excel. OFX e CSV/Excel também não são MVP obrigatório (o MVP pode operar com lançamento
  manual), mas são mais simples de viabilizar antes do PDF.
- **Integração com Google Calendar** — registrada apenas como visão funcional nesta tarefa
  (criação de eventos para contas, faturas, cobranças, parcelas, assinaturas, despesas anuais),
  sem nenhuma decisão técnica prematura.
- **Envio automático de mensagens de cobrança** — o MVP e a fase seguinte cobrem apenas modelo
  configurável com cópia manual.
- **Backup completo e exportação de dados protegendo anexos** em um fluxo dedicado de backup
  recuperável — o MVP e a fase seguinte cobrem anexos vinculados e privados; um fluxo de backup
  formalizado (agendado, verificável) é evolução.
- **Recomendações avançadas do Simulador** (aprendizado a partir do histórico da própria família,
  comparações mais sofisticadas) — o MVP cobre uma recomendação explicada com números fixos e
  regras claras; qualquer camada de recomendação "inteligente" adicional é evolução futura e
  nunca deve substituir a explicação numérica transparente já exigida no MVP.
- **Conciliação automática irreversível** — fora de qualquer fase até que o processo manual/
  sugerido tenha sido validado com uso real (risco documentado em `VISAO-FUNCIONAL.md` e nesta
  seção).

## Dependências entre módulos

```text
Empresa e usuários
  └─ Contas bancárias ──────────────┐
  └─ Categorias ─────────────────────┤
       └─ Receitas ───────────────────┤
       └─ Contas a pagar ─────────────┤
       └─ Cartões de crédito ─────────┤
            └─ Compras no crédito ────┤
                 └─ Parcelas ─────────┤
                 └─ Faturas ──────────┤
                 └─ Estornos ─────────┤
       └─ Compras para terceiros ─────┤
       └─ Compromissos/Empréstimos ───┤
                                       ├─> Orçamento e limites
                                       ├─> Meta de economia
                                       ├─> Simulador de Gastos
                                       ├─> Dashboard
                                       └─> Lixeira e histórico (transversal a todos)
```

O Simulador de Gastos e o Dashboard são os módulos que **consomem** todos os demais — não podem
ser implementados antes que contas, categorias, cartões, faturas, orçamento e meta de economia
já existam, ainda que em versão inicial. Importação/conciliação e categorização automática
dependem de haver volume real de lançamentos manuais para serem úteis, por isso ficam para
depois do MVP.

## Riscos

- **Duplicidade em importações** — mitigado no MVP por não incluir importação automática; a fase
  seguinte precisa de comparação por múltiplos critérios (valor, data, conta, descrição) antes de
  liberar qualquer importação.
- **Leitura inconsistente de PDFs** — motivo para manter importação em PDF como evolução futura,
  não MVP.
- **Confusão entre compra e pagamento da fatura** — mitigado pela separação explícita de datas
  (compra, consumo, fatura, pagamento) e pelas duas visões do crédito, ambas MVP.
- **Dupla contabilização de transferências** — mitigado pela regra explícita de que transferências
  entre contas próprias não são receita nem despesa (MVP).
- **Considerar valor a receber como dinheiro garantido** — mitigado por manter valores a receber e
  exposição a terceiros como indicadores separados do saldo disponível, nunca somados
  silenciosamente a ele.
- **Distorção do gasto real por compras para terceiros** — mitigado pela marcação obrigatória de
  compra residencial × terceiro desde o MVP.
- **Excesso de funcionalidades no MVP** — mitigado por este próprio corte; qualquer item que não
  tenha justificativa direta com uma pergunta central do produto foi movido para fase seguinte ou
  evolução futura.
- **Dificuldade de manter projeções confiáveis** (3/6/12 meses do Simulador) — depende de dados
  de faturas/parcelas/assinaturas estarem completos e corretos; risco a ser reavaliado após o
  MVP em uso real.
- **Alterações de fechamento e vencimento de cartão** — podem exigir recálculo de faturas em
  aberto; regra de recálculo precisa ser definida na modelagem (não definida nesta tarefa).
- **Segurança de comprovantes** — mitigado por serem privados à empresa desde o MVP (anexos
  vinculados ao registro correto).
- **Crescimento da complexidade do Simulador** — mitigado por entrar no MVP em versão inicial
  (débito, crédito à vista, parcelado, assinatura, conta recorrente, empréstimo concedido, compra
  para terceiro) e evoluir depois, em vez de tentar entregar a versão completa de uma vez.

## Documentos relacionados

- `VISAO-FUNCIONAL.md` — visão consolidada do produto.
- `GLOSSARIO.md` — definição dos termos usados neste documento.
- `PENDENCIAS.md` — decisões que dependem de confirmação futura (inclusive a decisão de
  arquitetura sobre reaproveitar ou não o módulo `FINANCEIRO` genérico já existente).
