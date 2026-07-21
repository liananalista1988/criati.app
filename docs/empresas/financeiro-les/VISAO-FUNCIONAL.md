# Visão Funcional — Financeiro LeS

Documento de consolidação funcional da empresa piloto **Financeiro LeS**, a primeira empresa real
a ser cadastrada na plataforma Criati. Esta é uma tarefa exclusivamente de análise e planejamento
(`LES-F1-001`) — nenhum código, migration, endpoint ou tela foi implementado a partir deste
documento. A empresa `Financeiro LeS` ainda **não** foi cadastrada na plataforma.

Relação com a aplicação `FINANCEIRO` já existente na Criati (ver `docs/FINANCEIRO.md`): o módulo
genérico atual cobre apenas contas, categorias, lançamentos e um dashboard simples — não cobre
cartões de crédito, faturas, parcelas, contas a pagar recorrentes, empréstimos, simulador,
orçamento, conciliação ou categorização automática (todos registrados em `docs/FINANCEIRO.md`
como "Limitações desta fase"). A visão funcional abaixo é **maior** do que o módulo `FINANCEIRO`
genérico atual. Se o Financeiro LeS será construído estendendo esse módulo, como um módulo novo
dedicado, ou como uma combinação dos dois é uma decisão de arquitetura que **não** foi tomada
nesta tarefa — ver `docs/empresas/financeiro-les/PENDENCIAS.md`.

## Problema

O problema central que o Financeiro LeS deverá resolver é o **descontrole financeiro causado
principalmente pelo uso excessivo e pouco planejado de cartões de crédito**. A família consegue
ver o extrato bancário e as faturas isoladamente, mas não consegue enxergar com clareza:

- o gasto real do mês (débito + crédito, sem duplicidade);
- o quanto já está comprometido nos meses seguintes (parcelas, assinaturas, contas recorrentes);
- se uma nova compra é segura antes de ser feita.

## Público

- **Empresa/tenant**: `Financeiro LeS`, controle financeiro pessoal e residencial de uma família.
- **Usuários iniciais**: duas pessoas da mesma família, cada uma com login próprio (e-mail e
  senha), mesmo nível de acesso e visão compartilhada de todos os dados.
- Ambas as pessoas podem visualizar, cadastrar, editar, categorizar, conciliar e excluir
  qualquer dado — não existe área financeira isolada por pessoa, apenas filtros por pessoa.
- Todos os gastos residenciais são compartilhados entre as duas pessoas; os registros podem
  indicar quem realizou a operação, para fins de filtro e histórico, não de restrição de acesso.
- Autenticação em dois fatores fica para evolução futura (fora do MVP).
- Comprovantes e documentos são privados aos usuários autorizados da empresa (nunca públicos).

## Objetivos

O sistema deverá ajudar a família a responder, no mínimo:

- quanto dinheiro existe atualmente;
- quanto entrou no mês;
- quanto foi gasto no débito;
- quanto foi comprado no crédito;
- qual foi o gasto real total;
- quanto efetivamente saiu das contas;
- quanto já está comprometido nos próximos meses;
- quais faturas precisam ser pagas;
- quanto ainda existe disponível no orçamento;
- se a meta de economia será atingida;
- se uma nova compra comprometerá o orçamento;
- quanto terceiros ainda devem à família;
- quanto a família deve a terceiros;
- quais categorias mais consomem a renda;
- se os gastos aumentaram ou diminuíram.

## Módulos identificados

| Módulo | Resumo |
| --- | --- |
| Empresa e usuários | `Financeiro LeS`, duas pessoas, mesmo nível de acesso, visão compartilhada |
| Contas bancárias | 4 contas iniciais (2 pessoas × 2 bancos), saldo consolidado sem dupla contabilização |
| Receitas | Salários fixos, recorrência, previsto × recebido |
| Categorias | Moradia, Alimentação, Saúde, Transporte, Financeiro, Compromissos e doações, Outras |
| Contas a pagar | Únicas e recorrentes, geração automática de ocorrências, estados de pagamento |
| Cartões de crédito | 4 cartões iniciais, físicos e virtuais, limite bancário × limite saudável |
| Compras no crédito | Parceladas ou à vista, duas visões (decisão × mensal) |
| Faturas | Competência, período, estados, pagamento integral/parcial |
| Estornos e cancelamentos | Vinculados à compra original, nunca tratados como receita |
| Assinaturas | Recorrência, projeção mensal/anual, histórico de reajustes |
| Orçamento e limites | Limite geral, por categoria, limite saudável do cartão, alertas (nunca bloqueio) |
| Meta de economia | Percentual inicial de 7% (configurável, não fixo no código) |
| Metas financeiras | Reduzir fatura, quitar dívidas, reserva, redução por categoria |
| Simulador de Gastos | Funcionalidade central — simula decisões antes de assumir despesas |
| Sugestão de forma de pagamento | Comparação numérica dentro do Simulador |
| Compromissos a pagar | Obrigações fora do cartão (empréstimos recebidos, dívidas informais) |
| Empréstimos concedidos | Valores emprestados a terceiros, com controle de recebimento |
| Compras para terceiros | Exposição financeira a terceiros, separada do consumo residencial |
| Agenda financeira | Calendário de contas, faturas, parcelas, assinaturas e compromissos |
| Google Calendar | Evolução planejada, visão funcional apenas, sem detalhamento técnico |
| Mensagens de cobrança | Modelos configuráveis, cópia manual (envio automático fora do MVP) |
| Importação de extratos/faturas | OFX, CSV, Excel, PDF; Banco do Brasil e Banco Inter |
| Conciliação | Prevenção de duplicidade, sugestão de correspondência, nunca automática irreversível |
| Categorização automática | Regras por descrição/estabelecimento/conta/cartão/pessoa |
| Dashboard | Tela inicial, resumo consolidado, comparações mês a mês |
| Lixeira e histórico | Exclusão reversível, histórico de alterações importantes |
| Comprovantes e anexos | Privados à empresa, vinculados ao registro correto |
| Exportação e backup | PDF, Excel, exportação completa, backup recuperável |
| Responsividade | Desktop e celular, ação de destaque mobile: "Simular nova despesa" |

