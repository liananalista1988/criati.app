# AGENTS.md — Regras de Trabalho do Projeto Criati

## Objetivo

Este arquivo contém instruções obrigatórias para qualquer pessoa ou agente de IA que trabalhe no projeto Criati.

Antes de alterar código, banco, dependências ou estrutura, leia:

```text
AGENTS.md
docs/VISAO_DO_PRODUTO.md
docs/DECISOES.md
docs/ARQUITETURA.md
docs/MODELO_MULTIEMPRESA.md
docs/ESCOPO_MVP.md
docs/BANCO_DE_DADOS.md
docs/SEGURANCA.md
docs/ROADMAP.md
```

As decisões documentadas representam a arquitetura aprovada do projeto.

## Idioma

- Código e identificadores: português, salvo termos técnicos consolidados.
- Documentação: português.
- Mensagens para o usuário: português claro.
- Commits: português.
- Nomes de bibliotecas e padrões técnicos permanecem no idioma original.

## Informações do projeto

```text
Projeto: Criati
Group: br.app
Artifact: criati
Package base: br.app.criati
Java: 17
Build: Maven
Backend: Spring Boot 4.1.0
Frontend: Thymeleaf, HTML, CSS e JavaScript
Banco: PostgreSQL
Banco local: criati_db
Arquitetura: monólito modular
```

## Papel do agente

O agente deverá atuar como programador responsável, respeitando:

- visão do produto;
- decisões aprovadas;
- arquitetura modular;
- segurança multiempresa;
- escopo atual;
- padrões do projeto;
- histórico do Git.

O agente não deverá redefinir o produto ou a arquitetura silenciosamente.

## Antes de iniciar uma tarefa

O agente deverá:

1. ler os documentos relevantes;
2. inspecionar a estrutura atual;
3. verificar `git status`;
4. identificar arquivos relacionados;
5. explicar resumidamente o plano;
6. apontar dúvidas ou riscos;
7. implementar somente o escopo solicitado;
8. validar o resultado.

Se houver contradição entre documentos, parar e solicitar decisão.

## Regra de escopo

Realizar somente o que foi solicitado.

Não aproveitar uma tarefa para:

- reescrever módulos não relacionados;
- trocar tecnologias;
- atualizar todas as dependências;
- modificar identidade visual;
- criar funcionalidades extras;
- reorganizar todo o projeto;
- excluir código do usuário;
- alterar documentos aprovados sem autorização.

Melhorias adicionais poderão ser sugeridas separadamente.

## Decisões que exigem aprovação

Não alterar sem aprovação:

- Java;
- versão principal do Spring Boot;
- banco de dados;
- estratégia multiempresa;
- arquitetura modular;
- Thymeleaf;
- Flyway;
- Hibernate `validate`;
- UUID;
- modelo de autenticação;
- modelo de permissões;
- módulos do MVP;
- dependências estruturais;
- estratégia de armazenamento;
- política de auditoria;
- nomes principais do projeto;
- package base.

Quando sugerir mudança, informar:

```text
problema
alternativas
recomendação
impactos
risco
esforço estimado
```

## Arquitetura

A Criati será um monólito modular organizado por funcionalidade.

Estrutura planejada:

```text
br.app.criati
├── shared
├── security
├── tenant
├── empresa
├── unidade
├── usuario
├── acesso
├── perfil
├── permissao
├── modulo
├── plano
├── assinatura
├── auditoria
├── notificacao
├── arquivo
└── tarefa
```

Dentro de um módulo, quando necessário:

```text
controller
dto
model
repository
service
validation
mapper
```

Não criar pastas vazias apenas para reproduzir uma estrutura.

## Dependências entre camadas

Fluxo padrão:

```text
Controller → Service → Repository → Banco
```

Regras:

- controller não acessa repository diretamente;
- controller não contém regra de negócio;
- entidade não depende de controller;
- DTO não é entidade;
- templates não acessam diretamente o banco;
- validações críticas ficam no backend;
- integração entre módulos ocorre por serviços ou contratos claros.

## Controllers

Controllers deverão:

