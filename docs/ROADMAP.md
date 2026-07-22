# Roadmap — Criati

## Objetivo

Este documento organiza a evolução da Criati em fases, evitando que funcionalidades futuras desviem o foco do MVP.

## Roadmap específico — Financeiro LeS

O Financeiro LeS possui roadmap técnico próprio, organizado em fundação, operação financeira,
cartões e faturas, planejamento, terceiros, simulador e automações posteriores. A primeira tarefa
recomendada é `LES-F2-001 — Preparação técnica do domínio Financeiro LeS`. Fases, dependências,
gates, testes e backlog estão em
[`empresas/financeiro-les/ROADMAP-TECNICO.md`](empresas/financeiro-les/ROADMAP-TECNICO.md), sem
alterar a prioridade geral do núcleo SaaS registrada neste documento.

O roadmap poderá ser atualizado conforme:

- validação com clientes;
- capacidade da equipe;
- necessidades comerciais;
- custos de infraestrutura;
- riscos técnicos;
- retorno das funcionalidades.

## Legenda

```text
CONCLUÍDO
EM ANDAMENTO
PLANEJADO
FUTURO
```

# Fase 0 — Fundação do projeto

Status: **EM ANDAMENTO**

## Concluído

- definição da Criati como SaaS genérico e multiempresa;
- Java 17;
- Spring Boot 3.x;
- Maven;
- VS Code;
- PostgreSQL instalado;
- banco local `criati_db`;
- projeto executando;
- Spring Security ativo;
- Git configurado;
- repositório privado no GitHub;
- branch principal `main`;
- primeiro commit;
- arquitetura de monólito modular aprovada;
- Thymeleaf aprovado;
- Flyway aprovado;
- UUID aprovado;
- banco compartilhado com isolamento por empresa aprovado.

## Em andamento

- documentação do produto;
- documentação da arquitetura;
- modelo multiempresa;
- escopo do MVP;
- regras do banco;
- regras de segurança;
- roadmap;
- instruções para agentes de IA;
- README principal.

## Critério de conclusão

- todos os documentos revisados;
- decisões sem contradições;
- tarefas iniciais priorizadas;
- projeto limpo no Git;
- documentação enviada ao GitHub.

# Fase 1 — Preparação técnica

Status: **PLANEJADO**

## Objetivo

Preparar o projeto para desenvolvimento seguro em diferentes ambientes.

## Entregas

- reorganização dos pacotes por módulo;
- perfis `local`, `test`, `homolog` e `prod`;
- variáveis de ambiente;
- conexão com PostgreSQL;
- Flyway configurado;
- primeira migration;
- Hibernate em modo `validate`;
- tratamento global de erros;
- auditoria base;
- layout inicial;
- pipeline básico de testes;
- verificação de segredos no Git;
- instruções para executar o projeto.

## Critério de conclusão

- aplicação inicia com PostgreSQL;
- Flyway executa corretamente;
- testes básicos passam;
- nenhuma credencial está no repositório;
- projeto pode ser configurado em outra máquina.

# Fase 2 — Autenticação

Status: **PLANEJADO**

## Objetivo

Substituir o login temporário do Spring por autenticação própria.

## Entregas

- entidade de usuário;
- login personalizado;
- login por e-mail e senha;
- hash seguro de senha;
- logout;
- sessão segura;
- bloqueio de usuário inativo;
- recuperação de senha;
- alteração de senha;
- tokens temporários;
- proteção contra tentativas automatizadas;
- auditoria de eventos relevantes.

## Critério de conclusão

- usuário ativo consegue entrar;
- usuário inativo é bloqueado;
- recuperação de senha funciona;
- tokens expiram e não podem ser reutilizados;
- senhas não aparecem no banco, logs ou Git;
- testes de autenticação passam.

# Fase 3 — Empresas e unidades

Status: **PLANEJADO**

## Objetivo

Criar a base de clientes da plataforma.

## Entregas

- cadastro de empresa;
- edição;
- consulta;
- ativação;
- suspensão;
- cancelamento;
- código amigável;
- fuso horário;
- Unidade Principal automática;
- unidades adicionais;
- ativação e desativação de unidades;
- configurações básicas da empresa;
- painel administrativo inicial.

