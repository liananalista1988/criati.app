# Arquitetura Funcional — Financeiro LeS

Decisão arquitetural, separação de responsabilidades entre núcleo e extensões, estratégia
multiempresa, e as decisões documentais resolvidas nesta tarefa para as pendências estruturais
identificadas em `LES-F1-002`. Complementa `MODELO-DE-DADOS.md` (as entidades) sem repetir os
campos já descritos lá.

## 1. Decisão arquitetural

**Decisão confirmada: Opção C — núcleo financeiro compartilhado com extensões específicas do
Financeiro LeS.** Não foi encontrada, nesta análise, nenhuma descoberta técnica que torne essa
abordagem inviável — ao contrário, o módulo `FINANCEIRO` genérico já existente
(`docs/FINANCEIRO.md`) confirma que `ContaFinanceira`, `CategoriaFinanceira` e
`LancamentoFinanceiro` são conceitos simples e estáveis, adequados para reaproveitamento direto.

Por que não a Opção A (evoluir diretamente o módulo existente): a aplicação `FINANCEIRO` é hoje
um produto genérico oferecido a qualquer empresa da plataforma (catálogo de aplicações,
`docs/MODELO_MULTIEMPRESA.md`). Colocar cartão de crédito, fatura, parcela, simulador, empréstimo
e exposição a terceiros diretamente nela obrigaria **qualquer** cliente do `FINANCEIRO` genérico a
conviver com conceitos que só fazem sentido para um uso residencial — o dashboard, os relatórios
e as regras do módulo genérico ficariam mais complexos para todo mundo, mesmo quem nunca usará
cartão de crédito dentro do sistema.

Por que não a Opção B (domínio completamente separado): duplicaria integralmente conta,
categoria e lançamento simples — conceitos que são genuinamente os mesmos independentemente do
contexto (uma conta bancária tem saldo, titular e movimentações, ponto), junto com o isolamento
multiempresa, o cálculo de saldo e o modelo de permissões já validados e testados no módulo
existente, sem nenhum ganho real de clareza.

A Opção C reaproveita o que é genuinamente igual (núcleo) e isola o que é genuinamente diferente
(extensão), seguindo o mesmo padrão de catálogo de aplicações já usado pela plataforma — a
extensão pode ser habilitada apenas para empresas com esse perfil de uso, sem afetar quem usa só
o `FINANCEIRO` genérico.

## 2. Separação de responsabilidades: núcleo × extensão

### Núcleo financeiro compartilhado (reaproveitável por qualquer empresa)

Responsabilidade: registrar contas bancárias, categorizar entradas e saídas simples, calcular
saldo, e fornecer os conceitos de pessoa, favorecido, anexo e recorrência que **qualquer** domínio
financeiro (residencial ou empresarial) precisa.

Entidades: `ContaFinanceira`, `CategoriaFinanceira`, `LancamentoFinanceiro`, `Pessoa`,
`ParteFinanceira` (favorecido), `Anexo`, `Recorrencia`, `InstituicaoFinanceira`, `Bandeira`.

O núcleo **nunca** conhece cartão, fatura, parcela, simulador, empréstimo ou exposição a
terceiros — essas são responsabilidades exclusivas da extensão. Isso é o que permite a um futuro
cliente usar apenas o núcleo (aplicação `FINANCEIRO` genérica, como já existe hoje) sem herdar
nenhuma complexidade do Financeiro LeS.

### Extensão do Financeiro LeS (habilitada apenas para empresas com esse perfil de uso)

Responsabilidade: tudo que é específico do controle financeiro residencial centrado em cartão de
crédito — cartão, cartão virtual, compra no crédito, fatura, parcela, pagamento de fatura,
orçamento, limite saudável, meta de economia, conta a pagar avançada (com recorrência e valor
variável), compromisso a pagar, empréstimo concedido, compra para terceiro, recebível
(exposição financeira), conciliação, regra de categorização, simulador e projeção financeira.

A extensão **referencia** entidades do núcleo (uma `CompraCredito` referencia `CategoriaFinanceira`
e `Pessoa`; um `CartaoCredito` referencia `ContaFinanceira` como conta de pagamento) mas nunca as
substitui nem duplica seus campos.

