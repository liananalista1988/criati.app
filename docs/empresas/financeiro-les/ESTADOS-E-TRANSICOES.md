# Estados e Transições — Financeiro LeS

Tabelas de estado por entidade do Financeiro LeS, em nível funcional (não define nomes de campo
ou enum). Cada tabela lista o estado atual, a ação que provoca a transição, o próximo estado, as
validações exigidas e as transições proibidas. Consolida o comportamento já descrito em
`PROCESSOS.md` e `REGRAS-DE-NEGOCIO.md` em formato de referência rápida para a modelagem de
dados.

## Receita

| Estado atual | Ação | Próximo estado | Validações | Transições proibidas |
| --- | --- | --- | --- | --- |
| (nova) | Registrar receita prevista | Prevista | Conta de destino informada; valor maior que zero | — |
| Prevista | Confirmar recebimento | Realizada | Data de recebimento informada | Prevista → Conciliada diretamente (precisa passar por Realizada) |
| Realizada | Conciliar com extrato | Conciliada | Movimentação bancária correspondente identificada (`PROCESSOS.md`, seção 2) | Conciliada → Prevista (não retrocede) |
| Prevista ou Realizada | Cancelar | Cancelada | — | Conciliada → Cancelada direto (precisa desconciliar antes, ver seção "Conciliação") |
| Cancelada | — | (estado final) | — | Nenhuma transição a partir de Cancelada |

## Despesa no débito

| Estado atual | Ação | Próximo estado | Validações | Transições proibidas |
| --- | --- | --- | --- | --- |
| (novo) | Registrar gasto previsto | Previsto | Conta, categoria e data informadas | — |
| Previsto | Confirmar realização | Realizado | Data do gasto confirmada | — |
| Realizado | Conciliar com extrato | Conciliado | Movimentação bancária correspondente identificada | — |
| Previsto ou Realizado | Cancelar | Cancelado | — | Conciliado → Cancelado direto (precisa desconciliar antes) |
| Cancelado | — | (estado final) | — | Nenhuma transição a partir de Cancelado |

## Conta a pagar

| Estado atual | Ação | Próximo estado | Validações | Transições proibidas |
| --- | --- | --- | --- | --- |
| (nova) | Gerar ocorrência (única ou recorrente) | Pendente | Favorecido, categoria e vencimento informados; se recorrente, periodicidade definida | — |
| Pendente | Pagar parcialmente | Parcialmente paga | Valor pago menor que o valor devido | — |
| Pendente ou Parcialmente paga | Pagar o restante | Paga | Valor total quitado | — |
| Pendente ou Parcialmente paga | Vencimento ultrapassado sem quitação total | Atrasada | — | Paga → Atrasada (não retrocede) |
| Atrasada | Pagar o restante (com juros/multa, se houver) | Paga | Valor total quitado | — |
| Pendente | Cancelar | Cancelada | — | Paga → Cancelada (uma conta já quitada não é cancelada, apenas estornada como despesa, se aplicável) |
| Paga ou Cancelada | — | (estado final) | — | Nenhuma transição a partir de Paga ou Cancelada |

## Fatura

| Estado atual | Ação | Próximo estado | Validações | Transições proibidas |
| --- | --- | --- | --- | --- |
| (nova) | Ciclo se inicia (dia seguinte ao fechamento anterior) | Aberta | Cartão e competência definidos | — |
| Aberta | Dia de fechamento do cartão chega | Fechada | Nenhuma nova compra é aceita nesta fatura a partir daqui | Fechada → Aberta (não retrocede) |
| Fechada | Pagamento integral até o vencimento | Paga | Valor total da fatura quitado | — |
| Fechada | Pagamento parcial ou mínimo | Parcialmente paga | Saldo remanescente registrado, sujeito a juros/encargos na próxima fatura | — |
| Fechada ou Parcialmente paga | Vencimento ultrapassado sem quitação total | Atrasada | — | Paga → Atrasada (não retrocede) |
| Parcialmente paga (por vários ciclos consecutivos) | Reconhecer saldo financiado continuado | Refinanciada | Encargos e juros acumulados identificados | — |
| Atrasada ou Parcialmente paga | Quitar o saldo total | Paga | Valor total (incluindo juros/multa/encargos) quitado | — |
| Paga | — | (estado final do ciclo) | — | Nenhuma transição a partir de Paga |