## Critério de conclusão

- somente a Criati cadastra empresas;
- toda empresa recebe uma Unidade Principal;
- dados ficam isolados;
- suspensão não exclui dados;
- operações críticas são auditadas.

# Fase 4 — Usuários multiempresa e convites

Status: **PLANEJADO**

## Objetivo

Permitir que o mesmo usuário participe de várias empresas.

## Entregas

- vínculo entre usuário e empresa;
- estados do vínculo;
- convite por e-mail;
- validade de 24 horas;
- criação de senha pelo convidado;
- cancelamento;
- reenvio;
- acesso a unidades específicas;
- seleção da empresa após login;
- troca de empresa durante a sessão;
- exibição clara da empresa atual.

## Critério de conclusão

- um usuário acessa várias empresas com o mesmo login;
- permissões não vazam entre empresas;
- convite é de uso único;
- troca de empresa substitui todo o contexto;
- usuário não acessa empresa sem vínculo.

# Fase 5 — Perfis e permissões

Status: **PLANEJADO**

## Objetivo

Controlar o que cada usuário pode fazer dentro de cada empresa.

## Entregas

- catálogo de permissões;
- perfis padrão;
- perfis personalizados;
- associação entre perfil e permissão;
- associação entre vínculo e perfil;
- proteção de URLs;
- proteção de métodos;
- menus conforme acesso;
- consulta de permissões efetivas.

## Critério de conclusão

- esconder botão não é a única proteção;
- backend bloqueia operações indevidas;
- perfil de uma empresa não funciona em outra;
- testes de autorização passam;
- alterações são auditadas.

# Fase 6 — Módulos, planos e assinaturas

Status: **PLANEJADO**

## Objetivo

Controlar comercialmente os recursos disponíveis para cada empresa.

## Entregas

- catálogo de módulos;
- ativação de módulo por empresa;
- desativação sem apagar dados;
- cadastro de planos;
- módulos por plano;
- limites de usuários;
- limites de unidades;
- limites de armazenamento;
- assinatura manual;
- estados da assinatura;
- menus baseados nos módulos ativos.

## Critério de conclusão

- somente a Criati ativa módulos;
- administrador distribui acessos internos;
- URL direta de módulo desativado é bloqueada;
- limites são validados;
- cobrança continua manual.

# Fase 7 — Auditoria e notificações

Status: **PLANEJADO**

## Objetivo

Registrar ações importantes e criar comunicação interna.

## Entregas

- central de auditoria;
- filtros;
- notificações internas;
- preferências do usuário;
- envio de e-mail;
- modelos de e-mail;
- avisos administrativos;
- alertas de segurança básicos.

## Critério de conclusão

- auditoria não é editável pelas telas comuns;
- logs não expõem segredos;
- e-mails não enviam senhas;
- notificações respeitam a empresa;
- falha de e-mail não corrompe a operação principal.

# Fase 8 — Dashboard do núcleo

Status: **PLANEJADO**

## Administração da Criati

- empresas por status;
- assinaturas por status;
- usuários ativos;
- módulos utilizados;
- empresas em implantação;
- alertas administrativos.

## Empresa

- empresa atual;
- unidade atual;
- usuários;
- módulos;
- notificações;
- atalhos autorizados.

## Critério de conclusão

- indicadores respeitam permissões;
- nenhum dado é misturado entre empresas;
- interface funciona em computador e celular.

# Fase 9 — Tarefas e Processos: base

Status: **PLANEJADO**

## Objetivo

Entregar o primeiro módulo operacional da Criati.

## Entregas

- módulo Tarefas e Processos;
- cadastro de tarefas;
- responsáveis;
- participantes;
- unidades;
- categorias;
- prioridades;
- prazos;
- descrição;
- arquivamento;
- permissões do módulo;
- códigos amigáveis.

## Critério de conclusão

- tarefa pertence a uma empresa;
- responsável possui vínculo ativo;
- unidade pertence à mesma empresa;
- operações são autorizadas no backend;
- arquivamento preserva histórico.

# Fase 10 — Fluxos, lista e Kanban

Status: **PLANEJADO**