**Habilitação por empresa**: seguindo o mesmo padrão já existente de `EmpresaAplicacao`
(`docs/MODELO_MULTIEMPRESA.md`), a extensão do Financeiro LeS deve ser um novo código de
aplicação no catálogo (nome exato a definir na modelagem técnica, ex.: `FINANCEIRO_RESIDENCIAL`),
habilitado apenas para a empresa `Financeiro LeS` e para futuros clientes com o mesmo perfil de
uso — nunca habilitado automaticamente junto com `FINANCEIRO`. Uma empresa pode ter `FINANCEIRO`
sem a extensão (uso genérico simples) ou `FINANCEIRO` **e** a extensão (uso residencial completo)
— a extensão sempre pressupõe o núcleo habilitado, nunca existe sozinha.

## 3. Estratégia multiempresa

Reaproveita integralmente os princípios já documentados em `docs/MODELO_MULTIEMPRESA.md` — nada
novo é criado aqui, apenas aplicado às entidades do Financeiro LeS.

- **Todas** as entidades do núcleo e da extensão são tabelas empresariais e possuem `empresa_id`
  obrigatório — não existe entidade financeira "global" por empresa (as únicas entidades
  realmente globais são `InstituicaoFinanceira` e `Bandeira`, quando cadastradas como catálogo da
  plataforma, com `empresa_id` nulo; uma empresa também pode cadastrar sua própria instituição ou
  bandeira, com `empresa_id` preenchido).
- Todo relacionamento entre entidades financeiras deve validar que ambas as pontas pertencem à
  **mesma empresa** antes de persistir ou consultar — ex.: `CompraCredito.cartao_utilizado_id`
  deve apontar para um `CartaoCredito` da mesma `empresa_id` da compra; `ParcelaCompra.fatura_id`
  deve apontar para uma `Fatura` da mesma empresa da parcela. Nenhuma entidade financeira de uma
  empresa pode se relacionar com entidade financeira de outra (regra obrigatória da tarefa,
  reafirmada aqui).
