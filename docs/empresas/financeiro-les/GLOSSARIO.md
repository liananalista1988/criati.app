# Glossário — Financeiro LeS

Definição dos termos centrais usados em `VISAO-FUNCIONAL.md`, `MVP.md` e `PENDENCIAS.md`. Este
glossário fixa o significado funcional de cada termo para orientar a modelagem de dados e as
regras de negócio nas próximas etapas — não define nomes de campo, tabela ou classe.

**Gasto real** — o total efetivamente consumido pela família em um período, somando débito e
crédito sem duplicidade. Diferente de "saída efetiva das contas", porque uma compra no crédito é
gasto real no momento da compra, mas só sai da conta quando a fatura é paga.

**Saída efetiva** — o valor que de fato saiu das contas bancárias em um período (pagamentos de
débito, PIX, boletos e faturas pagas). É o que explica a variação real de saldo das contas.

**Consumo residencial** — o gasto que pertence de fato ao sustento e às despesas da família,
excluindo qualquer valor gasto em nome de terceiros que será reembolsado.

**Compra para terceiro** — compra feita no cartão ou conta da família em benefício de outra
pessoa, que não deve ser contada como consumo residencial e que gera um valor a receber dessa
pessoa.

**Exposição financeira a terceiros** — o valor que a família ainda pode precisar cobrir com
recursos próprios enquanto não for reembolsada por compras feitas para terceiros. Representa um
risco, não um débito garantido — nunca deve ser somado ao saldo disponível como se já estivesse
recebido.

**Fatura** — o documento de cobrança de um cartão de crédito referente a uma competência e
período, consolidando compras à vista, parcelas, assinaturas, encargos, estornos e pagamentos
daquele ciclo.

**Parcela** — cada uma das divisões futuras de uma compra parcelada, vinculada à compra original,
com sua própria fatura de cobrança e data de vencimento, mas sempre herdando a categoria da
compra original.

**Limite saudável** — o teto de uso do cartão de crédito definido pela própria família como
confortável, distinto e normalmente bem inferior ao limite concedido pelo banco. Serve apenas
para alertar, nunca para bloquear um lançamento.

**Disponibilidade segura** — o valor que a família pode gastar em um período sem comprometer a
meta de economia nem os compromissos já assumidos, calculado como:

```text
Renda prevista
− despesas
− faturas
− parcelas
− compromissos
− meta de economia
= disponibilidade segura
```

**Valor previsto** — o valor esperado de uma receita ou despesa antes de ela se confirmar (ex.:
salário previsto para o dia 5, conta estimada antes de o boleto ser emitido).

**Valor realizado** — o valor efetivamente recebido ou pago, usado para comparação com o valor
previsto e para compor o gasto real e a saída efetiva.

**Conciliação** — o processo de associar um lançamento manual a uma movimentação bancária ou de
fatura importada, confirmando que representam a mesma operação e evitando duplicidade.

**Compromisso a pagar** — uma obrigação financeira da família fora do cartão de crédito (ex.:
empréstimo recebido, compra feita no cartão de outra pessoa, dívida informal, parcelamento
direto com um credor).

**Valor a receber** — o que terceiros devem à família, seja por um empréstimo concedido, seja por
uma compra feita para terceiro no cartão da família.

**Melhor dia de compra** — a data, calculada a partir do dia de fechamento do cartão, que
maximiza o prazo entre a compra e o vencimento da fatura correspondente.

**Cartão principal / cartão virtual** — um cartão virtual é sempre vinculado a um cartão físico
principal, compartilhando limite, fatura, vencimento e forma de pagamento; nunca existe de forma
independente.

## Documentos relacionados

- `VISAO-FUNCIONAL.md`
- `MVP.md`
- `PENDENCIAS.md`