## Entregas

- fluxo padrão;
- status personalizados;
- cores;
- ordenação;
- status inicial;
- status final;
- visualização em lista;
- paginação;
- pesquisa;
- filtros;
- Kanban;
- movimentação entre status;
- histórico de movimentações.

## Critério de conclusão

- status pertence à empresa;
- status em uso não é apagado;
- movimentação exige permissão;
- Kanban funciona em tela menor;
- alterações geram histórico.

# Fase 11 — Comentários, arquivos e recorrência

Status: **PLANEJADO**

## Entregas

- comentários;
- participantes notificados;
- anexos;
- armazenamento protegido;
- download autorizado;
- tarefas recorrentes;
- recorrência diária;
- recorrência semanal;
- recorrência mensal;
- prevenção de duplicidade;
- registro de falhas.

## Critério de conclusão

- upload possui limite e validação;
- arquivos não ficam publicamente expostos;
- recorrência respeita o fuso da empresa;
- tarefas não são geradas duas vezes;
- desativação preserva tarefas anteriores.

# Fase 12 — Indicadores do módulo

Status: **PLANEJADO**

## Indicadores iniciais

- tarefas por status;
- tarefas atrasadas;
- tarefas por responsável;
- tarefas concluídas;
- tarefas por prioridade;
- tarefas por unidade;
- evolução por período.

## Critério de conclusão

- indicadores utilizam dados da empresa atual;
- filtros funcionam;
- resultados podem ser conferidos;
- consultas possuem desempenho adequado.

# Fase 13 — Homologação e primeiro cliente

Status: **PLANEJADO**

## Objetivo

Validar o sistema em situação real antes de ampliar a operação.

## Entregas

- ambiente de homologação;
- implantação de empresa piloto;
- treinamento;
- coleta de feedback;
- correção de falhas;
- revisão de experiência;
- revisão de permissões;
- revisão de isolamento;
- documentação de suporte;
- checklist de publicação.

## Critério de conclusão

- empresa piloto utiliza o fluxo principal;
- erros críticos foram corrigidos;
- dados permanecem isolados;
- backup e restauração foram testados;
- operação básica está documentada.

# Fase 14 — Produção inicial

Status: **PLANEJADO**

## Entregas

- infraestrutura de produção;
- PostgreSQL de produção;
- armazenamento em nuvem;
- HTTPS;
- domínio do sistema;
- backup automático;
- monitoramento;
- alertas técnicos;
- política de logs;
- procedimento de restauração;
- procedimento de atualização;
- revisão de segurança.

## Critério de conclusão

- publicação reproduzível;
- segredos protegidos;
- backup restaurável;
- monitoramento ativo;
- aplicação acessível por HTTPS;
- migrations executadas com segurança.

# Evoluções posteriores ao MVP

Status: **FUTURO**

## Modelos completos de processos

- modelos reutilizáveis;
- etapas;
- dependências;
- responsáveis padrão;
- prazos relativos;
- criação automática de tarefas;
- acompanhamento do processo completo.

## Suporte interno temporário

- modo de suporte;
- justificativa;
- tempo limitado;
- aviso visível;
- auditoria;
- encerramento automático;
- restrições para ações críticas.

## WhatsApp

- notificações selecionadas;
- consentimento;
- modelos aprovados;
- preferências;
- limites;
- integração com provedor oficial.

## Cobrança automática

- gateway de pagamento;
- cobrança recorrente;
- faturas;
- avisos de vencimento;
- conciliação;
- alteração automática de status;
- histórico financeiro.

## Inteligência artificial

- auxílio na criação de relatórios;
- resumo de atividades;
- identificação de atrasos;
- sugestões;
- pesquisa inteligente;
- geração assistida de textos;
- controles de privacidade;
- limites por plano;
- revisão humana para decisões críticas.

## Novos módulos

Possibilidades:

```text
Financeiro
Estoque
Clientes
Vendas
Documentos
Agenda
Atendimento
Relatórios
Investimentos
Integrações
```

Cada novo módulo terá escopo e documentação próprios.

## API e integrações

- API REST;
- chaves por empresa;
- limites por plano;
- webhooks;
- integrações com sistemas externos;
- documentação da API;
- auditoria;
- rate limiting.