## Regras centrais

- **Transferências entre contas próprias não são receita nem despesa** — o saldo consolidado
  precisa evitar dupla contabilização.
- **A categorização é aplicada à compra inteira**, nunca separadamente por parcela; a parcela
  herda a categoria da compra original.
- **O limite saudável do cartão é separado do limite bancário** — o sistema apenas alerta,
  nunca bloqueia um lançamento.
- **Compras para terceiros no cartão da família** ocupam limite, entram na fatura e afetam o
  fluxo de caixa, mas não entram como consumo residencial — geram um valor a receber e compõem
  o indicador de **exposição financeira a terceiros**.
- **Estornos permanecem vinculados à compra original** e nunca são classificados como receita
  comum.
- **O pagamento recorrente do apartamento** (PIX mensal para uma pessoa) é classificado como
  despesa de moradia associada a um favorecido, com recorrência mensal e conciliação — nunca
  tratado genericamente apenas como "PIX enviado".
- **O Simulador de Gastos e as sugestões de forma de pagamento sempre alertam, nunca bloqueiam**
  a operação, e a recomendação sempre expõe os números usados (nunca uma recomendação opaca
  baseada apenas em texto gerado por IA).
- **Toda exclusão vai para lixeira** (reversível); alterações importantes mantêm histórico
  (valor, categoria, vencimento, data, status, conta, cartão, pessoa responsável). Não é
  necessário avisar a outra pessoa após uma alteração.
- **Anexos e comprovantes são privados** aos usuários autorizados da empresa.

## Fluxos gerais (nível funcional, não técnico)

1. **Registrar receita** → conta de destino → renda tratada de forma conjunta na visão familiar.
2. **Registrar despesa/pagamento (débito ou conta a pagar)** → categoria → conta → reflete no
   saldo consolidado e no gasto real do período.
3. **Registrar compra no crédito** → cartão → categoria → parcelamento (se houver) → compõe a
   fatura correspondente → gera as duas visões (decisão e mensal).
4. **Fechar/pagar fatura** → integral (regra normal da família), parcial ou mínima (com alerta
   de risco) → reflete no saldo da conta usada para pagamento.
5. **Simular uma nova decisão de gasto** → comparar formas de pagamento → ver impacto no mês,
   nas faturas, no limite, na categoria e na meta de economia → decidir com números explícitos.
6. **Importar extrato/fatura** → conciliar com lançamentos existentes → resolver duplicidades →
   categorizar (manual ou por regra automática).
7. **Registrar compromisso ou empréstimo** (a pagar ou a receber) → acompanhar na agenda
   financeira → atualizar saldo pendente até a quitação.
8. **Consultar o dashboard** → visão consolidada de tudo o que está acima, com comparações entre
   períodos e contra o orçamento/meta.

## Indicadores (visão consolidada)

Ver a lista completa em `MVP.md`, seção "Dashboard" — os indicadores aqui descritos alimentam
tanto o dashboard quanto o Simulador de Gastos, e devem ser calculados a partir da mesma fonte de
verdade (nunca duas fórmulas divergentes para o mesmo número, mesmo princípio já adotado pelo
módulo `FINANCEIRO` genérico em `SaldoFinanceiroService`).

## Critérios de sucesso

- todas as contas e cartões cadastrados;
- gasto real mensal calculado corretamente (débito e crédito consolidados sem duplicidade);
- faturas projetadas e parcelas futuras visíveis;
- orçamento familiar e meta de economia calculados;
- pelo menos uma simulação de compra concluída de ponta a ponta;
- valores de terceiros separados do consumo residencial;
- ausência de duplicidade após conciliação;
- dashboard compreensível para os dois usuários.

## Documentos relacionados

- `MVP.md` — separação entre MVP, fase seguinte e evoluções futuras.
- `GLOSSARIO.md` — definição dos termos centrais.
- `PENDENCIAS.md` — decisões que dependem de confirmação futura.