- receber requisições;
- validar entradas;
- chamar services;
- preparar página ou resposta;
- tratar redirecionamentos;
- utilizar DTOs;
- permanecer pequenos.

Não deverão:

- executar consulta diretamente;
- definir empresa atual a partir de parâmetro não validado;
- implementar regras extensas;
- retornar entidade JPA diretamente em APIs futuras.

## Services

Services deverão:

- concentrar casos de uso;
- validar regras de negócio;
- validar autorização;
- trabalhar com o contexto da empresa;
- controlar transações;
- chamar repositories;
- gerar auditoria quando necessário.

Utilizar `@Transactional` de forma consciente.

Operações somente de leitura poderão utilizar:

```java
@Transactional(readOnly = true)
```

## Repositories

Repositories empresariais deverão consultar pelo contexto da empresa.

Preferir:

```text
findByIdAndEmpresaId(...)
findAllByEmpresaId(...)
existsByIdAndEmpresaId(...)
```

Evitar:

```text
findById(...)
findAll()
```

em dados empresariais, salvo quando houver motivo administrativo seguro e explícito.

## Multiempresa

Esta é uma regra crítica.

Toda operação empresarial deverá validar:

```text
usuário autenticado
empresa atual
vínculo ativo
unidades autorizadas
perfis
permissões
módulo ativo
status da empresa
status da assinatura
```

Nunca confiar diretamente em:

```text
empresa_id da URL
empresa_id do formulário
campo oculto
cabeçalho do navegador
parâmetro JavaScript
```

O contexto deverá ser derivado da sessão autenticada e validada.

## Consultas empresariais

Uma consulta por UUID não é suficiente.

Correto:

```text
recurso_id + empresa_id atual
```

Incorreto:

```text
somente recurso_id
```

Se o registro pertencer a outra empresa, responder como recurso não encontrado ou acesso negado conforme a política, sem revelar sua existência.

## Unidades

Ao utilizar `unidade_id`, validar:

- unidade pertence à empresa atual;
- unidade está ativa;
- usuário tem acesso à unidade;
- registro relacionado pertence à mesma empresa.

## Segurança

Seguir `docs/SEGURANCA.md`.

Nunca:

- salvar senha em texto puro;
- enviar senha por e-mail;
- registrar senha ou token;
- desativar CSRF globalmente;
- liberar todas as rotas;
- confiar somente na interface;
- concatenar SQL;
- expor stack trace;
- colocar segredos no Git;
- armazenar arquivo enviado em pasta pública;
- permitir download sem autorização.

## Spring Security

- Negar acesso por padrão.
- Liberar somente rotas públicas necessárias.
- Manter CSRF habilitado.
- Utilizar proteção no backend.
- Aplicar autorização em URL e caso de uso.
- Usar mensagens genéricas no login e recuperação.
- Invalidar sessões quando apropriado.
- Preparar contas administrativas para MFA futuro.

## Senhas e tokens

- Utilizar `PasswordEncoder`.
- Preferir Argon2id.
- Tokens devem ser aleatórios, temporários e de uso único.
- Token utilizado deverá ser invalidado.
- Token expirado deverá ser rejeitado.
- Nunca registrar token completo.
- Nunca guardar senha reversível.

## Banco de dados

Seguir `docs/BANCO_DE_DADOS.md`.

Regras obrigatórias:

```text
PostgreSQL
Flyway
UUID
TIMESTAMPTZ para instantes
empresa_id em tabelas empresariais
Hibernate ddl-auto=validate
```

## Flyway

Diretório:

```text
src/main/resources/db/migration
```

Regras:

- toda mudança terá nova migration;
- não editar migration já executada;
- utilizar nome descritivo;
- incluir chaves e restrições;
- incluir índices necessários;
- preservar dados;
- testar migrations;
- não inserir dados de demonstração em produção.

## JPA e entidades

- Usar UUID nas entidades principais.
- Evitar expor entidades diretamente.
- Implementar `equals` e `hashCode` com cuidado.
- Evitar `toString` incluindo relacionamentos.
- Evitar carregamento excessivo.
- Não utilizar `FetchType.EAGER` indiscriminadamente.
- Não utilizar `CascadeType.ALL` sem analisar impacto.
- Não usar Lombok `@Data` automaticamente em entidades.
- Utilizar auditoria padronizada.
- Considerar `@Version` quando houver concorrência.

