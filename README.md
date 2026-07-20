# Criati.app

Plataforma SaaS modular e multiempresa para transformar processos manuais, planilhas e rotinas desorganizadas em soluções digitais simples, seguras e personalizadas.

## Visão

A Criati atende principalmente pequenos e médios negócios que precisam organizar processos, automatizar tarefas e centralizar informações sem desenvolver um sistema inteiro do zero.

Cada empresa utiliza um núcleo comum e acessa somente os módulos contratados e autorizados.

## Status

Projeto em fase de fundação e documentação.

Situação atual:

- projeto Spring Boot criado;
- Java 17 configurado;
- Maven Wrapper disponível;
- aplicação inicial executando;
- PostgreSQL instalado;
- banco local `criati_db` criado;
- Git configurado;
- repositório privado no GitHub;
- arquitetura multiempresa definida;
- documentação do MVP em construção.

## Arquitetura

A aplicação utilizará um monólito modular.

```text
Criati
├── Núcleo SaaS
│   ├── Autenticação
│   ├── Empresas
│   ├── Unidades
│   ├── Usuários
│   ├── Perfis e permissões
│   ├── Módulos
│   ├── Planos e assinaturas
│   ├── Auditoria
│   └── Notificações
└── Módulos operacionais
    ├── Tarefas e Processos
    ├── Financeiro
    ├── Estoque
    └── Outros módulos futuros
```

## Tecnologias

- Java 17;
- Spring Boot 3.x;
- Spring MVC;
- Spring Security;
- Spring Data JPA;
- Hibernate;
- Thymeleaf;
- HTML;
- CSS;
- JavaScript;
- PostgreSQL;
- Flyway;
- Maven;
- JUnit;
- Git e GitHub.

## Identificação técnica

```text
Group: br.app
Artifact: criati
Package base: br.app.criati
Application name: criati
```

## Modelo multiempresa

- banco PostgreSQL compartilhado;
- isolamento por `empresa_id`;
- unidades opcionais;
- uma Unidade Principal por empresa;
- usuário com acesso a várias empresas;
- perfis diferentes em cada empresa;
- módulos habilitados por empresa;
- autorização obrigatória no backend.

Nenhuma consulta empresarial deverá depender apenas do identificador do registro.

## Escopo do MVP

O MVP será dividido em duas etapas.

### Etapa 1 — Núcleo SaaS

- autenticação;
- recuperação de senha;
- empresas;
- unidades;
- usuários;
- convites;
- seleção de empresa;
- perfis;
- permissões;
- módulos;
- planos;
- assinaturas manuais;
- configurações;
- auditoria;
- notificações;
- dashboard.

### Etapa 2 — Tarefas e Processos

- tarefas;
- responsáveis;
- participantes;
- prioridades;
- prazos;
- categorias;
- fluxos personalizados;
- lista;
- Kanban;
- comentários;
- anexos;
- histórico;
- recorrência;
- alertas;
- indicadores.

## Pré-requisitos

Para executar localmente:

- Java 17;
- PostgreSQL;
- Git;
- VS Code ou outra IDE compatível;
- acesso ao banco local;
- Maven Wrapper incluído no projeto.

Verificar o Java:

```cmd
java -version
```

Resultado esperado:

```text
java version 17
```

## Banco local

Configuração definida:

```text
Servidor: localhost
Porta: 5432
Banco: criati_db
Usuário da aplicação: criati_app
```

A senha deverá permanecer fora do Git.

## Variáveis de ambiente

A aplicação usa perfis Spring separados por ambiente: `local`, `test`, `homolog` e `prod`. O perfil `local` é usado por padrão quando `SPRING_PROFILES_ACTIVE` não é informado. O perfil `test` usa H2 em memória e não depende de variáveis de ambiente.

Variáveis obrigatórias para os perfis `local`, `homolog` e `prod`:

```text
SPRING_PROFILES_ACTIVE
DB_URL
DB_USERNAME
DB_PASSWORD
```

