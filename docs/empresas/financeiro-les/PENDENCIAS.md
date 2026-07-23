# Pendências — Financeiro LeS

Decisões e dados que **dependem de confirmação futura** antes ou durante a modelagem de dados e
das regras de negócio. Nenhum valor foi inventado neste documento nem em `VISAO-FUNCIONAL.md`,
`MVP.md` ou `GLOSSARIO.md` — onde o contexto da tarefa não trouxe um dado concreto, ele está
registrado aqui como pendente, não preenchido com um exemplo fictício apresentado como real.

## Dados dos cartões de crédito

- Limite bancário de cada um dos 4 cartões iniciais — não informado.
- Limite saudável desejado para cada cartão — não informado (a família definirá um valor por
  cartão; o exemplo "R$ 12.000 / R$ 2.500" usado no contexto da tarefa é ilustrativo, não um
  dado real de nenhum cartão específico).
- Dia de fechamento e dia de vencimento de cada cartão — não informado.
- Banco emissor, titular e bandeira de cada um dos 4 cartões — não informado.
- Quais dos 4 cartões (se algum) são virtuais vinculados a um cartão físico principal — não
  informado.
- Se e quando o número de cartões será reduzido (mencionado como possibilidade futura, sem
  cartão ou prazo definido).

**Nota da LES-F3-001**: a estrutura cadastral de cartões (`CartaoCredito`, físico/virtual, limite total,
limite saudável, fechamento, vencimento, bandeira, instituição, titular, bloqueio) foi implementada nesta
tarefa e já é capaz de armazenar todos os dados acima assim que a família os informar. Nenhum dos dados
concretos listados acima foi preenchido com valor fictício apresentado como real — os testes usam apenas
valores de exemplo claramente fictícios (`1234`, `5000.00` etc.). Esta pendência permanece aberta apenas
quanto aos **dados reais**, não quanto à arquitetura, que já está pronta para recebê-los.

## Dados das contas bancárias

- Saldo inicial de cada uma das 4 contas (Pessoa 1 — BB, Pessoa 1 — Inter, Pessoa 2 — BB,
  Pessoa 2 — Inter) — não informado.
- Se haverá conta em dinheiro em espécie cadastrada desde o início, mesmo não sendo prioridade.

## Dados dos usuários

- Nomes reais das duas pessoas (o contexto usa apenas "Pessoa 1" e "Pessoa 2") — não informado.
- E-mails que serão usados para o login de cada pessoa — não informado.

## Valores recorrentes

- Valor do PIX recorrente do pagamento do apartamento — não informado.
- Valor e periodicidade de cada conta a pagar recorrente conhecida (condomínio, energia, água,
  internet, plano de saúde etc.) — não informado.
- Valor e periodicidade de cada assinatura ativa hoje — não informado.
- Salário de cada pessoa, data de recebimento e conta de destino — não informado (mencionado
  apenas que cada pessoa recebe o salário na própria conta).

## Metas

- Se 7% da renda mensal é a única meta de economia inicial ou se já existem outras metas
  financeiras nomeadas (reduzir fatura, quitar dívida específica, formar reserva com valor-alvo)
  — não informado além do percentual de 7%.
- Prazo desejado para qualquer meta financeira além da meta de economia — não informado.

## Histórico financeiro disponível

- Se existe histórico de lançamentos, faturas ou extratos anteriores à criação do sistema que
  deverá ser importado/cadastrado retroativamente, e a partir de qual data — não informado.
- Empréstimos ou compromissos já em andamento hoje (fora do cartão) que precisarão ser
  cadastrados como saldo inicial — não informado além da previsão funcional de que o sistema deve
  suportar esse cadastro.

## Decisão de arquitetura — RESOLVIDA (LES-F1-003)

- **Reaproveitar o módulo `FINANCEIRO` genérico ou construir um módulo novo dedicado.** Recomendado
  na LES-F1-002 (Opção C) e **confirmado como decisão** na LES-F1-003: núcleo financeiro
  compartilhado (conta, categoria, lançamento simples, pessoa, favorecido, anexo, recorrência)
  com extensões específicas do Financeiro LeS (cartão, compra no crédito, fatura, parcela,
  orçamento, meta de economia, empréstimos, recebíveis, conciliação, simulador). Ver
  `ARQUITETURA-FUNCIONAL.md`, seção 1, e o registro correspondente em `docs/DECISOES.md`,
  seção "Financeiro LeS — núcleo compartilhado com extensões". Removida desta lista de pendências
  por já estar decidida (ainda não implementada — a implementação é escopo de uma etapa futura).

