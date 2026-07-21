# Regras de Negócio — Financeiro LeS

Regras de negócio verificáveis do Financeiro LeS, organizadas por tema, mais os casos extremos
obrigatórios e a recomendação sobre o reaproveitamento do módulo `FINANCEIRO` genérico já
existente na plataforma. Complementa `PROCESSOS.md` (onde cada regra se aplica dentro do fluxo)
sem repetir a descrição dos fluxos.

## 1. Regras de consolidação e patrimônio

- Transferência entre contas próprias nunca é receita nem despesa consolidada — ela apenas move
  dinheiro entre contas da mesma família (`PROCESSOS.md`, seção 2).
- O saldo consolidado nunca conta a mesma movimentação duas vezes; uma transferência conciliada
  entre duas contas próprias soma zero no total, nunca soma como entrada em uma conta e depois
  novamente como saída de outra dentro do consolidado.
- Uma conta inativa mantém seu saldo histórico no consolidado (mesmo princípio já adotado pelo
  módulo `FINANCEIRO` genérico, ver `docs/FINANCEIRO.md`).
- Dinheiro em espécie, quando cadastrado, segue as mesmas regras de qualquer outra conta.

## 2. Regras de receita e despesa

- Renda é tratada de forma conjunta na visão familiar — não existe "renda da Pessoa 1" isolada
  da "renda da Pessoa 2" para fins de orçamento e meta de economia (o filtro por pessoa existe
  apenas para consulta, não para segregação de cálculo).
- Um lançamento de débito só é editável enquanto estiver previsto ou realizado; depois de
  conciliado, qualquer correção deve ocorrer por um novo lançamento de ajuste, nunca sobrescrevendo
  o valor já conciliado (protege o histórico de conciliações passadas).
- Cancelamento preserva o registro — nunca há exclusão física de receita ou despesa; um
  cancelamento é sempre uma transição de estado, nunca a remoção do dado (mesmo princípio do
  `LancamentoFinanceiro` do módulo genérico).

## 3. Regras de cartão de crédito

- Um cartão virtual sempre está vinculado a exatamente um cartão principal e compartilha com ele
  limite, fatura, vencimento e forma de pagamento — um cartão virtual nunca tem limite, fatura ou
  vencimento próprios.
- O limite ocupado por uma compra é o **valor total** da compra no momento em que ela é feita,
  independentemente de quantas parcelas existirem — parcelar não reduz o impacto no limite no
  momento da compra.
- O limite saudável é sempre um valor **igual ou menor** que o limite bancário, nunca maior; ele
  serve apenas para alertar a família, nunca para bloquear uma compra ou um lançamento.
- O melhor dia de compra é calculado a partir do dia de fechamento do cartão — é uma informação
  de apoio à decisão, nunca uma restrição de uso do cartão.

## 4. Regras de compra no crédito e parcelamento

- A categoria de uma compra parcelada é definida uma única vez, na compra original, e herdada por
  todas as suas parcelas — uma parcela nunca pode ter categoria diferente da compra que a gerou
  (exceto por uma correção manual explícita na compra original, que deve recalcular todas as
  parcelas ainda não pagas).
- O gasto real do mês soma o **valor total** de cada compra no crédito no mês em que a compra foi
  feita (visão da decisão de consumo) — nunca a soma das parcelas daquele mês.
- O orçamento e o comprometimento mensal somam o **valor de cada parcela** no mês em que ela é
  cobrada na fatura (visão mensal) — essas duas somas (gasto real e comprometimento mensal) usam
  a mesma compra, mas nunca devem ser apresentadas como o mesmo número (ver
  `VISAO-FUNCIONAL.md`, "Duas visões do crédito").
- Pagar a fatura nunca gera um novo lançamento de despesa/consumo — o consumo já foi reconhecido
  quando a compra foi feita (`PROCESSOS.md`, seção 4).

## 5. Regras de estornos e cancelamentos

- Um estorno (integral, parcial, ou recebido em fatura futura) permanece sempre vinculado à
  compra original — nunca é um lançamento solto no sistema.
- Um estorno nunca é classificado como receita comum, mesmo que reduza o valor devido na fatura
  — ele é o desfazimento total ou parcial de um consumo já reconhecido, não uma nova entrada de
  dinheiro.
- Uma compra cancelada antes de compor qualquer fatura simplesmente deixa de gerar parcelas;
  uma compra cancelada depois de já ter parcelas em faturas fechadas precisa de estorno das
  parcelas restantes, não de exclusão retroativa das já cobradas.

## 6. Regras de contas a pagar e compromissos

- Uma conta recorrente com valor variável é sempre gerada com um valor estimado e depois
  atualizada para o valor real; a diferença entre estimado e realizado nunca é aplicada
  retroativamente a um mês já fechado no orçamento (`PROCESSOS.md`, seção 7).
