# Arquitetura — Criati

## Visão geral

A Criati será desenvolvida como um monólito modular.

A aplicação será publicada como uma única unidade, mas o código será organizado por módulos de negócio independentes. Essa abordagem reduz custos e complexidade operacional sem comprometer a organização e a evolução futura.

## Tecnologias principais

- Java 17;
- Spring Boot 3.x;
- Spring MVC;
- Spring Security;
- Spring Data JPA;
- Hibernate;
- Thymeleaf;
- HTML, CSS e JavaScript;
- PostgreSQL;
- Flyway;
- Maven;
- JUnit;
- Git e GitHub.

## Organização por módulos

O código deverá ser organizado prioritariamente por funcionalidade, e não somente por tipo técnico.

Estrutura planejada:

```text
br.app.criati
├── CriatiApplication.java
├── shared
│   ├── config
│   ├── exception
│   ├── validation
│   ├── web
│   └── util
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

## Persistência multiempresa

A fundação utiliza um banco PostgreSQL compartilhado e um único esquema compartilhado. O isolamento lógico ocorre por `empresa_id` nas tabelas empresariais e deve ser reforçado por constraints, consultas contextualizadas e testes automatizados.

O usuário é global e pode possuir vínculos com várias empresas por meio de `usuario_empresa`. Perfis são empresariais, permissões são globais e a atribuição de perfil ocorre sobre o vínculo. A associação `usuario_empresa_perfil` inclui a empresa nas chaves estrangeiras compostas para impedir atribuições entre empresas diferentes.

O Flyway é o único proprietário da criação e evolução do esquema. O Hibernate utiliza obrigatoriamente `spring.jpa.hibernate.ddl-auto=validate` e apenas verifica a compatibilidade entre entidades e migrations.

Riscos futuros que exigem atenção:

- consultas empresariais sem o filtro da empresa atual;
- associações que permitam relacionar registros de empresas diferentes;
- cache ou tarefas assíncronas sem empresa explícita;
- uso de identificadores recebidos do cliente como contexto confiável;
- evolução de migrations sem constraints e índices multiempresa equivalentes.

TenantContext, filtros automáticos e autenticação serão implementados em entregas posteriores e não fazem parte desta fundação.