## DTOs

Criar DTOs específicos para:

- formulário;
- criação;
- edição;
- listagem;
- detalhes;
- resposta de API futura.

Não permitir que o cliente envie campos administrativos indevidos.

Exemplos:

```text
empresa_id
administrador
status interno
plano_id
criado_por
```

Esses campos deverão ser definidos ou validados pelo backend.

## Validação

Utilizar Bean Validation quando apropriado:

```text
@NotNull
@NotBlank
@Size
@Email
@Pattern
```

Toda validação relevante deverá existir no backend.

Mensagens deverão ser claras e apropriadas para o usuário.

## Tratamento de erros

Utilizar tratamento global.

O usuário não deverá visualizar:

- stack trace;
- SQL;
- caminho local;
- credencial;
- tabela interna;
- detalhes de outra empresa.

Logs técnicos deverão conter apenas dados necessários.

## Interface

A interface seguirá a identidade da Criati:

```text
gradiente azul-roxo
visual moderno
clareza
simplicidade
responsividade
```

Regras:

- funcionar em computador e celular;
- componentes reutilizáveis;
- contraste adequado;
- estados de foco;
- mensagens de erro próximas ao campo;
- confirmação para ações críticas;
- empresa atual sempre visível;
- modo de suporte futuro sempre visível quando ativo.

## Thymeleaf

- Utilizar fragments para componentes comuns.
- Manter escape de HTML habilitado.
- Evitar `th:utext`.
- Incluir token CSRF nos formulários.
- Não implementar regra de autorização somente no template.
- O template pode ocultar ações, mas o backend deverá protegê-las.

## JavaScript

- Utilizar somente quando agregar interação necessária.
- Evitar duplicar regras de negócio.
- Não armazenar segredo.
- Não confiar em validação somente no navegador.
- Tratar erros.
- Evitar dependências desnecessárias.
- Manter scripts organizados por funcionalidade.

## Arquivos

- Validar extensão, conteúdo e tamanho.
- Utilizar allowlist.
- Gerar nome interno.
- Não confiar no nome original.
- Armazenar fora da pasta pública.
- Verificar empresa antes de download.
- Registrar metadados no PostgreSQL.
- Não armazenar arquivo binário diretamente no banco sem decisão aprovada.

## Auditoria

Auditar ações críticas.

Não auditar conteúdo sensível desnecessário.

Auditoria deverá conter, quando aplicável:

```text
evento
usuário
empresa
registro
resultado
data e hora
origem
detalhes seguros
```

## Logs

Utilizar níveis adequados:

```text
ERROR
WARN
INFO
DEBUG
TRACE
```

Produção não deverá manter logs excessivos em `DEBUG` ou `TRACE`.

Nunca registrar:

```text
senha
token
cookie
segredo
chave de API
conteúdo integral de documento sensível
```

## E-mail

E-mail no MVP será utilizado para:

- convite;
- recuperação de senha;
- eventos importantes;
- notificações configuradas.

Regras:

- nunca enviar senha;
- usar link HTTPS em produção;
- token temporário;
- conteúdo mínimo necessário;
- falha no e-mail não pode deixar operação inconsistente.

## Testes

Toda funcionalidade deverá possuir testes proporcionais ao risco.

Prioridades:

- regras de negócio;
- isolamento por empresa;
- permissões;
- unidades;
- autenticação;
- tokens;
- módulos ativos;
- migrations;
- operações críticas.

Teste obrigatório:

```text
Usuário da Empresa A não consegue acessar dados da Empresa B.
```

Também testar acesso utilizando UUID válido pertencente a outra empresa.

## Comandos de validação

No Windows:

```cmd
mvnw.cmd test
```

Em Linux ou macOS:

```bash
./mvnw test
```

Para executar localmente no Windows:

```cmd
mvnw.cmd spring-boot:run
```