## Dúvidas de regra da LES-F1-002 — RESOLVIDAS na LES-F1-003

As seis dúvidas abaixo, registradas na LES-F1-002, foram decididas nesta tarefa
(`ARQUITETURA-FUNCIONAL.md`, seções 6, 7, 11, 12, 13 e 14) — mantidas aqui apenas como referência
histórica de que já têm decisão registrada, não mais como pendência aberta:

- **Antecipação de parcela com desconto de juros**: decidido — no MVP, permite informar o valor
  pago e um desconto manual, sem recálculo automático de contratos complexos
  (`ARQUITETURA-FUNCIONAL.md`, seção 13).
- **Prazo de retenção da lixeira**: decidido — sem prazo automático no MVP; exclusão definitiva é
  sempre manual (`ARQUITETURA-FUNCIONAL.md`, seção 11).
- **Recebimento/pagamento superior ao saldo**: decidido — exige classificação explícita do
  excedente (juros, multa, adiantamento ou valor não identificado), nunca aceito
  automaticamente (`ARQUITETURA-FUNCIONAL.md`, seção 12).
- **Janela da média histórica**: decidido — últimos três meses completos, nunca o mês corrente
  (`ARQUITETURA-FUNCIONAL.md`, seção 6).
- **Margem de segurança do risco de pagamento parcial da fatura** e **ritmo de "meta em risco"**:
  critérios objetivos propostos e confirmados em `ARQUITETURA-FUNCIONAL.md`, seção 7 — os
  percentuais exatos (80%/100%) ficam registrados como configuráveis no futuro, não fixos no
  código, mas **não são mais uma pendência de definição de critério**, apenas um detalhe de
  ajuste fino a validar com o uso real.
- **Economia como meta projetada ou transferência real**: decidido — meta projetada no MVP, sem
  transferência obrigatória (`ARQUITETURA-FUNCIONAL.md`, seção 14).

## Pendências que permanecem (dados concretos, ainda não informados)

Nenhuma pendência de dado concreto foi resolvida nesta tarefa — são decisões que só a família
pode fornecer, não decisões de arquitetura ou de regra. Mantidas integralmente:

### Dados dos cartões de crédito

- Limite bancário de cada um dos 4 cartões iniciais — não informado.
- Limite saudável desejado para cada cartão — não informado (a família definirá um valor por
  cartão; o exemplo "R$ 12.000 / R$ 2.500" usado no contexto da tarefa é ilustrativo, não um
  dado real de nenhum cartão específico).
- Dia de fechamento e dia de vencimento de cada cartão — não informado.
- Banco emissor, titular e bandeira de cada um dos 4 cartões — não informado.
- Quais dos 4 cartões (se algum) são virtuais vinculados a um cartão físico principal — não
  informado.
- Se e quando o número de cartões será reduzido (mencionado como possibilidade futura, sem
  cartão ou prazo definido).

### Dados das contas bancárias

- Saldo inicial de cada uma das 4 contas (Pessoa 1 — BB, Pessoa 1 — Inter, Pessoa 2 — BB,
  Pessoa 2 — Inter) — não informado.
- Se haverá conta em dinheiro em espécie cadastrada desde o início, mesmo não sendo prioridade.

### Dados dos usuários

- Nomes reais das duas pessoas (o contexto usa apenas "Pessoa 1" e "Pessoa 2") — não informado.
- E-mails que serão usados para o login de cada pessoa — não informado.

### Valores recorrentes

- Valor do PIX recorrente do pagamento do apartamento — não informado.
- Valor e periodicidade de cada conta a pagar recorrente conhecida (condomínio, energia, água,
  internet, plano de saúde etc.) — não informado.
- Valor e periodicidade de cada assinatura ativa hoje — não informado.
- Salário de cada pessoa, data de recebimento e conta de destino — não informado (mencionado
  apenas que cada pessoa recebe o salário na própria conta).

### Metas

