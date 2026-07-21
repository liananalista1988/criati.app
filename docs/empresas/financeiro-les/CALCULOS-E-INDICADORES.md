# Cálculos e Indicadores — Financeiro LeS

Fórmulas em linguagem de negócio para os indicadores do Financeiro LeS. Nenhuma fórmula aqui
depende de uma estrutura de dados específica — a modelagem de dados da próxima etapa deve
implementar cada uma delas sobre as entidades que vier a definir, mantendo o resultado descrito
aqui. Onde um critério numérico ainda não foi confirmado pela família, está marcado como pendente
e também registrado em `PENDENCIAS.md`.

## Gasto real do mês

```text
Gasto real do mês
  = soma das despesas no débito realizadas no mês (por data do gasto)
  + soma do valor total de cada compra no crédito feita no mês (por data da compra, não por parcela)
  + soma das contas a pagar pagas no mês que não tenham sido já contadas como despesa no débito
  − estornos e cancelamentos que reduzem qualquer um dos itens acima
```

Nunca inclui: pagamento de fatura (já reconhecido nas compras que a compõem), transferência entre
contas próprias, valor emprestado a terceiro (não é gasto, é redução de caixa disponível com
contrapartida de valor a receber), compra para terceiro (não é consumo residencial — ver
"Exposição financeira a terceiros" abaixo).

## Saída efetiva da conta

```text
Saída efetiva da conta no período
  = soma de todos os débitos, PIX e boletos pagos diretamente da conta
  + soma de todas as faturas pagas (integral ou parcialmente) no período, pelo valor efetivamente pago
  − transferências entre contas próprias (não contam como saída efetiva do consolidado)
```

Diferente do gasto real: uma compra parcelada em 10x é gasto real total no mês da compra, mas
gera saída efetiva apenas quando cada fatura correspondente é paga, mês a mês.

## Saldo consolidado

```text
Saldo consolidado = soma do saldo atual de todas as contas da empresa (ativas e inativas)
```

Saldo atual de uma conta = saldo inicial + receitas realizadas nessa conta − despesas realizadas
nessa conta (mesmo princípio já usado pelo módulo `FINANCEIRO` genérico, ver `docs/FINANCEIRO.md`,
"Cálculo de saldo") — transferências entre contas próprias entram como saída de uma e entrada de
outra, sem alterar o total consolidado.

## Saldo projetado

```text
Saldo projetado (para uma data futura)
  = saldo consolidado atual
  + receitas previstas até aquela data
  − despesas previstas até aquela data
  − faturas com vencimento até aquela data
  − parcelas com vencimento até aquela data
  − compromissos a pagar com vencimento até aquela data
```

Usado pelo Simulador de Gastos para as projeções de 3, 6 e 12 meses (`PROCESSOS.md`, seção 15).

## Orçamento disponível

```text
Orçamento disponível (geral ou por categoria)
  = limite definido (geral ou da categoria)
  − valor já realizado no período
  − valor já previsto/comprometido no período (compras e contas ainda não pagas, mas já lançadas)
```

Quando negativo, é uma ultrapassagem — gera alerta, nunca bloqueio (`REGRAS-DE-NEGOCIO.md`,
regra 10).

## Disponibilidade segura

```text
Disponibilidade segura
  = Renda prevista
  − despesas
  − faturas
  − parcelas
  − compromissos
  − meta de economia
```

Fórmula já registrada em `VISAO-FUNCIONAL.md` e `MVP.md`; repetida aqui como referência central
de cálculo. Pode resultar em valor negativo — isso é um resultado válido e deve ser mostrado
(nunca escondido), indicando que a família comprometeu mais do que a renda prevista permite
(ver caso extremo "Meta de economia impossível" em `REGRAS-DE-NEGOCIO.md`).

## Comprometimento do cartão

```text
Comprometimento do cartão = valor total ocupado do limite bancário
  = soma do valor total de compras ainda não totalmente pagas (parcelas futuras + fatura em aberto)
  + juros e encargos de faturas não pagas integralmente
```

## Comprometimento saudável

```text
Comprometimento saudável = comprometimento do cartão ÷ limite saudável definido para o cartão
```

Expresso como percentual. Quando ultrapassa 100%, o cartão já está acima do limite saudável
definido pela família (ainda que dentro do limite bancário) — gera alerta (`REGRAS-DE-NEGOCIO.md`,
regra 3).

## Parcelas futuras

```text
Parcelas futuras (em um mês específico ou no total)
  = soma do valor de todas as parcelas ainda não pagas com vencimento naquele mês (ou em todos os meses futuros, se "no total")
```

Compõe o comprometimento futuro usado na disponibilidade segura e nas projeções do Simulador.

## Exposição financeira a terceiros