- Um valor recebido como empréstimo (compromisso a pagar) nunca é somado à renda do mês nem ao
  gasto real — ele aumenta o caixa disponível momentaneamente, mas gera uma obrigação de mesmo
  valor no comprometimento futuro (`PROCESSOS.md`, seção 8).
- O pagamento do apartamento (PIX recorrente para uma pessoa) é sempre classificado como despesa
  de moradia com recorrência mensal e favorecido definido — nunca genericamente como "PIX
  enviado" (regra explícita do contexto do produto, ver `VISAO-FUNCIONAL.md`).

## 7. Regras de empréstimos concedidos e compras para terceiros

- Um empréstimo concedido reduz o caixa disponível no momento em que é feito, mas nunca é
  contado como consumo da residência — ele vira um valor a receber (`PROCESSOS.md`, seção 9).
- Uma compra para terceiro no cartão da família ocupa limite e entra na fatura exatamente como
  qualquer outra compra, mas **nunca** entra no consumo residencial reportado no dashboard — ela
  compõe separadamente a exposição financeira a terceiros (`PROCESSOS.md`, seção 10;
  fórmula em `CALCULOS-E-INDICADORES.md`).
- Um valor a receber (de empréstimo ou de compra para terceiro) nunca é somado ao saldo
  disponível como se já estivesse garantido — ele é sempre apresentado como um indicador
  separado, mesmo quando o devedor tem um bom histórico de pagamento.
- O atraso do terceiro em uma compra feita para ele nunca altera a obrigação da família com o
  banco emissor do cartão — a fatura vence e deve ser paga independentemente de o terceiro ter
  reembolsado a família ou não.

## 8. Regras de conciliação

- Nenhuma conciliação é automática e irreversível no MVP — toda correspondência sugerida precisa
  de confirmação da família antes de virar conciliação definitiva (`PROCESSOS.md`, seção 11).
- Um lançamento já conciliado só pode ser desfeito por uma ação explícita de "desconciliar",
  nunca por uma nova importação que simplesmente sobrescreva a conciliação anterior.
- A mesma movimentação bancária nunca pode ser conciliada com dois lançamentos diferentes ao
  mesmo tempo, e o mesmo lançamento nunca pode ser conciliado com duas movimentações diferentes
  ao mesmo tempo.

## 9. Regras de categorização

- Uma regra de categorização mais específica (por estabelecimento) sempre prevalece sobre uma
  regra mais genérica (por texto parcial), independente da ordem de criação (`PROCESSOS.md`,
  seção 12).
- A aplicação retroativa de uma regra nova a lançamentos antigos é sempre uma ação explícita da
  família, nunca automática ao criar a regra.
- Desativar uma regra não altera a categorização já aplicada a lançamentos passados — afeta
  apenas lançamentos futuros a partir da desativação.

## 10. Regras de orçamento e meta de economia

- O sistema **nunca bloqueia** um lançamento por ultrapassar o orçamento, o limite saudável do
  cartão ou a meta de economia — em todos os casos, apenas alerta.
- O percentual e o valor da meta de economia nunca ficam fixos em código — são sempre
  configuráveis pela família, por competência, com possibilidade de exceção mensal (`MVP.md`).
- A disponibilidade segura sempre desconta a meta de economia da renda prevista, junto com
  despesas, faturas, parcelas e compromissos — nunca é calculada sem considerar a meta
  configurada (fórmula em `CALCULOS-E-INDICADORES.md`).

## 11. Regras do Simulador de Gastos

- O Simulador sempre usa as mesmas fórmulas e os mesmos dados do restante do sistema — nunca uma
  fórmula paralela exclusiva da simulação, para que o resultado simulado seja diretamente
  comparável ao dashboard real.
- Uma recomendação do Simulador sempre expõe os números usados no cálculo — nunca é apresentada
  apenas como um texto de recomendação sem os valores por trás.
- O Simulador sempre alerta, nunca bloqueia; salvar como lançamento real é sempre uma ação
  explícita e separada da simulação em si.

## 12. Regras de lixeira e histórico

- Um item na lixeira nunca participa de nenhum cálculo ativo (saldo, orçamento, dashboard,
  simulador) — confirmado como regra obrigatória do produto (`PROCESSOS.md`, seção 18).
- Restaurar um item devolve exatamente o estado anterior à exclusão, incluindo seus anexos.
- Não é necessário notificar a outra pessoa da residência após uma alteração ou exclusão — ambas
  têm o mesmo nível de acesso e podem consultar o histórico quando quiserem.

## Casos extremos obrigatórios

