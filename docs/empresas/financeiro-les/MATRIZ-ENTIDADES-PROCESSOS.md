# Matriz Entidades × Processos — Financeiro LeS

Participação de cada entidade (`MODELO-DE-DADOS.md`) em cada processo (`PROCESSOS.md`). Usada
para detectar entidades excessivamente acopladas — uma entidade que participa de quase todos os
processos (ex.: `ContaFinanceira`) é esperada, por ser um conceito central; uma entidade de
extensão que aparecesse em processos totalmente fora do seu domínio seria um sinal de acoplamento
indevido a investigar na modelagem técnica.

`X` = participa diretamente (é lida, escrita ou referenciada nesse processo). Célula vazia = não
participa.

| Entidade | Receitas | Débito | Crédito | Faturas | Parcelamento | Contas a pagar | Empréstimos | Terceiros | Conciliação | Orçamento | Simulador | Dashboard | Lixeira | Auditoria |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Pessoa | X | X | X | | | | | | | | X | X | | X |
| ParteFinanceira (favorecido) | | | X | | | X | X | X | | | X | | | |
| ContaFinanceira | X | X | X | | | X | X | X | X | | X | X | X | X |
| CategoriaFinanceira | X | X | X | | | X | | | X | X | X | X | X | X |
| LancamentoFinanceiro | X | X | | | | | | | X | X | X | X | X | X |
| Anexo | | X | X | | | X | X | X | | | | | X | |
| Recorrencia | X | | | | | X | | | | | | | | |
| InstituicaoFinanceira / Bandeira | | | X | | | | | | | | | | | |
| CartaoCredito | | | X | X | X | | | | X | X | X | X | X | X |
| CompraCredito | | | X | X | X | | | X | X | X | X | X | X | X |
| ParcelaCompra | | | X | X | X | | | X | X | X | X | X | X | X |
| Fatura | | | X | X | X | | | | X | X | X | X | X | X |
| PagamentoFatura | | | | X | | | | | X | | | X | | X |
| ContaAPagar / OcorrenciaContaAPagar | | | | | | X | | | X | X | X | X | X | X |
| CompromissoAPagar | | | | | | | X | | | X | X | X | X | X |
| EmprestimoConcedido | | | | | | | X | | | | X | X | X | X |
| Recebivel | | | | | | | X | X | | | X | X | X | X |
| Orcamento | | | | | | X | | | | X | X | X | | X |
| MetaEconomia | X | | | | | | | | | X | X | X | | X |
| Conciliação (Arquivo/Transação/Vínculo) | X | X | X | X | X | X | | | X | | | | | X |
| RegraCategorizacao | | X | X | | | X | | | X | | | | X | X |

## Leitura da matriz

- **Entidades do núcleo** (`Pessoa`, `ParteFinanceira`, `ContaFinanceira`, `CategoriaFinanceira`,
  `LancamentoFinanceiro`, `Anexo`, `Recorrencia`, `InstituicaoFinanceira`/`Bandeira`) participam de
  muitos processos, como esperado — são conceitos genuinamente transversais, usados tanto no
  núcleo genérico quanto em todas as extensões.
- **`CompraCredito` e `ParcelaCompra`** são as entidades de extensão com maior número de
  participações (10 processos cada) — coerente com serem o centro do problema que o produto
  resolve (`VISAO-FUNCIONAL.md`: descontrole por cartão de crédito). Não é um sinal de
  acoplamento indevido, é o núcleo do domínio residencial.
- **Nenhuma entidade de extensão aparece nos processos "Receitas" ou "Débito"** além de
  `Conciliação` (que precisa comparar contra qualquer tipo de lançamento, débito ou crédito, para
  evitar duplicidade) e `RegraCategorizacao`/`Anexo` (que também se aplicam a débito) — confirma
  que receita e despesa no débito permanecem inteiramente no núcleo, sem depender de nenhum
  conceito específico do Financeiro LeS. Isso é o resultado esperado da Opção C
  (`ARQUITETURA-FUNCIONAL.md`).
- **`PagamentoFatura` não participa de "Parcelamento"** diretamente — ele quita a fatura como um
  todo (ou parcialmente), não parcelas individuais; a ligação entre pagamento e parcela é sempre
  indireta, via `Fatura`. Isso é intencional: evita que o pagamento precise conhecer o detalhe de
  cada parcela.
- **`Orcamento` e `MetaEconomia` não participam de "Terceiros"** diretamente — a exposição
  financeira a terceiros é um indicador calculado a partir de `Recebivel` (ver
  `MODELO-DE-DADOS.md`), não um valor que o orçamento ou a meta de economia precisem conhecer
  como entrada; ambos continuam calculados a partir apenas do consumo residencial e do fluxo de
  caixa da família, coerente com a regra de que valores a receber não entram na disponibilidade
  segura (`REGRAS-DE-NEGOCIO.md`).

## Documentos relacionados

- `MODELO-DE-DADOS.md` — definição de cada entidade listada aqui.
- `PROCESSOS.md` — definição de cada processo listado aqui.
- `ARQUITETURA-FUNCIONAL.md` — separação núcleo × extensão que esta matriz confirma na prática.
