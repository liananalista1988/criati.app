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

A configuração definitiva será adicionada durante a preparação técnica.

Variáveis planejadas:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
APP_BASE_URL
```

Exemplo conceitual:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Nunca incluir valores reais neste arquivo.

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