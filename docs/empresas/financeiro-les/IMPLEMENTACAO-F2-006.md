# Implementação LES-F2-006 — Recorrências e compromissos financeiros básicos

## Nota sobre a numeração da tarefa

`docs/empresas/financeiro-les/BACKLOG-INICIAL.md` associa o identificador `LES-F2-006` a
"Transferências entre contas próprias", não a recorrências. O enunciado recebido para esta tarefa,
porém, é explícito e detalhado sobre recorrências financeiras básicas, tema já reservado no roadmap
(`docs/DECISOES.md`, seção "Financeiro LeS — núcleo compartilhado com extensões específicas") e
antes documentado apenas como pendência em `PENDENCIAS.md`. A implementação seguiu o enunciado
recebido; a divergência de numeração fica registrada aqui para quem revisar o histórico do backlog,
sem bloquear a entrega.

## Diagnóstico e reaproveitamento

O commit de referência (`a282a36`, LES-F2-005) já entrega `LancamentoFinanceiro` completo: receita
e despesa, competência, vencimento, liquidação, status, pessoa, parte, conta, categoria, saldo
derivado, filtros e resumo. O enum `OrigemLancamentoFinanceiro` já reservava o valor `RECORRENCIA`
desde a V9, mas nada o utilizava. Não havia entidade, tabela, service, repository, controller,
página, JavaScript, `@Scheduled` ou qualquer conceito de recorrência, agendamento, parcela, conta
fixa ou lançamento futuro em nenhum lugar do código — apenas uma proposta de modelo de dados em
`MODELO-DE-DADOS.md` (seção "Recorrência (modelo reutilizável)") e uma pendência aberta sobre o
critério de dia inválido em meses menores.

Esta tarefa reaproveita integralmente `LancamentoFinanceiro`: a recorrência gera o mesmo tipo de
lançamento já existente (com origem `RECORRENCIA` e referência à regra de origem), em vez de criar
uma segunda hierarquia de lançamentos. Nenhuma migration, entidade ou API anterior foi alterada de
forma incompatível.

## Entidade `RecorrenciaFinanceira`

Nova entidade em `br.app.criati.financeiro.model.RecorrenciaFinanceira`, seguindo o mesmo padrão
de `LancamentoFinanceiro`/`ContaFinanceira` (sem base class comum — nenhuma entidade financeira
existente usa uma; introduzir uma agora seria uma mudança de convenção não pedida por esta tarefa).
Campos: empresa, tipo, descrição, valor padrão, conta, categoria, pessoa financeira, parte
financeira (opcional), forma de pagamento (opcional), periodicidade, intervalo, dia de referência,
mês de referência (somente anual), data inicial, data final (opcional), próxima competência,
status, gerar automaticamente, observação, auditoria de criação/atualização e auditoria específica
de pausa/encerramento (data e usuário). `empresaId` nunca é aceito do cliente — vem sempre do
`ContextoEmpresaAtual` da sessão, como em todo o módulo.

## Recorrência, ocorrência e lançamento

Três conceitos deliberadamente distintos:

- **Recorrência** — a regra (`RecorrenciaFinanceira`). Nunca movimenta saldo.
- **Ocorrência** — uma competência específica gerada pela regra (identificada por
  `recorrencia_id + data_competencia`, não uma tabela própria).
- **Lançamento** — o `LancamentoFinanceiro` gerado para aquela competência
  (`LancamentoFinanceiro.gerarDeRecorrencia(...)`), com `origem = RECORRENCIA` e referência
  (`recorrencia_id`) à regra. O impacto no saldo só ocorre quando esse lançamento é liquidado,
  exatamente como qualquer lançamento manual.

## Tipos financeiros e categoria

`RecorrenciaFinanceira.tipo` reutiliza `TipoFinanceiro` (`RECEITA`/`DESPESA`). A categoria deve
pertencer à mesma empresa, estar ativa para novas recorrências e ter o mesmo tipo da recorrência —
mesma regra já aplicada a `LancamentoFinanceiro`. Transferências recorrentes não foram
implementadas.