## Segurança avançada

- autenticação multifator;
- alertas de login;
- gestão centralizada de segredos;
- análise de vulnerabilidades;
- antivírus de arquivos;
- Content Security Policy;
- rate limiting distribuído;
- Row-Level Security;
- revisão externa de segurança.

## Personalização avançada

- logotipo da empresa;
- cores;
- domínio próprio;
- templates;
- dashboards personalizados;
- relatórios personalizados.

## Aplicativo móvel

Será avaliado somente após:

- validação do sistema web responsivo;
- demanda real de clientes;
- APIs estabilizadas;
- análise de custo e manutenção.

## Realidade aumentada

Poderá ser utilizada como diferencial demonstrativo ou em módulos específicos, desde que exista utilidade comercial.

Não faz parte do MVP.

Exemplos futuros:

- visualização de produtos;
- experiências promocionais;
- demonstrações interativas;
- treinamento;
- posicionamento de objetos no ambiente.

# Itens que não devem antecipar o MVP

- microsserviços;
- aplicativo nativo;
- marketplace;
- múltiplos bancos sem necessidade;
- arquitetura excessivamente distribuída;
- funções de IA sem controle;
- integrações sem cliente interessado;
- personalizações que prejudiquem o núcleo comum.

# Processo de inclusão no roadmap

Uma nova funcionalidade deverá responder:

1. qual problema resolve?
2. quem utilizará?
3. existe cliente ou evidência de demanda?
4. pertence ao núcleo ou a um módulo?
5. qual o risco de segurança?
6. qual o custo de manutenção?
7. altera o banco?
8. exige novo fornecedor?
9. pode ser entregue depois?
10. qual o critério de aceite?

# Definição de prioridade

Prioridade alta:

- segurança;
- isolamento;
- autenticação;
- empresas;
- usuários;
- permissões;
- estabilidade;
- backup;
- tarefas do MVP.

Prioridade média:

- melhorias de experiência;
- indicadores;
- automações validadas;
- integrações solicitadas.

Prioridade baixa:

- efeitos visuais;
- tecnologias demonstrativas;
- integrações sem demanda;
- personalizações isoladas;
- funcionalidades que não ajudam o primeiro cliente.

# Princípio principal

A Criati deverá crescer conforme problemas reais de clientes, preservando uma base comum, segura, modular e sustentável.# Roadmap — Criati

## Objetivo

Este documento organiza a evolução da Criati em fases, evitando que funcionalidades futuras desviem o foco do MVP.

O roadmap poderá ser atualizado conforme:

- validação com clientes;
- capacidade da equipe;
- necessidades comerciais;
- custos de infraestrutura;
- riscos técnicos;
- retorno das funcionalidades.

## Legenda

```text
CONCLUÍDO
EM ANDAMENTO
PLANEJADO
FUTURO
```

# Fase 0 — Fundação do projeto

Status: **EM ANDAMENTO**

## Concluído

- definição da Criati como SaaS genérico e multiempresa;
- Java 17;
- Spring Boot 3.x;
- Maven;
- VS Code;
- PostgreSQL instalado;
- banco local `criati_db`;
- projeto executando;
- Spring Security ativo;
- Git configurado;
- repositório privado no GitHub;
- branch principal `main`;
- primeiro commit;
- arquitetura de monólito modular aprovada;
- Thymeleaf aprovado;
- Flyway aprovado;
- UUID aprovado;
- banco compartilhado com isolamento por empresa aprovado.

## Em andamento

- documentação do produto;
- documentação da arquitetura;
- modelo multiempresa;
- escopo do MVP;
- regras do banco;
- regras de segurança;
- roadmap;
- instruções para agentes de IA;
- README principal.

## Critério de conclusão

- todos os documentos revisados;
- decisões sem contradições;
- tarefas iniciais priorizadas;
- projeto limpo no Git;
- documentação enviada ao GitHub.

# Fase 1 — Preparação técnica

Status: **PLANEJADO**

## Objetivo

Preparar o projeto para desenvolvimento seguro em diferentes ambientes.

## Entregas