## Compra no crédito

| Estado atual | Ação | Próximo estado | Validações | Transições proibidas |
| --- | --- | --- | --- | --- |
| (nova) | Registrar compra | Registrada | Cartão, categoria, valor total e parcelas definidos | — |
| Registrada | Gerar parcelas e vincular à(s) fatura(s) | Em andamento | Ao menos uma parcela vinculada a uma fatura | — |
| Em andamento | Todas as parcelas pagas | Quitada | Nenhuma parcela pendente ou atrasada | — |
| Registrada ou Em andamento | Cancelar (antes de qualquer parcela cobrada) | Cancelada | Nenhuma parcela ainda cobrada em fatura fechada | Em andamento com parcelas já pagas → Cancelada (precisa de estorno, não cancelamento) |
| Em andamento ou Quitada | Estornar integralmente | Estornada integral | Parcelas ainda não pagas removidas; parcelas pagas geram estorno financeiro | — |
| Em andamento | Estornar parcialmente | Estornada parcial | Parcelas futuras recalculadas (`PROCESSOS.md`, seção 6) | — |
| Cancelada ou Estornada integral | — | (estado final) | — | Nenhuma transição a partir destes estados |

## Parcela

| Estado atual | Ação | Próximo estado | Validações | Transições proibidas |
| --- | --- | --- | --- | --- |
| (nova, gerada pela compra) | Vincular à fatura correspondente | Pendente | Fatura de destino definida pela data de fechamento do cartão | — |
| Pendente | Fatura em que está entra em Fechada | Cobrada | — | — |
| Cobrada | Fatura é paga (integral ou parcialmente, cobrindo esta parcela) | Paga | — | — |
| Cobrada | Fatura vence sem pagamento total | Atrasada | — | Paga → Atrasada (não retrocede) |
| Pendente ou Cobrada | Estornar (compra original estornada) | Estornada | Vínculo com a compra original mantido | Parcela Paga → Estornada direto (precisa de estorno financeiro específico, não apenas mudança de estado) |
| Pendente | Cancelar (compra original cancelada antes da fatura fechar) | Cancelada | — | Cobrada ou Paga → Cancelada (uma parcela já cobrada não é simplesmente cancelada) |
| Paga, Estornada ou Cancelada | — | (estado final) | — | Nenhuma transição a partir destes estados |

## Empréstimo concedido

| Estado atual | Ação | Próximo estado | Validações | Transições proibidas |
| --- | --- | --- | --- | --- |
| (novo) | Registrar empréstimo | Em aberto | Devedor, valor principal e conta de origem informados | — |
| Em aberto | Registrar recebimento parcial | Parcialmente recebido | Valor recebido menor que o saldo devido | — |
| Em aberto ou Parcialmente recebido | Registrar recebimento total do saldo | Quitado | Saldo devido zerado | — |
| Em aberto ou Parcialmente recebido | Data prometida ultrapassada sem quitação total | Atrasado | — | Quitado → Atrasado (não retrocede) |
| Em aberto, Parcialmente recebido ou Atrasado | Renegociar condições | Renegociado | Novas condições registradas sem apagar o histórico original | — |
| Em aberto | Cancelar (ex.: valor perdoado) | Cancelado | — | Quitado → Cancelado (não se aplica a algo já quitado) |
| Quitado ou Cancelado | — | (estado final) | — | Nenhuma transição a partir destes estados |

## Compromisso a pagar

| Estado atual | Ação | Próximo estado | Validações | Transições proibidas |
| --- | --- | --- | --- | --- |
| (novo) | Registrar compromisso | Pendente | Credor e valor original informados | — |
| Pendente | Pagar parcialmente | Parcialmente pago | Valor pago menor que o saldo devido | — |
| Pendente ou Parcialmente pago | Pagar o restante | Quitado | Saldo devido zerado | — |
| Pendente ou Parcialmente pago | Vencimento ultrapassado sem quitação total | Atrasado | — | Quitado → Atrasado (não retrocede) |
| Pendente, Parcialmente pago ou Atrasado | Renegociar condições | Renegociado | Novas condições registradas sem apagar o histórico original | — |
| Pendente | Cancelar | Cancelado | — | Quitado → Cancelado (não se aplica) |
| Quitado ou Cancelado | — | (estado final) | — | Nenhuma transição a partir destes estados |