## Periodicidades

Enum `PeriodicidadeRecorrencia` implementado com **`MENSAL`** e **`ANUAL`** apenas. `SEMANAL` e
`PERSONALIZADA` foram avaliadas e adiadas deliberadamente: o mecanismo de prevenção de duplicidade
desta entrega (`recorrencia_id + data_competencia`, uma ocorrência por mês-calendário) pressupõe no
máximo uma ocorrência por competência mensal — uma recorrência semanal pode gerar várias ocorrências
no mesmo mês, o que exigiria um redesenho do conceito de competência da ocorrência (ex.: data exata
em vez de mês) para permanecer seguro contra duplicidade. Implementar isso com segurança ficaria
fora do escopo desta tarefa; a decisão está registrada em `docs/DECISOES.md`. `MENSAL`/`ANUAL`
cobrem os exemplos do enunciado (salário, condomínio, energia, internet, dízimo, assinatura anual).

- **Mensal**: `intervalo` (a cada N meses, padrão 1) + `diaReferencia` (1–31).
- **Anual**: os mesmos campos + `mesReferencia` (1–12), obrigatório só nesta periodicidade.

## Tratamento de meses menores

`RecorrenciaFinanceira.calcularVencimento(YearMonth competencia)` usa
`Math.min(diaReferencia, competencia.lengthOfMonth())`: dia 31 em abril usa 30, dia 31 ou 30 em
fevereiro usa 28 (ou 29 em ano bissexto — `YearMonth.lengthOfMonth()` já trata bissextos
corretamente). A mesma regra vale para recorrência anual em 29 de fevereiro: em ano não bissexto
cai automaticamente em 28/02. Essa é a resolução formal da pendência registrada em
`PENDENCIAS.md` ("Critério de 'meses sem o dia configurado'"), usando a proposta conservadora já
sugerida ali. Testada em `RecorrenciaFinanceiraTests` (mês de 30 dias, fevereiro comum, fevereiro
bissexto, anual em 29/02).

## Competência da ocorrência

A competência da ocorrência é a própria `YearMonth` (armazenada como `LocalDate` no dia 1 em
`RecorrenciaFinanceira.proximaCompetencia`, e como `dataCompetencia` no `LancamentoFinanceiro`
gerado — sempre normalizada para o dia 1, diferente de lançamentos manuais que podem usar qualquer
dia do mês como já ocorria antes desta tarefa). A competência não depende da liquidação: é definida
no momento da geração e não muda depois. `RecorrenciaFinanceira.competenciaInicial()` calcula a
primeira competência elegível a partir de `dataInicial` (para anual, rola para o próximo ano se o
mês de referência já passou dentro do ano de `dataInicial`).

## Status

Enum `StatusRecorrencia`: `ATIVA`, `PAUSADA`, `ENCERRADA`. Ativa gera novas ocorrências; pausada e
encerrada não geram (`RecorrenciaFinanceiraStatusInvalidoException`, HTTP 409). Pausar e encerrar
preservam lançamentos já gerados e todo o histórico; nenhuma exclusão física ocorre. Reativação
(`retomar`) recalcula a próxima competência para o mês atual quando a recorrência ficou atrasada,
sem gerar retroativamente as competências perdidas — isso só acontece por ação explícita
(`gerar-competencia`). Encerrar não permite reativação nesta entrega (sem "reabrir encerrada",
consistente com o enunciado, que não pede essa transição). Status, pausa e encerramento registram
usuário e instante (`pausadaEm`/`pausadaPor`, `encerradaEm`/`encerradaPor`).

## Geração automática e manual

`gerarAutomaticamente` (booleano) marca a recorrência para geração em lote. A geração automática
foi implementada como **endpoint administrativo explícito**
(`POST /api/contexto/financeiro/recorrencias/gerar-automaticas`, sem `@Scheduled`) — uma das
estratégias explicitamente aceitas pelo enunciado, escolhida por não introduzir execução oculta
difícil de testar nem dependência nova. A decisão está registrada em `docs/DECISOES.md`. A geração
manual usa o mesmo mecanismo por trás de dois endpoints: gerar a próxima competência
(`POST /{id}/gerar`) e gerar uma competência específica (`POST /{id}/gerar-competencia`).