- reorganização dos pacotes por módulo;
- perfis `local`, `test`, `homolog` e `prod`;
- variáveis de ambiente;
- conexão com PostgreSQL;
- Flyway configurado;
- primeira migration;
- Hibernate em modo `validate`;
- tratamento global de erros;
- auditoria base;
- layout inicial;
- pipeline básico de testes;
- verificação de segredos no Git;
- instruções para executar o projeto.

## Critério de conclusão

- aplicação inicia com PostgreSQL;
- Flyway executa corretamente;
- testes básicos passam;
- nenhuma credencial está no repositório;
- projeto pode ser configurado em outra máquina.

# Fase 2 — Autenticação

Status: **PLANEJADO**

## Objetivo

Substituir o login temporário do Spring por autenticação própria.

## Entregas

- entidade de usuário;
- login personalizado;
- login por e-mail e senha;
- hash seguro de senha;
- logout;
- sessão segura;
- bloqueio de usuário inativo;
- recuperação de senha;
- alteração de senha;
- tokens temporários;
- proteção contra tentativas automatizadas;
- auditoria de eventos relevantes.

## Critério de conclusão

- usuário ativo consegue entrar;
- usuário inativo é bloqueado;
- recuperação de senha funciona;
- tokens expiram e não podem ser reutilizados;
- senhas não aparecem no banco, logs ou Git;
- testes de autenticação passam.

# Fase 3 — Empresas e unidades

Status: **PLANEJADO**

## Objetivo

Criar a base de clientes da plataforma.

## Entregas

- cadastro de empresa;
- edição;
- consulta;
- ativação;
- suspensão;
- cancelamento;
- código amigável;
- fuso horário;
- Unidade Principal automática;
- unidades adicionais;
- ativação e desativação de unidades;
- configurações básicas da empresa;
- painel administrativo inicial.

## Critério de conclusão

- somente a Criati cadastra empresas;
- toda empresa recebe uma Unidade Principal;
- dados ficam isolados;
- suspensão não exclui dados;
- operações críticas são auditadas.

# Fase 4 — Usuários multiempresa e convites

Status: **PLANEJADO**

## Objetivo

Permitir que o mesmo usuário participe de várias empresas.

## Entregas

- vínculo entre usuário e empresa;
- estados do vínculo;
- convite por e-mail;
- validade de 24 horas;
- criação de senha pelo convidado;
- cancelamento;
- reenvio;
- acesso a unidades específicas;
- seleção da empresa após login;
- troca de empresa durante a sessão;
- exibição clara da empresa atual.

## Critério de conclusão

- um usuário acessa várias empresas com o mesmo login;
- permissões não vazam entre empresas;
- convite é de uso único;
- troca de empresa substitui todo o contexto;
- usuário não acessa empresa sem vínculo.

# Fase 5 — Perfis e permissões

Status: **PLANEJADO**

## Objetivo

Controlar o que cada usuário pode fazer dentro de cada empresa.

## Entregas

- catálogo de permissões;
- perfis padrão;
- perfis personalizados;
- associação entre perfil e permissão;
- associação entre vínculo e perfil;
- proteção de URLs;
- proteção de métodos;
- menus conforme acesso;
- consulta de permissões efetivas.

## Critério de conclusão

- esconder botão não é a única proteção;
- backend bloqueia operações indevidas;
- perfil de uma empresa não funciona em outra;
- testes de autorização passam;
- alterações são auditadas.

# Fase 6 — Módulos, planos e assinaturas

Status: **PLANEJADO**

## Objetivo

Controlar comercialmente os recursos disponíveis para cada empresa.

## Entregas

- catálogo de módulos;
- ativação de módulo por empresa;
- desativação sem apagar dados;
- cadastro de planos;
- módulos por plano;
- limites de usuários;
- limites de unidades;
- limites de armazenamento;
- assinatura manual;
- estados da assinatura;
- menus baseados nos módulos ativos.

## Critério de conclusão

- somente a Criati ativa módulos;
- administrador distribui acessos internos;
- URL direta de módulo desativado é bloqueada;
- limites são validados;
- cobrança continua manual.

# Fase 7 — Auditoria e notificações

Status: **PLANEJADO**