- Toda consulta busca sempre por `id + empresa_id` do contexto ativo, nunca por `id` isolado
  (mesmo padrão já documentado em `docs/MODELO_MULTIEMPRESA.md`, "Regras obrigatórias de
  segurança") — um identificador de outra empresa deve responder exatamente como inexistente,
  nunca revelando que o registro existe em outro tenant.
- **Nunca confiar no `empresa_id` recebido do frontend** — o contexto de empresa ativa vem
  exclusivamente da sessão do backend (`ContextoEmpresaService`, já implementado e documentado em
  `docs/MODELO_MULTIEMPRESA.md`), nunca de um campo enviado pelo cliente.
- Usuários acessam dados financeiros da empresa através do mesmo mecanismo de contexto de empresa
  ativa já existente — nenhum mecanismo de acesso novo é necessário só para o Financeiro LeS.
- **Superadministrador**: não recebe nenhum acesso direto às entidades financeiras de nenhuma
  empresa. Ele nunca tem um `usuario_empresa` (ver `docs/MODELO_MULTIEMPRESA.md`, "Superadministrador
  da Criati") e, portanto, nunca tem contexto de empresa ativa — todas as consultas financeiras
  exigem esse contexto (mesmo princípio já aplicado ao módulo `FINANCEIRO` genérico, ver
  `docs/FINANCEIRO.md`, "Permissões": *"Um Superadministrador sem vínculo empresarial nunca
  acessa o módulo"*). Esta tarefa **não** desenha nenhuma exceção a essa regra — nenhum acesso do
  Superadministrador aos dados financeiros de uma empresa sem contexto válido foi proposto.

## 4. Usuário, vínculo e pessoa da residência

Três conceitos distintos, sem sobreposição de dados:

- **Usuário**: identidade autenticada da plataforma (`Usuario`, já existente, global,
  identificado por e-mail). Não é alterado por esta tarefa.
- **Vínculo empresa–usuário**: define o acesso ao tenant (`UsuarioEmpresa`, já existente). Define
  se a pessoa pode operar na empresa `Financeiro LeS` e com qual perfil (`ADMINISTRADOR`,
  `GESTOR` ou `USUARIO` — ambas as duas pessoas da residência devem ter o mesmo perfil, conforme
  `VISAO-FUNCIONAL.md`: "mesmo nível de acesso").
- **Pessoa** (nova, núcleo): representa quem realizou, recebeu ou é titular de uma operação
  financeira dentro do domínio (ver `MODELO-DE-DADOS.md`). Vínculo com `usuario_id` é **opcional**
  — uma pessoa pode existir só para fins de referência (ex.: um filho sem login) sem nunca ter
  acessado o sistema.

O domínio financeiro nunca duplica nome, e-mail, senha ou qualquer outro dado de autenticação —
`Pessoa.nome` é um dado próprio do domínio financeiro (pode até divergir do nome cadastrado em
`Usuario`, ex.: um apelido usado internamente pela família), e `Pessoa.usuario_id` é a única
ponte entre os dois mundos.

## 5. Simulador de Gastos

O Simulador nunca persiste dados reais até uma confirmação explícita.

- **Dados que vêm do banco**: saldo consolidado atual, orçamento e limites vigentes, meta de
  economia configurada, faturas e parcelas já existentes, compromissos e recebíveis em aberto —
  tudo que já existe é lido normalmente (mesmas consultas usadas pelo dashboard).
- **Dados informados pelo usuário**: tipo de operação, valor, forma de pagamento, cartão/conta,
  parcelas, categoria, data, terceiro (quando aplicável) — nenhum desses dados é gravado
  enquanto a simulação não for confirmada.
- **Resultados transitórios**: o cenário calculado (impacto, projeções, classificação de risco,
  recomendação) existe apenas durante a simulação — não é uma entidade persistida por padrão. Se
  a família quiser manter um histórico de simulações consultadas (não confirmadas), isso pode ser
  avaliado como uma extensão futura opcional, não um requisito do MVP.
- **Quando um cenário pode ser salvo**: apenas por uma ação explícita e separada ("salvar como
  lançamento real"), que então executa o processo normal correspondente ao tipo de operação
  simulada (compra no crédito, despesa no débito, novo compromisso etc.) — a confirmação nunca
  acontece implicitamente ao apenas calcular o cenário.
- **Como evitar que simulações alterem saldos reais**: o cálculo do Simulador é sempre uma
  consulta (leitura), nunca uma escrita, até a confirmação explícita — a mesma garantia que
  qualquer consulta de leitura já oferece no restante da plataforma.

## 6. Projeção financeira

A projeção (usada no dashboard e no Simulador para os horizontes de 3, 6 e 12 meses) é construída
somando, para cada mês futuro: receitas previstas (recorrentes + configuradas manualmente),
contas recorrentes já geradas ou a gerar, parcelas já existentes com vencimento naquele mês,
faturas projetadas, compromissos com vencimento naquele mês, recebíveis com vencimento naquele
mês, o orçamento e a meta de economia vigentes — mais, no caso do Simulador, a nova operação
sendo simulada.

### Decisão sobre média histórica

**Confirmada a proposta da tarefa**: quando não houver um valor previsto informado manualmente
para uma receita ou despesa variável em um mês futuro, o sistema usa a **média dos últimos três
meses completos** como estimativa — nunca meses incompletos (o mês corrente, ainda em andamento,
nunca entra nessa média) — e essa estimativa pode sempre ser substituída manualmente pela
família. Esta decisão resolve a pendência "janela da média histórica" registrada em
`PENDENCIAS.md` (LES-F1-002) para o caso específico da projeção; o mesmo critério de três meses
completos deve ser usado também para o indicador de "variação mensal × média histórica" citado em
`CALCULOS-E-INDICADORES.md`, para manter consistência entre os dois cálculos.

## 7. Critérios de risco

**Confirmada a proposta da tarefa**, como os quatro níveis usados tanto pela fatura quanto pelo
Simulador de Gastos (mesmo raciocínio, aplicado ao contexto de cada um):

- **Saudável**: meta de economia preservada; saldo projetado não negativo; orçamento abaixo de
  80% consumido; a fatura projetada é integralmente pagável com a disponibilidade segura do mês.
- **Atenção**: orçamento entre 80% e 100% consumido; ou a meta de economia está reduzida (ainda
  positiva, mas abaixo do configurado); ou o comprometimento saudável do cartão (ver
  `CALCULOS-E-INDICADORES.md`) está elevado.
- **Alto risco**: orçamento ultrapassado (mais de 100%); ou a meta de economia não será atingida
  no ritmo atual; ou o saldo projetado está muito próximo de zero; ou fechar o mês depende de um
  recebimento de terceiro ainda não confirmado.
- **Crítico**: saldo projetado negativo; ou há risco de pagamento parcial/mínimo da fatura (a
  disponibilidade segura do mês do vencimento já é insuficiente, ver `CALCULOS-E-INDICADORES.md`);
  ou despesas essenciais (ex.: moradia) ficam sem cobertura projetada.

Os percentuais (80%, 100%) e os limiares qualitativos ("muito próximo de zero", "reduzida")
**serão configuráveis no futuro**, mas entram no MVP como valores fixos definidos aqui — não
fixados diretamente no código sem possibilidade de ajuste (mesmo princípio já aplicado à meta de
economia em `MVP.md`: a modelagem técnica deve tratá-los como configuração, não como constante
imutável, ainda que o MVP não exponha uma tela para editá-los).

## 8. Conciliação e categorização — comportamento

Estados e entidades já definidos em `ESTADOS-E-TRANSICOES.md` e `MODELO-DE-DADOS.md`. Regras de
comportamento adicionais:

- **Mesmo arquivo não processado silenciosamente duas vezes**: `ArquivoImportado.hash_arquivo` é
  verificado antes de processar um novo arquivo; se o hash já existe, o sistema informa que o
  arquivo já foi importado e pede confirmação explícita antes de reprocessar (nunca reprocessa
  silenciosamente).
- **Uma transação importada não pode ter duas conciliações ativas**: um `VinculoConciliacao` só
  pode existir enquanto não houver outro vínculo ativo para a mesma `TransacaoImportada` ou para
  o mesmo `LancamentoFinanceiro`/`ParcelaCompra` de destino.
- **Desconciliação mantém histórico**: desconciliar não apaga o `VinculoConciliacao` anterior —
  marca-o como encerrado e permite um novo vínculo, preservando o registro de que já houve uma
  tentativa de conciliação ali.
- **Categorização — resolução de conflito**: confirmada a ordem de prioridade já definida em
  `PROCESSOS.md` (estabelecimento > descrição exata > texto parcial); em caso de empate de
  especificidade, a regra mais recente prevalece **apenas como último desempate**; se ainda
  assim houver ambiguidade real (duas regras igualmente específicas e criadas ao mesmo tempo,
  hipótese rara), o conflito exige escolha manual do usuário em vez de uma resolução automática
  arbitrária.

## 9. Anexos

Metadados definidos em `MODELO-DE-DADOS.md`. Estratégia:

- **Privacidade**: um anexo só é acessível a usuários com vínculo ativo na mesma empresa —
  nenhuma URL pública, nenhum acesso sem autenticação (mesmo princípio de qualquer dado
  empresarial, `docs/MODELO_MULTIEMPRESA.md`).
- **Autorização**: a autorização de leitura/escrita de um anexo segue a mesma autorização da
  entidade a que ele está vinculado (ex.: só quem pode ver uma compra pode ver o comprovante
  dela) — nenhuma regra de autorização própria e paralela para anexos.
- **Lixeira**: um anexo acompanha o ciclo de vida do registro ao qual está vinculado — vai para a
  lixeira junto, é restaurado junto, é excluído definitivamente junto (`PROCESSOS.md`, seção 18).
- **Backup**: fora do escopo desta tarefa definir o mecanismo técnico; a exigência funcional é
  que qualquer estratégia futura de backup proteja também os anexos, não apenas os dados
  relacionais (já registrado como escopo de MVP × evolução futura em `MVP.md`).
- **Prevenção de acesso cruzado**: `Anexo.empresa_id` é sempre verificado junto com a entidade
  relacionada antes de qualquer leitura — mesmo padrão de isolamento multiempresa da seção 3.

## 10. Auditoria

Segue integralmente o padrão já definido em `docs/BANCO_DE_DADOS.md` ("Auditoria") — nenhuma
estratégia nova é criada para o Financeiro LeS. A auditoria técnica (quem alterou o quê, quando,
de que valor para que valor) é diferente do histórico funcional visível ao usuário (ex.: a lista
de alterações de uma compra ou de uma parcela, mostrada na própria tela do registro) — a segunda
pode ser uma **projeção de leitura** sobre a primeira, específica para cada entidade, sem exigir
uma estrutura de dados duplicada. Dados sensíveis (senha, token) nunca são registrados em
auditoria — mesma regra já vigente na plataforma.

## 11. Lixeira — decisão sobre retenção

**Confirmada a proposta da tarefa**: no MVP, itens excluídos permanecem na lixeira até uma
exclusão definitiva **manual** — não haverá limpeza automática por prazo nesta fase. Resolve a
pendência "prazo de retenção da lixeira" registrada em `PENDENCIAS.md` (LES-F1-002). Comportamento
confirmado: itens na lixeira não participam de nenhum cálculo ativo (`INVARIANTES.md`);
restaurar um item revalida seus relacionamentos antes de reativá-lo (ex.: restaurar uma parcela
cuja fatura foi excluída definitivamente enquanto ela estava na lixeira precisa de tratamento
explícito, não de uma restauração silenciosa que aponte para um registro inexistente); anexos
seguem o mesmo ciclo do item ao qual pertencem.

## 12. Recebimento/pagamento superior ao saldo — decisão

**Confirmada a proposta da tarefa**, aplicada tanto a `Recebivel` (recebimento de terceiro/
empréstimo) quanto a `PagamentoFatura`: o sistema **não** aceita automaticamente um valor superior
ao saldo devido como simples quitação com sobra silenciosa. Ao identificar que o valor informado
excede o saldo, o sistema exige que a família classifique o excedente como um dos seguintes,
antes de confirmar a operação: juros, multa, adiantamento (para a próxima competência) ou valor
não identificado (mantido separado até esclarecimento). Nunca é tratado automaticamente como
receita comum. Resolve a pendência "recebimento maior que o saldo a receber" registrada em
`PENDENCIAS.md` (LES-F1-002).

## 13. Antecipação de parcelas — decisão

**Confirmada a proposta da tarefa como limite do MVP**: o sistema permite informar o valor
efetivamente pago ao antecipar uma ou mais parcelas selecionadas, registrar um desconto quando
houver (sem calculá-lo automaticamente a partir de uma fórmula de juros complexa), e quitar as
parcelas selecionadas com esse valor — o sistema **não** recalcula automaticamente contratos de
parcelamento complexos (ex.: juros compostos, tabelas de amortização). Se a compra parcelada não
tiver juros próprios (o caso mais comum de cartão de crédito no Brasil, onde o parcelamento sem
juros é a norma), a antecipação é uma simples quitação antecipada, sem nenhum cálculo adicional.
Isso resolve, para o escopo do MVP, a pendência "antecipação de parcela com desconto de juros"
registrada em `PENDENCIAS.md` (LES-F1-002) — casos de parcelamento com juros complexos ficam para
avaliação futura, se e quando a família de fato usar esse tipo de parcelamento.

## 14. Meta de economia — decisão

**Confirmada a proposta da tarefa**: no MVP, a meta de economia é tratada como **meta projetada**
(um cálculo de acompanhamento, ver `CALCULOS-E-INDICADORES.md`) — atingir a meta não exige
nenhuma transferência automática ou obrigatória para uma conta específica de reserva. Uma
transferência real para uma conta de reserva, quando a família decidir fazê-la, é registrada
normalmente como o processo de transferência entre contas próprias já descrito em
`PROCESSOS.md`, e pode ser **exibida separadamente** no dashboard como um indicador de apoio
("valor efetivamente reservado"), sem que isso seja uma exigência estrutural do modelo de meta.
Resolve a pendência "economia como meta projetada ou transferência real" registrada em
`PENDENCIAS.md` (LES-F1-002).

## 15. Estados suportados pelo modelo

Confirmação de que o modelo de dados suporta integralmente as tabelas de `ESTADOS-E-TRANSICOES.md`:
cada entidade com estado (`LancamentoFinanceiro`, `ContaAPagar`/`OcorrenciaContaAPagar`, `Fatura`,
`CompraCredito`, `ParcelaCompra`, `EmprestimoConcedido`, `CompromissoAPagar`,
`VinculoConciliacao`, `MetaEconomia`, e qualquer entidade excluível via lixeira) possui um campo
`status` próprio, e as transições descritas naquele documento operam exclusivamente sobre esse
campo — nenhum estado foi proposto em `ESTADOS-E-TRANSICOES.md` sem uma entidade correspondente
neste modelo, e nenhuma entidade deste modelo introduz um estado que não esteja documentado lá.
Cancelamento, restauração, renegociação, pagamento parcial, conciliação e desconciliação são
todos transições de `status` (mais, quando aplicável, criação de um registro de histórico/
auditoria), nunca exclusão física.

## 16. Ordem futura de implementação

Ordem recomendada, com justificativa de dependência (nenhuma implementação ocorre nesta tarefa):

1. **Núcleo compartilhado** (`Pessoa`, `ParteFinanceira`, `Anexo`, `Recorrencia`,
   `InstituicaoFinanceira`, `Bandeira`) — nada mais depende funcionalmente disso, mas tudo o
   referencia.
2. **Dados iniciais do Financeiro LeS** (cadastro da empresa, dos dois usuários e das duas
   pessoas correspondentes) — precisa do núcleo (`Pessoa`) pronto.
3. **Contas e categorias** (`ContaFinanceira`, `CategoriaFinanceira`, já existentes, com os campos
   conceituais novos desta tarefa) — depende de `Pessoa` (titularidade) e `InstituicaoFinanceira`.
4. **Receitas e despesas** (`LancamentoFinanceiro`, incluindo transferência entre contas
   próprias) — depende de contas e categorias.
5. **Contas a pagar** (`ContaAPagar`/`OcorrenciaContaAPagar`) — depende de categorias,
   `ParteFinanceira` (favorecido) e `Recorrencia`.
6. **Cartões** (`CartaoCredito`) — depende de `ContaFinanceira` (conta de pagamento),
   `Pessoa` (titular), `InstituicaoFinanceira`, `Bandeira`.
7. **Compras e parcelas** (`CompraCredito`, `ParcelaCompra`) — depende de cartões e categorias.
8. **Faturas e pagamentos** (`Fatura`, `PagamentoFatura`) — depende de compras/parcelas
   existentes para ter o que faturar.
9. **Orçamento e meta** (`Orcamento`, `MetaEconomia`) — depende de categorias e cartões
   (limite saudável) já existirem, e se beneficia de haver histórico de lançamentos/compras para
   ter dados reais a comparar.
10. **Dashboard** — depende de tudo acima já existir, é a primeira funcionalidade que **lê** de
    todos os módulos anteriores sem introduzir dado novo.
11. **Recebíveis e compromissos** (`CompromissoAPagar`, `EmprestimoConcedido`, `Recebivel`,
    compra para terceiro) — funcionalmente independentes do fluxo principal de cartão, podem vir
    depois do dashboard sem bloquear a validação do núcleo do produto.
12. **Simulador** — depende de orçamento, meta, faturas, parcelas, compromissos e recebíveis já
    existirem, pois consome os mesmos dados e fórmulas de todos eles (`ARQUITETURA-FUNCIONAL.md`,
    seção 5).
13. **Conciliação** (`ArquivoImportado`, `TransacaoImportada`, `VinculoConciliacao`,
    `RegraCategorizacao`) — depende de haver volume real de lançamentos/compras/faturas para ter
    o que conciliar; é o módulo que mais se beneficia de vir depois do uso real do núcleo
    (mesmo raciocínio já registrado em `MVP.md`, "fase seguinte").
14. **Automações futuras** (Google Calendar, mensagens de cobrança automáticas, importação em
    PDF/OCR) — evoluções já registradas como fora do MVP em `MVP.md`, sem dependência adicional
    além do que já está pronto até aqui.

## Documentos relacionados

- `MODELO-DE-DADOS.md` — entidades detalhadas por esta arquitetura.
- `INVARIANTES.md` — regras estruturais que a arquitetura deve garantir.
- `MATRIZ-ENTIDADES-PROCESSOS.md` — participação de cada entidade em cada processo.
- `PENDENCIAS.md` — pendências resolvidas nesta tarefa e as que permanecem.
- `docs/DECISOES.md` — registro da decisão arquitetural macro (núcleo + extensão), por ser
  reutilizável pela plataforma além do Financeiro LeS.