## Janela de geração

`POST /{id}/gerar` só gera a competência que já é a "próxima" da recorrência (nunca uma competência
futura em relação ao mês atual — `DadosInvalidosException`, HTTP 400, se a próxima competência
ainda não chegou) e avança o cursor em um único passo por chamada. `gerar-automaticas` processa, em
um único lote, no máximo uma competência por recorrência elegível por execução — não itera
retroativamente várias competências em uma chamada. Isso segue a recomendação do enunciado de não
gerar meses futuros antecipadamente nem gerar retroativamente em massa sem confirmação explícita.

## Prevenção de duplicidade

Regra central: `recorrencia_id + data_competencia` é único. Aplicada em três camadas:

1. **Service** — `RecorrenciaFinanceiraService` sempre verifica
   `existsByRecorrenciaIdAndDataCompetencia`/`findByRecorrenciaIdAndDataCompetencia` antes de
   gerar; se já existe, devolve o lançamento existente (idempotente) em vez de duplicar ou lançar
   erro.
2. **Entidade JPA** — `@Table(uniqueConstraints = @UniqueConstraint(...))` em
   `LancamentoFinanceiro`, validado nos testes de integração (roda contra o schema gerado pelo
   Hibernate em H2).
3. **Migration** — `ALTER TABLE lancamento_financeiro ... ADD CONSTRAINT
   uq_lancamento_financeiro_recorrencia_competencia UNIQUE (recorrencia_id, data_competencia)` em
   `V10`. `NULL` é tratado como distinto pelo SQL padrão (Postgres e H2), então lançamentos
   manuais (`recorrencia_id` nulo) nunca colidem entre si por essa constraint.

Reexecuções são idempotentes: chamar a geração da mesma competência duas vezes nunca duplica
(testado em `RecorrenciaFinanceiraControllerTests`, tanto via `gerar-competencia` quanto via
`gerar-automaticas` executado duas vezes seguidas).

## Referência no lançamento

`LancamentoFinanceiro` ganhou `recorrencia` (`@ManyToOne` opcional, coluna `recorrencia_id`) e o
método estático `gerarDeRecorrencia(RecorrenciaFinanceira, dataCompetencia, dataVencimento, autor)`,
que copia conta, categoria, pessoa, parte, tipo, descrição, valor padrão e forma de pagamento da
regra, define `origem = RECORRENCIA` e mantém a referência à regra de origem. Lançamentos manuais
continuam com `origem = MANUAL` e `recorrencia = null`, sem qualquer alteração de comportamento.
Editar um lançamento gerado não altera a regra; encerrar/pausar/editar a regra não altera
lançamentos já gerados; nada nesta tarefa converte lançamentos antigos em recorrentes.

## Valor padrão e valor da ocorrência

`valorPadrao` é copiado para o lançamento no momento da geração (`BigDecimal`, escala 2,
`HALF_UP`, normalizado por `MoedaUtils.normalizar`, mesma classe já usada por lançamentos). Depois
de gerado, o lançamento pode ser editado individualmente pelas rotas já existentes de
`LancamentoFinanceiroController` — essa edição não altera a série, e editar a série depois não
altera lançamentos já gerados (testado explicitamente). Para despesas variáveis (energia, água), o
formulário documenta que o valor padrão é uma estimativa ajustável em cada ocorrência.

## Conta

Decisão pragmática: **conta é obrigatória na recorrência**, assim como já é obrigatória em
`LancamentoFinanceiro` (`conta_id NOT NULL` desde a V5). O enunciado sugeria permitir recorrência
sem conta até a liquidação; isso exigiria tornar `LancamentoFinanceiro.conta` opcional, uma mudança
estrutural no lançamento já entregue na LES-F2-005 (afetaria saldo derivado e todo o cálculo de
impacto por conta) fora do escopo desta tarefa. Mantendo conta obrigatória, cada ocorrência gerada
já nasce com destino financeiro definido, sem exigir edição obrigatória antes da liquidação.

