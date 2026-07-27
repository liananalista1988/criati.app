# Implementação LES-F3-004 — Abertura, composição e fechamento de faturas

## Escopo

Esta entrega cria faturas de cartão sem antecipar pagamento, lançamento financeiro, estorno,
cancelamento ou tela. Cada fatura pertence a uma empresa, a um cartão físico principal e a uma
competência, com unicidade garantida no banco.

## Competência e ciclo

A competência recebida pela API é normalizada para o dia de vencimento efetivo do cartão no mês
informado, preservando a regra já usada em `ParcelamentoCartaoService`: quando o dia não existe,
usa-se o último dia do mês. O fechamento fica no dia de fechamento efetivo da mesma competência.
O período começa no dia seguinte ao fechamento do mês anterior e termina no fechamento atual.

Esse desenho mantém compatibilidade com as parcelas existentes, cuja `competencia` já representa
o vencimento projetado. Não redefine ciclos nem recalcula parcelas.

## Composição

São elegíveis somente parcelas `ABERTA`:

- da mesma empresa;
- da competência exata da fatura;
- cuja compra aponta para o mesmo cartão principal;
- ainda sem fatura ou já associadas à própria fatura.

Assim, compras feitas em cartões virtuais compõem a única fatura do principal. A recomposição
associa as parcelas e soma seus valores com `BigDecimal` em escala 2. Repetir abertura ou
recomposição não duplica fatura, parcela ou valor.

## Fechamento e concorrência

Abertura, recomposição, fechamento e criação de compras usam o mesmo lock pessimista do cartão
principal. O fechamento também bloqueia a fatura e suas parcelas elegíveis, recompõe uma última
vez e muda o estado de `ABERTA` para `FECHADA`. `@Version` acrescenta defesa contra atualização
otimista fora desse fluxo.

O fechamento manual antes da data de fechamento calculada é rejeitado. Fatura fechada é imutável:
não pode ser recomposta nem fechada novamente. Compra retroativa que
geraria parcela para fatura já fechada é rejeitada. Compra já incluída em fatura fechada não pode
ser cancelada ou estornada pelo fluxo atual; essas evoluções exigirão regras próprias em
`LES-F3-006`.

## API

Base: `/api/contexto/financeiro/faturas`.

```text
GET  /                                  listar
GET  /{id}                              detalhar
POST /                                  abrir ou obter idempotentemente
POST /{id}/recompor                     recompor fatura aberta
POST /{id}/fechar                       fechar
```

Leituras exigem autenticação, empresa ativa e aplicação `FINANCEIRO`. Escritas exigem
`ADMINISTRADOR` e CSRF. Toda consulta por identificador inclui a empresa atual.

## Fora do escopo

- pagamentos integral, parcial, mínimo ou complementar;
- lançamentos de saída de caixa;
- saldo financiado, juros e encargos;
- estornos e cancelamentos vinculados à fatura;
- tela de faturas;
- fechamento automático por agendamento.