```text
Exposição financeira a terceiros
  = soma do valor a receber de todas as compras feitas para terceiros ainda não reembolsadas
  + soma do saldo a receber de todos os empréstimos concedidos ainda não quitados
```

Nunca somada ao saldo disponível ou ao saldo consolidado como se já estivesse garantida — é
sempre um indicador à parte (`REGRAS-DE-NEGOCIO.md`, seção 7).

## Variação mensal

```text
Variação mensal (de gasto real, de uma categoria, ou de qualquer indicador comparável)
  = ((valor do mês atual − valor do mês anterior) ÷ valor do mês anterior) × 100
```

Quando o mês anterior for zero (ex.: categoria nova), a variação percentual não pode ser
calculada por divisão — o indicador deve mostrar o valor absoluto de aumento em vez de um
percentual (critério de apresentação, não de cálculo financeiro).

Comparação com média histórica segue a mesma fórmula, substituindo "valor do mês anterior" pela
média dos últimos meses (quantidade de meses considerados na média ainda não definida — ver
`PENDENCIAS.md`).

## Meta de economia (cálculo de progresso)

```text
Progresso da meta de economia no período
  = (Renda prevista − despesas − faturas − parcelas − compromissos) ÷ Renda prevista
```

Comparado contra o percentual (ou valor fixo) configurado como meta:

- **Atingida**: ao final do período, o progresso calculado é igual ou maior que a meta
  configurada.
- **Em risco**: durante o período (antes do fechamento), a projeção com base no ritmo atual de
  gasto indica que o progresso ficará abaixo da meta se nada mudar.
- **Não atingida**: ao final do período, o progresso ficou abaixo da meta configurada.

O critério exato de quando o sistema deve classificar "em risco" durante o mês (que percentual
do período já decorrido, que ritmo de gasto) ainda não foi definido pela família — registrado em
`PENDENCIAS.md`.

## Risco de pagamento parcial da fatura

Critério proposto, a validar com a família na modelagem de dados:

- **Saudável**: a fatura está projetada para ser paga integralmente com a disponibilidade segura
  positiva no mês do vencimento.
- **Atenção**: a disponibilidade segura do mês do vencimento é positiva, mas menor que o valor da
  fatura mais uma margem de segurança (percentual de margem não definido — ver `PENDENCIAS.md`).
- **Alto risco**: a disponibilidade segura do mês do vencimento é insuficiente para cobrir a
  fatura integralmente, mas positiva.
- **Crítico**: a disponibilidade segura do mês do vencimento já é negativa antes mesmo de
  considerar a fatura.

Esses mesmos quatro níveis (saudável, atenção, alto risco, crítico) são usados pelo Simulador de
Gastos para classificar qualquer operação simulada, aplicando o mesmo raciocínio: quanto da
disponibilidade segura projetada a nova operação consome.

## Total a pagar

```text
Total a pagar (num momento ou até uma data)
  = soma do saldo pendente de todas as contas a pagar não canceladas
  + soma do saldo pendente de todos os compromissos a pagar não cancelados
  + soma do saldo pendente de todas as faturas não pagas integralmente
```

## Total a receber

```text
Total a receber (num momento ou até uma data)
  = soma do saldo a receber de todos os empréstimos concedidos não cancelados
  + soma do saldo a receber de todas as compras para terceiros ainda não reembolsadas
```

Equivalente à exposição financeira a terceiros somada a qualquer valor a receber que não seja de
terceiros stricto sensu (ex.: um estorno pendente de recebimento) — na prática, no escopo do MVP,
"total a receber" e "exposição financeira a terceiros" coincidem, porque as únicas fontes de
valor a receber previstas são empréstimo concedido e compra para terceiro.

## Saldo de empréstimo (concedido)

```text
Saldo de um empréstimo concedido
  = valor principal
  + juros acumulados (se configurados)
  + multa por atraso (se aplicável)
  − soma dos recebimentos já registrados
```

## Custo mensal e anual de assinaturas

```text
Custo mensal de assinaturas = soma do valor atual de todas as assinaturas ativas com cobrança naquele mês

Custo anual projetado = soma, para os próximos 12 meses, do valor de cada assinatura ativa prevista para cobrar naquele mês
  (considerando reajustes já conhecidos; assinaturas com valor fixo simplesmente repetem o mesmo valor 12 vezes)
```

## Documentos relacionados

- `PROCESSOS.md` — onde cada cálculo é usado dentro do fluxo.
- `REGRAS-DE-NEGOCIO.md` — regras que restringem o que entra ou não em cada soma.
- `ESTADOS-E-TRANSICOES.md` — estados que determinam se um valor entra ou não em uma soma
  (ex.: cancelado nunca entra, na lixeira nunca entra).
- `PENDENCIAS.md` — critérios numéricos ainda não confirmados pela família (margem de risco da
  fatura, ritmo de "em risco" da meta, janela da média histórica).