## Pessoa e parte financeira

Pessoa financeira é **obrigatória** na recorrência (o enunciado pede "preferencialmente
obrigatória"; a interpretação adotada foi obrigatória, coerente com o propósito da recorrência —
identificar a quem o compromisso pertence). Parte financeira permanece opcional. Ambas devem
pertencer à mesma empresa e estar ativas para novas recorrências; referências desativadas depois
permanecem no histórico, mesma regra já usada por lançamentos.

## Alteração da série e da ocorrência

`RecorrenciaFinanceiraService.editar` (`atualizarSerie` na entidade) altera valor padrão, conta,
categoria, pessoa, parte, forma, intervalo, dia, mês (se anual), data final, geração automática e
observação — nunca a periodicidade nem a data inicial, que definem a competência inicial e não têm
edição pedida pelo enunciado. A edição não toca `proximaCompetencia` nem lançamentos já gerados. A
interface exibe o aviso: "As alterações serão aplicadas somente às próximas ocorrências.
Lançamentos já gerados não serão modificados." Editar a ocorrência (o lançamento gerado) usa as
rotas já existentes de lançamentos e não afeta a série, testado nos dois sentidos.

## Pausa e retomada

`pausar`/`retomar` (`POST /{id}/pausar`, `POST /{id}/retomar`) exigem, respectivamente, status
`ATIVA` e `PAUSADA` (`RecorrenciaFinanceiraStatusInvalidoException`, 409, caso contrário). Pausar
não cancela nem altera lançamentos pendentes. Retomar recalcula `proximaCompetencia` para o mês
atual somente se a recorrência ficou para trás (nunca retrocede se já estava adiantada); não gera
nenhuma competência automaticamente — a geração das competências perdidas exige ação explícita
(`gerar-competencia`, uma por vez).

## Encerramento

`encerrar` (`POST /{id}/encerrar`) exige que o status não seja já `ENCERRADA`. Impede novas
ocorrências (`gerar`/`gerar-competencia`/`gerar-automaticas` retornam 409 depois de encerrada, com
a única exceção de que os já gerados continuam consultáveis), preserva histórico e lançamentos
pendentes/liquidados e registra usuário e data. Nada é excluído fisicamente.

## Geração retroativa

`POST /{id}/gerar-competencia` (`{ "competencia": "AAAA-MM" }`) permite gerar qualquer competência
dentro do período da recorrência, desde que não seja futura em relação ao mês atual — uma
competência por chamada, idempotente (chamar duas vezes para a mesma competência não duplica),
exigindo perfil de escrita (GESTOR/ADMINISTRADOR) e auditando o autor no lançamento gerado.

## Multiempresa

Toda consulta do `RecorrenciaFinanceiraRepository` recebe `empresaId` explicitamente (mesmo padrão
de todo o módulo — nenhum filtro implícito de tenant). `RecorrenciaFinanceiraService` sempre
resolve conta, categoria, pessoa e parte por `id + empresaId`; um identificador de outro tenant
resolve para "não encontrado" (404), nunca "acesso negado", para não revelar a existência do
recurso em outra empresa. Testado com duas empresas, incluindo tentativa cruzada de acesso direto
por ID (`GET /{id}` de uma recorrência de outra empresa).

## Migration

`V10__criar_recorrencias_financeiras.sql` cria `recorrencia_financeira` (FKs para empresa, conta,
categoria, pessoa, parte e usuários de auditoria; `CHECK`s de tipo, periodicidade, status, valor
positivo, intervalo positivo, dia 1–31, mês 1–12 só quando anual, e data final ≥ inicial) e altera
`lancamento_financeiro` adicionando `recorrencia_id` (FK opcional) e a constraint de unicidade
`recorrencia_id + data_competencia`. Nenhuma migration anterior foi editada; nenhum dado real foi
inserido.