| Caso extremo | Comportamento esperado |
| --- | --- |
| Compra lançada duas vezes | A conciliação (seção 8) deve identificar a duplicidade antes de confirmar um segundo lançamento como novo; se já foi lançada duas vezes por engano, uma das duas deve ser cancelada, nunca simplesmente excluída (preserva o histórico do erro). |
| Pagamento da fatura importado como despesa | Não deve ser aceito como um novo lançamento de despesa/consumo — deve ser reconhecido no processo de conciliação como o pagamento da fatura correspondente (regra da seção 4/6 de `PROCESSOS.md`), nunca somado ao gasto real. |
| Cartão virtual e principal na mesma fatura | É o comportamento esperado e correto — cartão virtual sempre compartilha a fatura do principal; a fatura deve mostrar as compras de ambos claramente identificadas por qual cartão (físico ou virtual) foi usado. |
| Transferência entre contas próprias | Nunca conta como receita nem despesa consolidada (regra 1). |
| Estorno parcial | Reduz o valor devido da compra original nas parcelas ainda não pagas, sem alterar parcelas já pagas; nunca é lançado como receita. |
| Parcela sem compra original identificada | Não deveria ocorrer no fluxo normal (toda parcela nasce de uma compra); se ocorrer por uma falha de importação, a parcela deve ficar em um estado que exija revisão manual antes de compor qualquer total, nunca ser somada silenciosamente ao comprometimento futuro. |
| Fatura paga parcialmente | Ver `PROCESSOS.md`, seção 5, "Efeito de uma fatura paga parcialmente". |
| Empréstimo atrasado | Aparece na agenda financeira como atrasado (`PROCESSOS.md`, seção 9); não altera o valor original devido, apenas seu estado. |
| Recebimento maior que o saldo a receber | Não deve ser aceito como está — o excedente deve ser tratado como um valor separado (ex.: início de um novo crédito ou devolução), nunca simplesmente zerando o saldo a receber com sobra silenciosa. Critério exato de tratamento do excedente ainda não definido — ver `PENDENCIAS.md`. |
| Compra para terceiro sem reembolso | Permanece como valor a receber em aberto, contando integralmente na exposição financeira a terceiros até ser quitada, cancelada ou renegociada — nunca é removida do indicador só porque está atrasada. |
| Conta recorrente cancelada | Encerra a geração de novas ocorrências a partir do cancelamento; ocorrências já geradas e pendentes permanecem até serem pagas ou também canceladas explicitamente. |
| Valor variável sem atualização | Permanece com o valor estimado no orçamento até ser atualizado manualmente; o sistema não deve travar nem impedir o fechamento do mês por causa disso, apenas manter a informação como estimada (não realizada). |
| Arquivo importado duas vezes | O processo de conciliação (seção 8) deve identificar que as movimentações já foram conciliadas na primeira importação e não criar duplicatas na segunda. |
| Regra de categorização conflitante | Resolvida pela ordem de prioridade da seção 9 (mais específica vence); em caso de empate de especificidade, a regra mais recente prevalece (`PROCESSOS.md`, seção 12). |
| Item excluído que já estava conciliado | Vai para a lixeira como qualquer outro item; enquanto estiver na lixeira, não participa de nenhum cálculo (regra 12) — se restaurado, volta a valer como conciliado. |
| Alteração retroativa de categoria | Permitida na compra/lançamento original a qualquer momento; quando a compra tem parcelas ainda não pagas, a mudança deve recalcular a categoria dessas parcelas (regra 4); parcelas já pagas mantêm a categoria com que foram pagas. |
| Saldo inicial incorreto | Corrigível enquanto a conta não tiver nenhum lançamento (mesmo princípio do módulo `FINANCEIRO` genérico, `docs/FINANCEIRO.md`); depois do primeiro lançamento, a correção deve ser feita por um lançamento de ajuste, nunca sobrescrevendo o saldo inicial original. |
| Mês sem renda recebida | O orçamento e a disponibilidade segura desse mês são calculados normalmente, apenas com renda prevista igual a zero (ou ao valor efetivamente recebido) — o sistema não deve travar nem esconder despesas e compromissos já assumidos só porque não houve renda. |
| Meta de economia impossível (despesas + compromissos já ultrapassam a renda prevista) | O sistema deve mostrar a disponibilidade segura como negativa e classificar o risco como alto/crítico (ver `CALCULOS-E-INDICADORES.md`) — nunca esconder o resultado negativo nem impedir a consulta. |

## Recomendação sobre o módulo `FINANCEIRO` existente

Esta análise responde à pendência de arquitetura registrada em `PENDENCIAS.md` na tarefa
`LES-F1-001`. **É uma recomendação, não uma decisão implementada** — nenhum código foi alterado
nesta tarefa.