## Conciliação (de um lançamento/movimentação importada)

| Estado atual | Ação | Próximo estado | Validações | Transições proibidas |
| --- | --- | --- | --- | --- |
| (nova movimentação importada) | Comparar contra lançamentos existentes | Não analisado → um dos abaixo | Critérios da seção "Conciliação" em `PROCESSOS.md` aplicados | — |
| Não analisado | Nenhum critério bate | Sem correspondência | — | — |
| Não analisado | Um ou mais critérios fortes batem | Correspondência alta | Valor, data e conta/cartão coincidem | — |
| Não analisado | Alguns critérios batem, mas não todos | Possível correspondência | — | — |
| Não analisado | Movimentação idêntica a uma já conciliada anteriormente | Duplicado | Mesmo identificador bancário ou mesma combinação valor+data+conta já conciliada | — |
| Não analisado | Falha ao interpretar o arquivo/linha | Erro de importação | — | — |
| Correspondência alta ou Possível correspondência | Família confirma a correspondência | Conciliado | Confirmação manual explícita (`REGRAS-DE-NEGOCIO.md`, regra 8) | Possível correspondência → Conciliado sem confirmação manual |
| Possível correspondência ou Sem correspondência | Família decide não conciliar | Ignorado | — | — |
| Conciliado | Família decide desfazer | Não analisado (reaberto) | Ação explícita de "desconciliar" | Conciliado → outro estado sem ação explícita |
| Duplicado ou Erro de importação | Correção manual | Não analisado (reaberto para nova análise) | — | — |

## Meta (de economia ou meta financeira nomeada)

| Estado atual | Ação | Próximo estado | Validações | Transições proibidas |
| --- | --- | --- | --- | --- |
| (nova) | Configurar meta | Configurada | Percentual ou valor, e competência ou prazo definidos | — |
| Configurada | Início do acompanhamento no período vigente | Em acompanhamento | — | — |
| Em acompanhamento | Progresso calculado atinge o alvo | Atingida | Cálculo de disponibilidade segura/progresso não negativo no fechamento do período (`CALCULOS-E-INDICADORES.md`) | — |
| Em acompanhamento | Progresso indica risco de não atingir, mas período ainda não fechou | Em risco | — | Atingida → Em risco (só se aplica antes do fechamento do período) |
| Em acompanhamento ou Em risco | Período fecha sem atingir o alvo | Não atingida | — | — |
| Configurada, Em acompanhamento ou Em risco | Cancelar a meta | Cancelada | — | Atingida ou Não atingida → Cancelada (um período já fechado não é cancelado, apenas historicamente registrado) |
| Atingida, Não atingida ou Cancelada | — | (estado final daquele período/meta) | — | Uma meta recorrente (ex.: meta de economia mensal) reabre um novo ciclo em "Configurada" a cada nova competência, não reaproveita o mesmo registro do período anterior |

## Item na lixeira (qualquer entidade excluível)

| Estado atual | Ação | Próximo estado | Validações | Transições proibidas |
| --- | --- | --- | --- | --- |
| Ativo | Excluir | Na lixeira | Registrar quem excluiu e quando (`PROCESSOS.md`, seção 18) | — |
| Na lixeira | Restaurar | Ativo | Volta a participar de todos os cálculos ativos | — |
| Na lixeira | Excluir definitivamente | Excluído definitivamente | Ação irreversível, distinta da exclusão para a lixeira | — |
| Excluído definitivamente | — | (estado final) | — | Nenhuma transição a partir deste estado |

Regra transversal a esta tabela, válida para toda entidade do Financeiro LeS: **um item em "Na
lixeira" nunca participa de nenhum cálculo ativo** (saldo, orçamento, dashboard, Simulador) — ver
`REGRAS-DE-NEGOCIO.md`, regra 12.

## Documentos relacionados

- `PROCESSOS.md` — descrição dos fluxos onde cada transição ocorre.
- `REGRAS-DE-NEGOCIO.md` — regras que restringem as transições acima.
- `CALCULOS-E-INDICADORES.md` — fórmulas usadas para decidir transições baseadas em cálculo
  (ex.: meta atingida/em risco/não atingida).
- `PENDENCIAS.md` — pontos ainda sem definição completa (ex.: critério exato de fechamento de
  período de uma meta).