No perfil `local`, `DB_URL` e `DB_USERNAME` têm valores padrão seguros para desenvolvimento (`jdbc:postgresql://localhost:5432/criati_db` e `criati_app`); `DB_PASSWORD` nunca tem valor padrão. Nos perfis `homolog` e `prod`, todas as variáveis são obrigatórias, sem valor padrão.

Variáveis opcionais para o bootstrap do primeiro Superadministrador (qualquer perfil):

```text
CRIATI_BOOTSTRAP_NOME
CRIATI_BOOTSTRAP_EMAIL
CRIATI_BOOTSTRAP_PASSWORD
```

Só têm efeito se ainda não existir nenhum Superadministrador cadastrado; não há senha padrão. Ver [Decisões](docs/DECISOES.MD).

Variável opcional para expiração de convites (qualquer perfil):

```text
CRIATI_CONVITE_EXPIRACAO_HORAS
```

Padrão de 72 horas se não informada.

Nunca incluir valores reais em arquivos versionados.

## Execução local

1. copiar `.env.example` para `.env`:

```cmd
copy .env.example .env
```

2. preencher `DB_PASSWORD` no `.env` com a senha local do PostgreSQL (o `.env` nunca deve ser commitado);
3. iniciar a aplicação pelo Maven Wrapper ou pelo VS Code.

O `.env` é carregado automaticamente pela aplicação quando presente na raiz do projeto (`spring.config.import`), e também é ignorado pelo Git. Apenas o `.env.example` deve ser versionado.

## Executando no Windows

Na raiz do projeto:

```cmd
mvnw.cmd spring-boot:run
```

## Executando no Linux ou macOS

```bash
./mvnw spring-boot:run
```

## Executando testes no Windows

```cmd
mvnw.cmd test
```

## Executando testes no Linux ou macOS

```bash
./mvnw test
```

## Acesso local

Quando a aplicação iniciar:

```text
http://localhost:8080
```

Enquanto a autenticação própria não for implementada, o Spring Security poderá apresentar a tela de login padrão e uma senha temporária no terminal.

Essa autenticação será substituída na fase apropriada.

## Superadministrador e endpoints administrativos

Superadministrador é um papel global da plataforma (não é um perfil de empresa). O primeiro é criado apenas pelo bootstrap descrito em "Variáveis de ambiente" — não existe endpoint público para essa criação.

Endpoints que exigem Superadministrador:

```text
POST /api/empresas
POST /api/usuarios
POST /api/admin/empresas
GET  /api/admin/empresas
GET  /api/admin/me
```

`POST /api/admin/empresas` cria a empresa e seu primeiro administrador (com o vínculo `ADMINISTRADOR`) em uma única operação transacional — é o caminho para dar os primeiros passos em uma empresa nova, já que `POST /api/usuarios-empresas` exige uma empresa ativa selecionada, que por sua vez exige um vínculo já existente.

## Convites

Os demais usuários de uma empresa (além do primeiro administrador) entram por convite, criado por um `ADMINISTRADOR` da própria empresa:

```text
POST   /api/contexto/convites            (ADMINISTRADOR, empresa ativa)
GET    /api/contexto/convites            (ADMINISTRADOR, empresa ativa)
DELETE /api/contexto/convites/{id}       (ADMINISTRADOR, empresa ativa)
GET    /api/convites/{token}             (público)
POST   /api/convites/{token}/aceitar     (público)
```

O convidado define sua própria senha ao aceitar; nenhuma senha é enviada por e-mail nem criada pelo administrador. Não há integração de e-mail real nesta fase — o token do convite só é retornado na resposta de criação em `local`/`test` (`criati.convite.expor-token-bruto`). Detalhes completos (token, expiração, política de duplicidade, tratamento de e-mail já cadastrado) em [Decisões](docs/DECISOES.MD).

## Gestão de acessos por empresa

Um `ADMINISTRADOR` gerencia os integrantes da empresa ativa (o vínculo `UsuarioEmpresa`, nunca o `Usuario` global):