### Opções avaliadas

**Opção A — Evoluir o módulo `FINANCEIRO` genérico existente.** Reaproveitaria diretamente
`ContaFinanceira`, `CategoriaFinanceira`, `LancamentoFinanceiro`, permissões e dashboard já
testados (ver `docs/FINANCEIRO.md`). Risco: esse módulo é hoje um produto genérico, oferecido a
qualquer empresa da plataforma (aplicação `FINANCEIRO`, catálogo de aplicações). Forçar conceitos
específicos do uso residencial — cartão, fatura, parcela, simulador, exposição a terceiros — para
dentro dele acoplaria um caso de uso muito específico (família, cartão de crédito pessoal) a um
módulo pensado para ser genérico para qualquer empresa cliente, o que complicaria o dashboard e
as regras para clientes que não têm esse perfil de uso.

**Opção B — Domínio completamente separado para o Financeiro LeS.** Isolaria totalmente o
produto residencial, sem nenhum risco de vazamento de conceito específico para o módulo
genérico. Risco: duplicaria integralmente conta, categoria e lançamento — conceitos que são
genuinamente os mesmos independente do contexto (uma conta bancária é uma conta bancária) — 
duplicando também o isolamento multiempresa, o modelo de permissões e o cálculo de saldo já
validados e testados no módulo existente, sem ganho real de clareza.

**Opção C — Núcleo compartilhado (conta, categoria, lançamento simples) + extensões
específicas (cartão, fatura, parcela, simulador, empréstimo, exposição a terceiros).**
Reaproveita a base já validada para tudo que é genuinamente igual entre um uso empresarial e um
uso residencial (conta bancária, categoria, um lançamento simples de débito), e cria conceitos
novos e específicos apenas para o que realmente é diferente (cartão de crédito com fatura e
parcela, simulador, empréstimo, exposição a terceiros) — conceitos que não fazem sentido para a
maioria dos outros clientes da aplicação `FINANCEIRO` genérica. Isso é consistente com o padrão
de catálogo de aplicações já existente na plataforma (`EmpresaAplicacao`, habilitação por
empresa) — a extensão específica poderia ser habilitada apenas para a `Financeiro LeS` e para
futuros clientes com o mesmo perfil de uso, sem afetar quem usa só o `FINANCEIRO` genérico.

### Critérios considerados

| Critério | Opção A | Opção B | Opção C |
| --- | --- | --- | --- |
| Reaproveitamento do já validado (conta, categoria, saldo, permissões) | Alto | Nenhum | Alto |
| Acoplamento entre residencial e genérico | Alto (risco) | Nenhum | Baixo e intencional (núcleo comum) |
| Risco de duplicação | Nenhum | Alto | Baixo |
| Impacto multiempresa (outros clientes do `FINANCEIRO` genérico) | Alto (regras específicas vazam para todos) | Nenhum | Nenhum (extensão isolada por aplicação) |
| Manutenção | Um módulo cada vez mais complexo e genérico ao mesmo tempo | Dois módulos a manter, com lógica repetida | Um núcleo simples + uma extensão coesa |
| Evolução para novos clientes com perfil residencial | Difícil sem afetar clientes genéricos | Fácil, mas repete trabalho a cada novo domínio | Fácil — reaproveita o núcleo, replica só a extensão |
| Possibilidade de produtos financeiros diferentes no futuro | Difícil de diferenciar dentro do mesmo módulo | Cada produto novo duplica tudo de novo | Cada produto novo é uma nova extensão sobre o mesmo núcleo |

### Recomendação

**Opção C — núcleo compartilhado (conta, categoria, lançamento simples de débito) com extensões
específicas para o uso residencial (cartão, compra no crédito, fatura, parcela, compromisso a
pagar, empréstimo concedido, exposição a terceiros, meta de economia, Simulador de Gastos).**

Esta recomendação **não implica** que a compra no crédito deva reutilizar literalmente a entidade
`LancamentoFinanceiro` do módulo genérico — a modelagem de dados da próxima etapa deve avaliar se
compra no crédito é uma entidade nova (mais provável, dado o volume de atributos específicos:
cartão, fatura, parcelas, estabelecimento) que apenas referencia a mesma `CategoriaFinanceira` já
existente, ou se há um caminho de reaproveitamento mais direto. Essa é uma decisão de modelagem
de dados, fora do escopo desta tarefa.

## Documentos relacionados

- `PROCESSOS.md`
- `ESTADOS-E-TRANSICOES.md`
- `CALCULOS-E-INDICADORES.md`
- `PENDENCIAS.md`
- `docs/FINANCEIRO.md` (módulo genérico existente, referência para a recomendação acima)