- Se 7% da renda mensal é a única meta de economia inicial ou se já existem outras metas
  financeiras nomeadas (reduzir fatura, quitar dívida específica, formar reserva com valor-alvo)
  — não informado além do percentual de 7%.
- Prazo desejado para qualquer meta financeira além da meta de economia — não informado.

### Histórico financeiro disponível

- Se existe histórico de lançamentos, faturas ou extratos anteriores à criação do sistema que
  deverá ser importado/cadastrado retroativamente, e a partir de qual data — não informado.
- Empréstimos ou compromissos já em andamento hoje (fora do cartão) que precisarão ser
  cadastrados como saldo inicial — não informado além da previsão funcional de que o sistema deve
  suportar esse cadastro.

## Novas pendências identificadas na modelagem (LES-F1-003)

- **Código da aplicação da extensão — RESOLVIDO na LES-F2-001:** `FINANCEIRO_RESIDENCIAL`, por
  representar uma extensão reutilizável para clientes residenciais sem acoplamento ao nome LeS.
  O código foi apenas reservado; sua inclusão no enum, catálogo e migration pertence à tarefa
  funcional que habilitar a extensão e não foi antecipada nesta preparação.
- **Conciliação entre `TipoContaFinanceira` (enum já existente) e os novos tipos de conta**
  propostos (`CONTA_PAGAMENTO`, `DINHEIRO`, `CARTEIRA`) — `MODELO-DE-DADOS.md` registra que é uma
  decisão de modelagem técnica futura, não resolvida aqui.
- **Critério de "meses sem o dia configurado"** em uma recorrência (ex.: dia 31 em mês de 30
  dias) — **RESOLVIDO na LES-F2-006:** implementado usando o último dia válido do mês (a mesma
  proposta conservadora já registrada aqui), aplicado uniformemente a recorrências mensais e
  anuais, inclusive 29 de fevereiro em anos não bissextos. Ver
  `docs/empresas/financeiro-les/IMPLEMENTACAO-F2-006.md` e `docs/DECISOES.md`. A confirmação da
  família permanece útil para validar a expectativa de uso real, mas não bloqueia mais a
  implementação.
- **Comprovantes de contas a pagar (ocorrência/pagamento) — CONFIRMADO como pendente na LES-F2-007:**
  a tarefa de contas a pagar (compromisso/ocorrência/pagamento) diagnosticou que nenhuma
  infraestrutura de upload/anexo existe em nenhum domínio do projeto e optou por não implementar
  armazenamento improvisado, exatamente conforme já reservado abaixo para `LES-F2-010`. Nenhum campo
  de comprovante foi adicionado ao modelo de dados de `ocorrencia_compromisso`/
  `pagamento_ocorrencia_compromisso` nesta entrega; ver
  `docs/empresas/financeiro-les/IMPLEMENTACAO-F2-007.md`, seção "Comprovantes".
- **Risco real encontrado na LES-TECH-001: Docker/Docker Desktop indisponível neste ambiente de
  execução.** A tarefa entregou ambiente Docker Compose, perfil Spring isolado, scripts (`subir.cmd`/
  `parar.cmd`/`validar.cmd`) e consultas de diagnóstico (`verificacoes.sql`) para validar as migrations
  `V1`-`V12` contra PostgreSQL real, mas **nenhuma execução real foi possível** (`docker`/`docker compose`
  ausentes tanto no Git Bash quanto no PowerShell). As migrations continuam validadas apenas indiretamente
  (schema gerado pelo Hibernate a partir das entidades, usado pelos testes JPA em H2) — nunca contra
  PostgreSQL real. Isso permanece uma pendência genuína até que alguém rode
  `scripts\postgresql\validar.cmd` em uma máquina com Docker Desktop instalado e reporte o resultado. Ver
  `docs/empresas/financeiro-les/VALIDACAO-POSTGRESQL.md` para o procedimento completo.

## Pendências de navegação e experiência (LES-F1-004)

- Validar a ordem final da sidebar com uso real; a proposta prioriza Dashboard, lançamentos,
  cartões, planejamento e terceiros.
- Confirmar a navegação inferior mobile `Início | Lançar | Simular | Agenda | Mais`; ela favorece
  uso com uma mão, mas ocupa espaço e pode duplicar o menu off-canvas.