Não declarar uma tarefa concluída se o projeto não compilar ou se testes relevantes falharem.

## Dependências

Antes de adicionar uma dependência:

1. confirmar se é necessária;
2. verificar manutenção;
3. verificar compatibilidade;
4. analisar segurança;
5. evitar duplicar recurso já disponível;
6. explicar o motivo.

Não atualizar várias dependências durante uma tarefa não relacionada.

## Configurações

Credenciais deverão utilizar variáveis de ambiente:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Não colocar valores reais nos arquivos versionados.

## Git

Antes de alterar:

```cmd
git status
```

Depois de alterar:

```cmd
git diff
git status
```

Regras:

- preservar alterações existentes do usuário;
- não apagar mudanças não relacionadas;
- não usar `git reset --hard`;
- não usar comandos destrutivos sem autorização;
- não criar commit sem solicitação;
- não realizar push sem solicitação;
- não enviar segredos;
- commits devem ser pequenos e descritivos.

## Commits

Padrão recomendado:

```text
Adiciona cadastro de empresas
Implementa seleção de empresa
Configura migrations do Flyway
Corrige isolamento de tarefas
Documenta regras de segurança
```

Evitar:

```text
ajustes
mudanças
update
teste
final
```

## Documentação

Ao alterar comportamento relevante:

- atualizar documentação;
- registrar nova decisão quando necessário;
- atualizar escopo;
- atualizar roadmap;
- explicar configuração;
- documentar migration;
- documentar variáveis de ambiente.

Código e documentação deverão permanecer consistentes.

## Proibições arquiteturais

Não implementar sem aprovação:

- microsserviços;
- banco por empresa;
- comunicação distribuída;
- React, Angular ou Vue;
- aplicativo móvel;
- gateway de pagamento;
- WhatsApp;
- inteligência artificial;
- Row-Level Security;
- autenticação externa;
- domínio personalizado;
- realidade aumentada.

Esses itens estão planejados ou poderão ser avaliados futuramente.

## Interação com o usuário

Ao iniciar uma tarefa, informar:

```text
o que será feito
arquivos esperados
riscos ou decisões necessárias
forma de validação
```

Durante o trabalho:

- comunicar bloqueios;
- não esconder erros;
- não afirmar sucesso sem testar;
- pedir decisão quando o impacto for estrutural.

Ao concluir, informar:

```text
resultado
arquivos alterados
testes executados
resultado dos testes
pendências
próximo passo recomendado
```

## Perguntas obrigatórias

O agente deverá perguntar antes de prosseguir quando:

- houver mais de uma interpretação relevante;
- faltar uma decisão de negócio;
- houver risco de perda de dados;
- for necessário alterar arquitetura;
- for necessário adicionar serviço pago;
- houver mudança de escopo;
- houver conflito entre documentos;
- uma ação externa produzir impacto relevante.

Dúvidas simples que não mudam o escopo poderão ser resolvidas com uma suposição explícita e conservadora.

## Definição de pronto

Uma tarefa somente será considerada pronta quando:

- requisito foi atendido;
- arquitetura foi respeitada;
- isolamento multiempresa foi verificado;
- segurança foi considerada;
- código compila;
- testes relevantes passam;
- migrations foram testadas;
- interface foi verificada, quando aplicável;
- documentação foi atualizada;
- nenhuma credencial foi incluída;
- alterações foram explicadas.

## Prioridade atual

A prioridade é concluir o núcleo SaaS e depois o módulo Tarefas e Processos.

Ordem geral:

1. configuração dos ambientes;
2. Flyway;
3. autenticação;
4. empresas;
5. unidades;
6. usuários e convites;
7. seleção de empresa;
8. perfis e permissões;
9. módulos;
10. planos e assinaturas;
11. auditoria;
12. dashboard;
13. tarefas;
14. fluxos;
15. lista;
16. Kanban;
17. comentários;
18. anexos;
19. recorrência;
20. notificações;
21. indicadores.

## Regra principal

A Criati deve permanecer simples de executar, segura para múltiplas empresas, organizada por módulos e orientada a problemas reais dos clientes.