## Objetivo

Registrar ações importantes e criar comunicação interna.

## Entregas

- central de auditoria;
- filtros;
- notificações internas;
- preferências do usuário;
- envio de e-mail;
- modelos de e-mail;
- avisos administrativos;
- alertas de segurança básicos.

## Critério de conclusão

- auditoria não é editável pelas telas comuns;
- logs não expõem segredos;
- e-mails não enviam senhas;
- notificações respeitam a empresa;
- falha de e-mail não corrompe a operação principal.

# Fase 8 — Dashboard do núcleo

Status: **PLANEJADO**

## Administração da Criati

- empresas por status;
- assinaturas por status;
- usuários ativos;
- módulos utilizados;
- empresas em implantação;
- alertas administrativos.

## Empresa

- empresa atual;
- unidade atual;
- usuários;
- módulos;
- notificações;
- atalhos autorizados.

## Critério de conclusão

- indicadores respeitam permissões;
- nenhum dado é misturado entre empresas;
- interface funciona em computador e celular.

# Fase 9 — Tarefas e Processos: base

Status: **PLANEJADO**

## Objetivo

Entregar o primeiro módulo operacional da Criati.

## Entregas

- módulo Tarefas e Processos;
- cadastro de tarefas;
- responsáveis;
- participantes;
- unidades;
- categorias;
- prioridades;
- prazos;
- descrição;
- arquivamento;
- permissões do módulo;
- códigos amigáveis.

## Critério de conclusão

- tarefa pertence a uma empresa;
- responsável possui vínculo ativo;
- unidade pertence à mesma empresa;
- operações são autorizadas no backend;
- arquivamento preserva histórico.

# Fase 10 — Fluxos, lista e Kanban

Status: **PLANEJADO**

## Entregas

- fluxo padrão;
- status personalizados;
- cores;
- ordenação;
- status inicial;
- status final;
- visualização em lista;
- paginação;
- pesquisa;
- filtros;
- Kanban;
- movimentação entre status;
- histórico de movimentações.

## Critério de conclusão

- status pertence à empresa;
- status em uso não é apagado;
- movimentação exige permissão;
- Kanban funciona em tela menor;
- alterações geram histórico.

# Fase 11 — Comentários, arquivos e recorrência

Status: **PLANEJADO**

## Entregas

- comentários;
- participantes notificados;
- anexos;
- armazenamento protegido;
- download autorizado;
- tarefas recorrentes;
- recorrência diária;
- recorrência semanal;
- recorrência mensal;
- prevenção de duplicidade;
- registro de falhas.

## Critério de conclusão

- upload possui limite e validação;
- arquivos não ficam publicamente expostos;
- recorrência respeita o fuso da empresa;
- tarefas não são geradas duas vezes;
- desativação preserva tarefas anteriores.

# Fase 12 — Indicadores do módulo

Status: **PLANEJADO**

## Indicadores iniciais

- tarefas por status;
- tarefas atrasadas;
- tarefas por responsável;
- tarefas concluídas;
- tarefas por prioridade;
- tarefas por unidade;
- evolução por período.

## Critério de conclusão

- indicadores utilizam dados da empresa atual;
- filtros funcionam;
- resultados podem ser conferidos;
- consultas possuem desempenho adequado.

# Fase 13 — Homologação e primeiro cliente

Status: **PLANEJADO**

## Objetivo

Validar o sistema em situação real antes de ampliar a operação.

## Entregas

- ambiente de homologação;
- implantação de empresa piloto;
- treinamento;
- coleta de feedback;
- correção de falhas;
- revisão de experiência;
- revisão de permissões;
- revisão de isolamento;
- documentação de suporte;
- checklist de publicação.

## Critério de conclusão

- empresa piloto utiliza o fluxo principal;
- erros críticos foram corrigidos;
- dados permanecem isolados;
- backup e restauração foram testados;
- operação básica está documentada.

# Fase 14 — Produção inicial

Status: **PLANEJADO**

## Entregas

- infraestrutura de produção;
- PostgreSQL de produção;
- armazenamento em nuvem;
- HTTPS;
- domínio do sistema;
- backup automático;
- monitoramento;
- alertas técnicos;
- política de logs;
- procedimento de restauração;
- procedimento de atualização;
- revisão de segurança.