## Constraints e índices

Índices por empresa, empresa+status, empresa+próxima competência, empresa+pessoa,
empresa+categoria e empresa+periodicidade em `recorrencia_financeira`; índice
empresa+recorrência em `lancamento_financeiro`. As mesmas regras (categoria compatível, conta/
pessoa/parte do mesmo tenant e ativas, valor positivo, datas coerentes) são validadas também no
service, já que H2 (usado nos testes) não executa as migrations Flyway (`spring.flyway.enabled=false`
no perfil de teste, schema gerado pelas anotações JPA) — ver seção "Validação em PostgreSQL".

## Repositories

`RecorrenciaFinanceiraRepository`: listagem e busca por empresa, filtros por status, tipo,
periodicidade, pessoa, categoria, busca textual por descrição, e localização de recorrências
prontas para geração automática (`findAllByEmpresaIdAndStatusAndGerarAutomaticamenteTrueAndProximaCompetenciaLessThanEqual`).
`LancamentoFinanceiroRepository` ganhou `existsByRecorrenciaIdAndDataCompetencia`,
`findByRecorrenciaIdAndDataCompetencia`, `findAllByEmpresaIdAndRecorrenciaIdOrderByDataCompetenciaDesc`
e `countByRecorrenciaId`. Nenhum método ignora o tenant.

## Services

`RecorrenciaFinanceiraService` centraliza toda a regra: CRUD, pausar/retomar/encerrar, cálculo de
competência/vencimento (delegado à entidade), validação de conta/categoria/pessoa/parte,
prevenção de duplicidade e geração transacional (`@Transactional`) do lançamento. O controller não
implementa nenhum cálculo — apenas mapeia request/response, igual ao padrão de
`LancamentoFinanceiroController`.

## DTOs

`RecorrenciaFinanceiraRequest`/`RecorrenciaFinanceiraEdicaoRequest` (records com Bean Validation),
`GerarCompetenciaRequest`, `RecorrenciaFinanceiraResponse` (inclui quantidade de ocorrências e
última competência gerada, calculadas sob demanda) e `ResumoRecorrenciasResponse`. Nenhum DTO
aceita `empresaId` editável.

## Validações

Tipo, descrição (obrigatória, ≤200), valor padrão (> 0, escala 2), periodicidade, intervalo (≥1),
dia (1–31), mês (obrigatório só se anual), data inicial (obrigatória), data final (≥ inicial),
categoria compatível com o tipo, conta/pessoa/parte do mesmo tenant e ativas para novas
referências. Todas replicadas em `RecorrenciaFinanceiraTests` (nível de entidade) e
`RecorrenciaFinanceiraControllerTests` (nível HTTP/400).

## Endpoints

`/api/contexto/financeiro/recorrencias`: `GET` (listar com filtros), `GET /{id}`, `POST` (criar),
`PUT /{id}` (editar série), `POST /{id}/pausar`, `POST /{id}/retomar`, `POST /{id}/encerrar`,
`POST /{id}/gerar` (próxima ocorrência), `POST /{id}/gerar-competencia` (retroativa/específica),
`POST /gerar-automaticas` (lote), `GET /{id}/ocorrencias` (lançamentos gerados) e `GET /resumo`.
Todos exigem sessão autenticada, empresa ativa com Financeiro habilitado (`ContextoFinanceiroService`,
mesmo portão de todo o módulo), perfil GESTOR/ADMINISTRADOR para escrita e CSRF nas mutações.

## Página

`/app/financeiro/recorrencias` (`PaginaController`, protegida pelo mesmo padrão de
`/app/financeiro/lancamentos`) e template `app/financeiro-recorrencias.html`: filtros (status,
tipo, periodicidade, pessoa, categoria, busca), cards de resumo, tabela de recorrências, formulário
de cadastro/edição com o aviso de que a edição só afeta próximas ocorrências, e modal de
ocorrências geradas — que não duplica o formulário de lançamento, apenas lista e direciona para
`/app/financeiro/lancamentos`. `financeiro-recorrencias.js` segue o mesmo padrão IIFE de
`financeiro-lancamentos.js`; `financeiro-api.js` ganhou o objeto `recorrencias`. Um atalho para a
nova página foi adicionado ao dashboard do Financeiro (`app/financeiro.html`).

