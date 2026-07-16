# Arquitetura — Criati

## Visão geral

A Criati será desenvolvida como um monólito modular.

A aplicação será publicada como uma única unidade, mas o código será organizado por módulos de negócio independentes. Essa abordagem reduz custos e complexidade operacional sem comprometer a organização e a evolução futura.

## Tecnologias principais

- Java 17;
- Spring Boot 4.1.0;
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