## Critério de conclusão

- publicação reproduzível;
- segredos protegidos;
- backup restaurável;
- monitoramento ativo;
- aplicação acessível por HTTPS;
- migrations executadas com segurança.

# Evoluções posteriores ao MVP

Status: **FUTURO**

## Modelos completos de processos

- modelos reutilizáveis;
- etapas;
- dependências;
- responsáveis padrão;
- prazos relativos;
- criação automática de tarefas;
- acompanhamento do processo completo.

## Suporte interno temporário

- modo de suporte;
- justificativa;
- tempo limitado;
- aviso visível;
- auditoria;
- encerramento automático;
- restrições para ações críticas.

## WhatsApp

- notificações selecionadas;
- consentimento;
- modelos aprovados;
- preferências;
- limites;
- integração com provedor oficial.

## Cobrança automática

- gateway de pagamento;
- cobrança recorrente;
- faturas;
- avisos de vencimento;
- conciliação;
- alteração automática de status;
- histórico financeiro.

## Inteligência artificial

- auxílio na criação de relatórios;
- resumo de atividades;
- identificação de atrasos;
- sugestões;
- pesquisa inteligente;
- geração assistida de textos;
- controles de privacidade;
- limites por plano;
- revisão humana para decisões críticas.

## Novos módulos

Possibilidades:

```text
Financeiro
Estoque
Clientes
Vendas
Documentos
Agenda
Atendimento
Relatórios
Investimentos
Integrações
```

Cada novo módulo terá escopo e documentação próprios.

## API e integrações

- API REST;
- chaves por empresa;
- limites por plano;
- webhooks;
- integrações com sistemas externos;
- documentação da API;
- auditoria;
- rate limiting.

## Segurança avançada

- autenticação multifator;
- alertas de login;
- gestão centralizada de segredos;
- análise de vulnerabilidades;
- antivírus de arquivos;
- Content Security Policy;
- rate limiting distribuído;
- Row-Level Security;
- revisão externa de segurança.

## Personalização avançada

- logotipo da empresa;
- cores;
- domínio próprio;
- templates;
- dashboards personalizados;
- relatórios personalizados.

## Aplicativo móvel

Será avaliado somente após:

- validação do sistema web responsivo;
- demanda real de clientes;
- APIs estabilizadas;
- análise de custo e manutenção.

## Realidade aumentada

Poderá ser utilizada como diferencial demonstrativo ou em módulos específicos, desde que exista utilidade comercial.

Não faz parte do MVP.

Exemplos futuros:

- visualização de produtos;
- experiências promocionais;
- demonstrações interativas;
- treinamento;
- posicionamento de objetos no ambiente.

# Itens que não devem antecipar o MVP

- microsserviços;
- aplicativo nativo;
- marketplace;
- múltiplos bancos sem necessidade;
- arquitetura excessivamente distribuída;
- funções de IA sem controle;
- integrações sem cliente interessado;
- personalizações que prejudiquem o núcleo comum.

# Processo de inclusão no roadmap

Uma nova funcionalidade deverá responder:

1. qual problema resolve?
2. quem utilizará?
3. existe cliente ou evidência de demanda?
4. pertence ao núcleo ou a um módulo?
5. qual o risco de segurança?
6. qual o custo de manutenção?
7. altera o banco?
8. exige novo fornecedor?
9. pode ser entregue depois?
10. qual o critério de aceite?

# Definição de prioridade

Prioridade alta:

- segurança;
- isolamento;
- autenticação;
- empresas;
- usuários;
- permissões;
- estabilidade;
- backup;
- tarefas do MVP.

Prioridade média:

- melhorias de experiência;
- indicadores;
- automações validadas;
- integrações solicitadas.

Prioridade baixa:

- efeitos visuais;
- tecnologias demonstrativas;
- integrações sem demanda;
- personalizações isoladas;
- funcionalidades que não ajudam o primeiro cliente.

# Princípio principal

A Criati deverá crescer conforme problemas reais de clientes, preservando uma base comum, segura, modular e sustentável.