- Validar por protótipo quantos indicadores cabem antes da rolagem em 1280 × 720 e 360 × 800; a
  especificação propõe no máximo seis na primeira faixa desktop.
- Definir quais comparações serão gráficos no MVP. Cards/tabelas podem bastar inicialmente; todo
  gráfico terá resumo e tabela equivalente.
- Confirmar se a Agenda do MVP terá tela própria em lista ou ficará no Dashboard. O calendário
  mensal permanece na fase seguinte.
- Confirmar o momento da conciliação manual simples; importação e sugestões avançadas permanecem
  fora do MVP obrigatório conforme `MVP.md`.
- Validar se Histórico e Lixeira ficam diretos em Configurações ou em “Segurança e dados”.
- Confirmar a necessidade de central de anexos; o MVP usa acesso dentro do registro.
- Avaliar uma visão consolidada adicional para Empréstimos, Compromissos e Recebíveis, sem
  unificar seus cálculos ou naturezas.
- Definir formatos, tamanho máximo e visualização no navegador para anexos, respeitando allowlist,
  armazenamento privado e autorização por empresa.
- Definir o comportamento técnico futuro em perda de conexão; o MVP não assume suporte offline.

## Classificação para início da implementação (LES-F1-005)

### Bloqueia implementação

- Aprovação do roadmap técnico e do escopo de `LES-F2-001` antes de iniciar código.

Não há dado real bloqueando a primeira tarefa. Uma decisão técnica passa a bloquear apenas a tarefa
que altera seu respectivo contrato, nunca todo o roadmap.

### Pode ser resolvido durante o desenvolvimento

- Conciliação dos tipos de conta existentes com os tipos conceituais novos, antes da migration que
  alterar ou ampliar `ContaFinanceira`.
- Regra para dia 31 em meses menores, antes de implementar recorrências — **RESOLVIDO na
  LES-F2-006**, usando o último dia válido do mês (ver acima).
- Ordem final da sidebar, navegação inferior mobile, densidade do dashboard e formato inicial da
  Agenda, por protótipo e homologação das respectivas tarefas.
- Local de Histórico/Lixeira, visão consolidada de terceiros e necessidade de central de anexos,
  sem unificar naturezas financeiras distintas.
- Formatos, tamanho máximo e visualização de anexos, antes de `LES-F2-010`.
- Momento da conciliação manual simples, respeitando que importação não bloqueia o primeiro MVP.

### Dado real a preencher depois

- Nomes, e-mails, contas, saldos, salários, recorrências, assinaturas, cartões, limites, datas de
  fechamento/vencimento, bandeiras e titulares.
- Metas adicionais, histórico disponível e compromissos já existentes.

Esses dados não bloqueiam implementação porque serão campos configuráveis e podem ser validados
com valores fictícios. Não devem ser versionados em migration ou fixture.

### Evolução futura

- Redução da quantidade de cartões, conta em espécie opcional e metas nomeadas adicionais.
- Gráficos não essenciais, calendário mensal completo, central de anexos e suporte offline.
- Importação avançada, PDF/OCR, integrações e refinamentos baseados no uso real.

## Documentos relacionados

- `VISAO-FUNCIONAL.md`, `MVP.md`, `GLOSSARIO.md` (LES-F1-001).
- `PROCESSOS.md`, `REGRAS-DE-NEGOCIO.md`, `ESTADOS-E-TRANSICOES.md`, `CALCULOS-E-INDICADORES.md`
  (LES-F1-002).
- `MODELO-DE-DADOS.md`, `ARQUITETURA-FUNCIONAL.md`, `INVARIANTES.md`,
  `MATRIZ-ENTIDADES-PROCESSOS.md` (LES-F1-003).
- `MAPA-DE-TELAS.md`, `NAVEGACAO.md`, `EXPERIENCIA-DO-USUARIO.md`,
  `ESPECIFICACAO-DAS-TELAS.md`, `JORNADAS-DO-USUARIO.md`, `ESTADOS-DE-INTERFACE.md`
  (LES-F1-004).
- `ROADMAP-TECNICO.md`, `PLANO-DE-IMPLEMENTACAO.md`, `DEPENDENCIAS-ENTRE-MODULOS.md`,
  `ESTRATEGIA-DE-TESTES.md`, `BACKLOG-INICIAL.md` (LES-F1-005).