## Filtros e ocorrências

Filtros: status, tipo, periodicidade, pessoa, categoria, busca textual. A tela de ocorrências lista
competência, vencimento, valor, status e liquidação de cada lançamento gerado, com link para a
tela de lançamentos (não uma tela de detalhe própria, que não existe no restante do app).

## Resumo

`GET /resumo` retorna recorrências ativas/pausadas/encerradas, receitas e despesas recorrentes
previstas (soma de `valorPadrao` das ativas, por tipo) e ocorrências geradas na competência atual.
A interface deixa explícito que valores previstos são estimativa, não saldo realizado — só a
liquidação afeta o saldo (`SaldoFinanceiroService`, inalterado por esta tarefa).

## Estados de interface

Carregando, vazio ("Nenhuma recorrência cadastrada. Cadastre receitas e despesas que se repetem
para reduzir lançamentos manuais."), erro, sucesso (toast), badges de status, confirmação via
`confirm()` para pausar/encerrar (ações com efeito visível), e mensagens de erro do backend
propagadas pelos toasts existentes (`CriatiUI.showToast`) para os casos de status inválido, dados
inválidos, referência inativa e fora do período.

## Segurança

Mesmo modelo do restante do Financeiro: autenticação, empresa ativa, `ContextoFinanceiroService`,
perfil GESTOR/ADMINISTRADOR para toda escrita (criar, editar, pausar, retomar, encerrar, gerar),
CSRF em toda mutação, e identificadores de outro tenant sempre resolvidos como 404. Testado em
`RecorrenciaFinanceiraControllerTests` (usuário comum bloqueado, anônimo 401, acesso cruzado entre
empresas) e `PaginaFinanceiroSegurancaTests` (rota protegida, redirecionamento sem Financeiro
habilitado, conteúdo da página com Financeiro habilitado).

## Auditoria

Manual, no mesmo padrão já usado por todo o módulo (sem framework de auditoria da JPA/Spring Data
em nenhum lugar do projeto): `criadoEm`/`criadoPor`, `atualizadoEm`/`atualizadoPor` em toda
alteração, mais `pausadaEm`/`pausadaPor` e `encerradaEm`/`encerradaPor` específicos para essas
transições, exigidos pelo enunciado. Não há histórico versionado campo a campo — a mesma limitação
já registrada e aceita na LES-F2-005.

## Compatibilidade

Lançamentos existentes, saldo derivado, dashboard, rotas, APIs e origem `MANUAL` permanecem
inalterados. Nenhum lançamento antigo foi convertido em recorrente. `V10` apenas adiciona uma
coluna opcional e uma constraint nova a `lancamento_financeiro`, sem alterar dados existentes.

## Limitações

- Apenas `MENSAL`/`ANUAL` (ver "Periodicidades").
- Conta e pessoa financeira obrigatórias na recorrência (ver seções específicas).
- Geração automática exige acionamento explícito do endpoint (sem `@Scheduled`).
- Sem cartões, parcelas, faturas, orçamento, conciliação, notificações externas ou liquidação
  parcial — todos fora do escopo desta tarefa.
- Sem histórico versionado campo a campo (mesma limitação da LES-F2-005).

## Impacto na próxima tarefa

O módulo de contas a pagar/compromissos completos (mencionado em `BACKLOG-INICIAL.md` como
LES-F5-002/LES-F2-008) pode reaproveitar `RecorrenciaFinanceira` como gerador de base, e a origem
`RECORRENCIA` já identifica lançamentos originados de regra. Se semanal/personalizada forem
necessárias no futuro, exigirão revisitar o conceito de competência da ocorrência (hoje um
mês-calendário) para preservar a mesma garantia de não duplicidade.