```text
GET    /api/contexto/usuarios                  (ADMINISTRADOR, empresa ativa; filtros: status, perfil, busca)
GET    /api/contexto/usuarios/{id}             (ADMINISTRADOR, empresa ativa)
PATCH  /api/contexto/usuarios/{id}/perfil      (ADMINISTRADOR, empresa ativa)
POST   /api/contexto/usuarios/{id}/suspender   (ADMINISTRADOR, empresa ativa)
POST   /api/contexto/usuarios/{id}/reativar    (ADMINISTRADOR, empresa ativa)
DELETE /api/contexto/usuarios/{id}             (ADMINISTRADOR, empresa ativa)
```

Suspensão e remoção são lógicas (o vínculo vira `INATIVO`; nunca há `DELETE` físico), preservam o `Usuario` global e os vínculos com outras empresas, e são bloqueadas quando afetariam o próprio vínculo do chamador ou o último `ADMINISTRADOR` ativo da empresa. Detalhes completos em [Decisões](docs/DECISOES.MD), seção "Gestão de acessos por empresa".

## Migrations

O Flyway será responsável pela estrutura do banco.

Diretório planejado:

```text
src/main/resources/db/migration
```

Exemplos:

```text
V1__cria_estrutura_inicial.sql
V2__cria_empresas_e_unidades.sql
V3__cria_usuarios_e_vinculos.sql
```

Migrations já executadas não poderão ser editadas.

## Estrutura planejada

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

A organização atual será ajustada gradualmente durante a implementação.

## Documentação

Antes de trabalhar no projeto, leia:

- [Visão do Produto](docs/VISAO_DO_PRODUTO.md)
- [Decisões](docs/DECISOES.md)
- [Arquitetura](docs/ARQUITETURA.md)
- [Modelo Multiempresa](docs/MODELO_MULTIEMPRESA.md)
- [Escopo do MVP](docs/ESCOPO_MVP.md)
- [Banco de Dados](docs/BANCO_DE_DADOS.md)
- [Segurança](docs/SEGURANCA.md)
- [Roadmap](docs/ROADMAP.md)
- [Regras para agentes](AGENTS.md)

## Segurança

Regras essenciais:

- não registrar segredos no Git;
- não salvar senha em texto puro;
- manter CSRF habilitado;
- validar permissões no backend;
- filtrar dados pela empresa atual;
- proteger uploads e downloads;
- utilizar Flyway;
- utilizar Hibernate em modo `validate`;
- registrar ações críticas;
- testar isolamento entre empresas.

Consulte [SEGURANCA.md](docs/SEGURANCA.md).

## Fluxo de trabalho

Antes de alterar:

```cmd
git status
```

Depois de alterar:

```cmd
git diff
git status
```

Antes de concluir:

```cmd
mvnw.cmd test
```

Commits deverão ser pequenos e descritivos.

Exemplo:

```text
Configura integração com PostgreSQL e Flyway
```

## Branch principal

```text
main
```

Novas estratégias de branches poderão ser definidas quando houver necessidade real.

## Repositório

```text
https://github.com/liananalista1988/criati.app
```

O repositório é privado.

## Roadmap resumido

1. concluir documentação;
2. configurar ambientes;
3. integrar PostgreSQL e Flyway;
4. implementar autenticação;
5. implementar empresas e unidades;
6. implementar usuários e convites;
7. implementar seleção de empresa;
8. implementar perfis e permissões;
9. implementar módulos, planos e assinaturas;
10. implementar auditoria e notificações;
11. implementar dashboard;
12. implementar Tarefas e Processos;
13. homologar com empresa piloto;
14. publicar a primeira versão.

Consulte [ROADMAP.md](docs/ROADMAP.md).

## Funcionalidades futuras

- modelos completos de processos;
- suporte interno temporário e auditado;
- WhatsApp;
- cobrança automática;
- inteligência artificial;
- novos módulos;
- API REST;
- aplicativo móvel;
- autenticação multifator;
- personalização avançada;
- realidade aumentada quando houver aplicação comercial.

## Contribuição

Este é um projeto privado.

Qualquer pessoa ou agente que trabalhar no projeto deverá seguir o arquivo [AGENTS.md](AGENTS.md).

Mudanças estruturais exigem aprovação e atualização da documentação.

## Licença

Uso privado e proprietário da Criati.app.

Todos os direitos reservados.