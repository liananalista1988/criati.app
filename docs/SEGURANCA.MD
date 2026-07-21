# Segurança — Criati

## Objetivo

Este documento estabelece as regras mínimas de segurança da Criati.

Como a plataforma será multiempresa, uma falha poderá afetar vários clientes. Portanto, segurança, autorização e isolamento deverão fazer parte da implementação desde o início.

## Princípios

- negar acesso por padrão;
- conceder somente o necessário;
- validar toda autorização no backend;
- não confiar em dados enviados pelo navegador;
- aplicar isolamento por empresa em todas as camadas;
- utilizar defesa em profundidade;
- não expor informações sensíveis;
- auditar operações críticas;
- manter dependências atualizadas;
- testar tentativas de acesso indevido.

## Autenticação e autorização

Autenticação responde:

```text
Quem é o usuário?
```

Autorização responde:

```text
O que esse usuário pode fazer nesta empresa?
```

O usuário autenticado não estará automaticamente autorizado a acessar todos os recursos.

A autorização deverá considerar:

```text
usuário
empresa atual
vínculo com a empresa
status do vínculo
unidades autorizadas
perfis
permissões
módulos contratados
status da empresa
status da assinatura
```

## Spring Security

A aplicação utilizará Spring Security.

Responsabilidades:

- autenticação;
- sessão;
- logout;
- proteção de URLs;
- proteção de métodos;
- CSRF;
- cookies seguros;
- controle de acesso;
- integração com recuperação de senha.

Regras:

- rotas serão bloqueadas por padrão;
- somente páginas explicitamente públicas serão liberadas;
- autorização não ficará somente nos controllers;
- casos de uso críticos deverão possuir validação no service;
- poderá ser utilizado `@PreAuthorize` quando apropriado;
- acesso ao painel da Criati será separado do acesso empresarial.

## Rotas públicas

Somente rotas necessárias poderão ser públicas:

```text
/login
/logout
/recuperar-senha
/redefinir-senha
/convite
/css/**
/js/**
/img/**
/error
```

Liberar uma rota pública exigirá justificativa.

### Implementação atual (fase de fundação)

Rotas públicas REST existentes, todas justificadas individualmente:

```text
POST /api/auth/login
GET  /api/convites/{token}
POST /api/convites/{token}/aceitar
```

`GET /api/convites/{token}` e `POST /api/convites/{token}/aceitar` existem porque o convidado ainda não tem conta nem sessão nesta aplicação antes de aceitar o convite. Nenhuma outra rota de API é pública.

Rotas de página (Thymeleaf) e recursos estáticos públicos, adicionadas na fase de interface web:

```text
GET /login
GET /css/**
GET /js/**
GET /img/**
GET /error
```

`/login` é a tela de autenticação; um usuário já autenticado que acessá-la é redirecionado para `/app/dashboard` pelo próprio controller, não pelo `SecurityConfig`. `/css/**`, `/js/**` e `/img/**` são recursos estáticos necessários para renderizar qualquer página, inclusive a de login, antes de existir sessão. `/error` é a página padrão de erro do Spring Boot. Nenhuma rota sob `/app/**` é pública — todas exigem autenticação (`anyRequest().authenticated()`), e um anônimo que tentar acessá-las é redirecionado para `/login` (ver seção "Sessões" e "CSRF" para o detalhe do `AuthenticationEntryPoint` diferenciado por rota).

## Senhas

Senhas nunca serão:

- armazenadas em texto puro;
- enviadas por e-mail;
- registradas nos logs;
- exibidas para administradores;
- incluídas em commits;
- recuperadas do banco em formato original.

A aplicação utilizará `PasswordEncoder`.

Algoritmo preferencial:

```text
Argon2id
```

Se houver uma restrição técnica devidamente justificada, BCrypt poderá ser utilizado com fator de trabalho adequado e documentado.

Regras iniciais:

- mínimo de 15 caracteres enquanto não houver MFA;
- máximo permitido de pelo menos 64 caracteres;
- aceitar frases-senha;
- não remover espaços silenciosamente;
- não truncar senhas;
- permitir caracteres especiais;
- impedir senhas comuns ou conhecidas como comprometidas quando houver mecanismo disponível;
- não exigir troca periódica sem motivo;
- exigir troca quando houver suspeita de comprometimento.

A autenticação seguirá as orientações da [OWASP para autenticação](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html) e [armazenamento de senhas](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).

## Mensagens de login

Mensagens não deverão confirmar se determinado usuário existe.

Mensagem adequada:

```text
E-mail ou senha inválidos.
```

Evitar:

```text
Este e-mail não existe.
A senha está incorreta.
O usuário está bloqueado.
```

Informações detalhadas poderão ser registradas internamente de forma segura.

## Proteção contra tentativas automatizadas

O login deverá possuir proteção contra:

- força bruta;
- password spraying;
- credential stuffing;
- enumeração de usuários.

Medidas previstas:

- limitação progressiva de tentativas;
- atrasos controlados;
- registro de falhas;
- alertas de comportamento anormal;
- bloqueio temporário, quando apropriado;
- CAPTCHA somente quando necessário;
- limitação por conta e por origem.

O sistema não deverá permitir que um atacante bloqueie permanentemente usuários apenas provocando tentativas inválidas.

## Sessões

O MVP utilizará sessão controlada pelo servidor.

A sessão deverá:

- possuir identificador imprevisível;
- ser renovada após o login;
- ser invalidada no logout;
- ser invalidada após redefinição de senha quando necessário;
- expirar após inatividade;
- não ser incluída em URLs;
- não ser registrada nos logs.

Configuração inicial sugerida:

```text
Tempo de inatividade: 30 minutos
Duração máxima de uma sessão: 8 horas
```

A duração poderá ser ajustada conforme a experiência de uso e o risco.

Operações administrativas críticas poderão exigir nova autenticação.

Referência: [OWASP — Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).

## Cookies

Cookies de sessão em produção deverão utilizar:

```text
HttpOnly
Secure
SameSite=Lax
```

Regras:

- `Secure` será obrigatório com HTTPS;
- `HttpOnly` impedirá acesso comum pelo JavaScript;
- o domínio e o caminho serão os mais restritos possíveis;
- dados sensíveis não serão gravados diretamente em cookies;
- a identificação da empresa nunca será aceita apenas por um cookie não validado.

## HTTPS

Produção utilizará somente HTTPS.

O sistema deverá:

- redirecionar HTTP para HTTPS;
- utilizar certificado válido;
- proteger login e todas as páginas autenticadas;
- habilitar HSTS quando o domínio e a infraestrutura estiverem estabilizados;
- nunca transmitir senha, token ou sessão por conexão aberta.

## CSRF

A proteção CSRF do Spring Security permanecerá habilitada.

Formulários Thymeleaf deverão incluir o token CSRF.

A proteção deverá abranger operações como:

```text
cadastrar
editar
desativar
excluir
convidar
alterar perfil
trocar plano
ativar módulo
enviar arquivo
```

Não desativar CSRF globalmente para resolver erros de formulário.

### Implementação atual (fase de fundação)

CSRF permanece habilitado globalmente. As únicas exceções registradas são `POST /api/auth/login` e `POST /api/convites/{token}/aceitar`, ambas pelo mesmo motivo: o cliente ainda não possui sessão/cookie desta aplicação antes dessa chamada. Nenhuma outra rota deverá ser adicionada a essa lista sem uma decisão explícita registrada em `docs/DECISOES.md`.

No navegador, o token é exposto via meta tags (`<meta name="_csrf">` / `<meta name="_csrf_header">`), renderizadas a partir do atributo de requisição `_csrf` do Thymeleaf em toda página autenticada. `criati-api.js` lê o nome do header a partir da própria meta tag (nunca hardcoded) e o anexa automaticamente em toda requisição `POST`/`PUT`/`PATCH`/`DELETE` feita via `fetch`. Requisições `GET` não enviam o header, como já previsto nesta seção.

Referência: [OWASP — CSRF Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).

## XSS e conteúdo HTML

Dados informados por usuários deverão ser escapados na saída.

Regras:

- utilizar a saída escapada padrão do Thymeleaf;
- evitar `th:utext`;
- não montar HTML diretamente com dados não confiáveis;
- validar campos;
- limitar rich text;
- sanitizar HTML quando sua utilização for realmente necessária;
- evitar JavaScript inline quando possível.

### Implementação atual (fase de fundação)

O JavaScript da interface (`criati-contexto.js`, `criati-ui.js`) usa exclusivamente `textContent` para inserir dados vindos da API (nome do usuário, nome da empresa, mensagens de toast); nenhum `innerHTML` recebe conteúdo não confiável. Os únicos usos de `innerHTML` limpam containers (`container.innerHTML = ""`) antes de montar elementos via `document.createElement`, nunca concatenam string com dado de usuário.

Uma Content Security Policy deverá ser planejada para produção.

## SQL Injection

Consultas deverão utilizar:

- Spring Data JPA;
- parâmetros;
- queries parametrizadas;
- critérios controlados.

Nunca concatenar entrada do usuário em SQL.

Exemplo proibido:

```java
"SELECT * FROM usuario WHERE email = '" + email + "'"
```

Mesmo filtros de ordenação e nomes de campos deverão usar listas permitidas.

## Isolamento multiempresa

O contexto da empresa será definido no backend após autenticação e validação do vínculo.

Nunca confiar diretamente em:

```text
empresa_id enviado em formulário
empresa_id na URL
empresa_id em cabeçalho criado pelo navegador
campo oculto
parâmetro JavaScript
```

Toda busca empresarial deverá combinar:

```text
ID do recurso + empresa atual
```

Exemplo seguro:

```java
findByIdAndEmpresaId(recursoId, empresaAtualId)
```

Exemplo proibido:

```java
findById(recursoId)
```

A tentativa de acessar um UUID pertencente a outra empresa não deverá revelar se o registro existe.

A implementação seguirá as orientações da [OWASP para aplicações multiempresa](https://cheatsheetseries.owasp.org/cheatsheets/Multi_Tenant_Security_Cheat_Sheet.html).

## Contexto da empresa

O contexto será estabelecido no início da requisição.

Deverá conter:

```text
usuario_id
empresa_id
usuario_empresa_id
unidades autorizadas
perfis
permissões
módulos ativos
```

Regras:

- contexto obrigatório para operações empresariais;
- contexto limpo ao final da requisição;
- troca de empresa exige validação;
- empresa suspensa respeita as regras de acesso definidas;
- tarefas assíncronas deverão receber contexto explicitamente;
- nenhuma thread poderá reutilizar contexto de outra requisição.

## Autorização

A aplicação utilizará o princípio de negar por padrão.

Cada operação deverá verificar:

1. usuário autenticado;
2. vínculo ativo;
3. empresa atual;
4. unidade autorizada;
5. módulo ativo;
6. permissão exigida;
7. propriedade do recurso;
8. estado permitido para a operação.

Ocultar um botão não substitui a autorização no backend.

Referência: [OWASP — Authorization](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html).

### Implementação atual (fase de fundação)

- Contexto (`usuario_id`, `empresa_id`, `usuario_empresa_id`, perfil) implementado via sessão HTTP e revalidado a cada uso; unidades autorizadas, permissões granulares e módulos ativos ainda não existem.
- `POST /api/usuarios-empresas` já verifica perfil ADMINISTRADOR na empresa ativa e rejeita `empresaId` divergente do contexto.
- `POST /api/empresas`, `POST /api/usuarios` e todas as rotas `/api/admin/**` exigem a autoridade global `ROLE_SUPERADMIN` (ver seção "Superadministrador"); qualquer outro usuário autenticado recebe 403 genérico e um anônimo recebe 401.
- Toda negação por contexto/perfil ausente ou inválido responde 403 genérico ("Acesso negado"), sem distinguir o motivo — cobre os casos "vínculo inexistente", "vínculo inativo", "empresa inativa" e "empresa inexistente" com a mesma resposta.
- `GET/PATCH/POST/DELETE /api/contexto/usuarios/**` (gestão de integrantes da empresa ativa) seguem o mesmo padrão: exigem ADMINISTRADOR na empresa ativa, buscam sempre por `id` + `empresaId` do contexto (nunca por `id` isolado) e respondem 403 genérico tanto para vínculo inexistente quanto para vínculo de outra empresa. Ver `docs/DECISOES.md`, seção "Gestão de acessos por empresa".
- Catálogo de aplicações (Financeiro/Clínica): `GET /api/admin/aplicacoes` e `/api/admin/empresas/{id}/aplicacoes/**` exigem `ROLE_SUPERADMIN` (mesmo padrão de `/api/admin/**`); as operações que alteram estado (`habilitar`/`desabilitar`) exigem CSRF, como qualquer operação mutável. `GET /api/contexto/aplicacoes` exige apenas usuário autenticado com contexto de empresa ativa válido, e nunca recebe `empresaId` do cliente — a empresa vem exclusivamente da sessão, filtrando por `empresa_id` + status do vínculo antes de retornar qualquer aplicação (nenhuma consulta busca aplicação apenas pelo seu identificador). Não existe `ROLE_FINANCEIRO`/`ROLE_CLINICA`: o acesso a uma aplicação é decidido consultando o vínculo `EmpresaAplicacao`, não uma authority do Spring Security. As páginas `GET /app/financeiro` e `GET /app/clinica` revalidam essa mesma regra no backend (`PaginaController`, nunca apenas ocultando o link no menu); sem contexto ativo ou sem a aplicação habilitada, ambas respondem com o mesmo redirecionamento genérico para `/app/aplicacoes`, sem revelar qual condição falhou nem dados de outra empresa. Detalhes completos em `docs/DECISOES.md`, seção "Catálogo de aplicações e Clínica Vida Demo".
- Módulo Financeiro (`/api/contexto/financeiro/**` e `/app/financeiro/**`): toda rota exige, além do contexto de empresa ativa, que `FINANCEIRO` esteja habilitada para a empresa (`ContextoFinanceiroService.exigirAcesso`), verificado centralizadamente no backend a cada requisição — nunca apenas pela ausência do link no menu. Contas, categorias e lançamentos são sempre buscados por `id` + `empresa_id` do contexto (nunca por `id` isolado); um identificador de outra empresa responde `404` genérico, idêntico ao de um identificador inexistente. Escrita estrutural (contas/categorias) restrita a `ADMINISTRADOR`; lançamentos aceitam `ADMINISTRADOR`/`GESTOR` (exceto reabrir um pago, só `ADMINISTRADOR`); `USUARIO` tem acesso somente leitura — sem nenhuma role nova do Spring Security, reaproveitando `PerfilUsuario` já existente. Valores monetários usam `BigDecimal` em toda a pilha, nunca `double`/`float`. Detalhes completos em `docs/FINANCEIRO.md`.
- Telas de Usuários e Convites (`/app/usuarios`, `/app/convites`): exigem `ADMINISTRADOR` na empresa ativa, checado em `PaginaController` (`ContextoEmpresaService.exigirContextoAtivo` + comparação de perfil) porque `ADMINISTRADOR`/`GESTOR`/`USUARIO` são perfis por empresa, não authorities do Spring Security — não há como declarar essa restrição via `hasAuthority(...)` em `SecurityConfig`. A falha (sem contexto ou perfil insuficiente) sempre lança `AcessoNegadoException`, tratada pelo `GlobalExceptionHandler` com a mesma resposta genérica `403` já usada pela API (`{"message": "Acesso negado"}`), mesmo sendo uma rota de página. Um Superadministrador sem vínculo empresarial não acessa essas páginas implicitamente. As telas consomem exclusivamente `/api/contexto/usuarios/**` e `/api/contexto/convites/**` já existentes — nenhum endpoint novo, nenhum `empresaId` enviado pelo cliente, nenhuma regra de autorização duplicada no frontend (os botões desabilitados sobre o próprio vínculo são só uma facilidade visual; o backend valida tudo de novo a cada chamada). O token bruto de um convite (quando o backend o retorna, hoje só em `local`/`test`) fica apenas numa variável em memória de `criati-convites.js`, nunca em `localStorage`/`sessionStorage`, nunca logado no console, descartado ao fechar o modal de sucesso. Detalhes completos em `docs/DECISOES.md`, seção "Interface de Usuários e Convites".

## Superadministrador

O Superadministrador da Criati será separado dos perfis empresariais.

Regras:

- não utilizar conta de banco como conta de login;
- não conceder acesso operacional silencioso;
- registrar operações administrativas;
- exigir justificativa para ações críticas;
- proteger alteração de módulos e assinaturas;
- considerar reautenticação para operações de alto impacto;
- preparar MFA para contas administrativas em uma fase posterior prioritária.

### Implementação atual (fase de fundação)

- Papel representado por `Usuario.superAdministrador` (booleano), concedendo a autoridade `ROLE_SUPERADMIN`; não é um vínculo `UsuarioEmpresa` e não pertence a nenhuma empresa.
- O primeiro Superadministrador é criado somente por um bootstrap de inicialização (`SuperAdministradorBootstrapService`), lido de variáveis de ambiente (`CRIATI_BOOTSTRAP_NOME`, `CRIATI_BOOTSTRAP_EMAIL`, `CRIATI_BOOTSTRAP_PASSWORD`); não existe endpoint público para essa criação, não existe senha padrão, e a rotina se autodesabilita quando já existe um Superadministrador cadastrado.
- E-mail de bootstrap já em uso: a rotina nunca promove um usuário existente nem cria duplicado; ela apenas encerra sem criar nada e registra um aviso, exigindo correção manual da configuração. Ver `docs/DECISOES.MD`.
- Risco operacional no primeiro bootstrap: iniciar múltiplas instâncias simultaneamente antes de existir qualquer Superadministrador pode causar falha transitória de inicialização em uma das instâncias (a unicidade de e-mail impede duplicidade de dados, mas não evita a corrida). Recomendação: o primeiro deploy deve usar uma única réplica ou subida gradual; a janela de risco desaparece após o primeiro Superadministrador ser criado. Ver `docs/DECISOES.MD`.
- Reautenticação para operações críticas, MFA e auditoria persistida de ações administrativas seguem como prioridades futuras (fora desta fase).

## Suporte temporário

O modo de suporte será implementado depois da base do MVP.

Ele deverá:

- exigir operador autorizado;
- exigir empresa selecionada;
- registrar motivo;
- possuir tempo limitado;
- mostrar aviso permanente;
- registrar início e encerramento;
- auditar ações;
- não usar a senha do cliente;
- não aceitar troca arbitrária de `empresa_id`;
- encerrar automaticamente ao expirar.

## Convites

Convites deverão utilizar tokens:

- aleatórios;
- imprevisíveis;
- de uso único;
- com validade de 24 horas;
- canceláveis;
- armazenados de forma protegida;
- inutilizados após uso.

O convite estará vinculado a:

```text
e-mail
empresa
perfil inicial
unidades autorizadas
responsável pelo convite
data de expiração
```

A aceitação deverá verificar novamente todos esses elementos.

### Implementação atual (fase de fundação)

- Token: `SecureRandom` de 256 bits, Base64 URL-safe; apenas o hash SHA-256 é persistido (`token_hash`, único), nunca o token bruto.
- Validade padrão: 72 horas (configurável por `CRIATI_CONVITE_EXPIRACAO_HORAS`; o valor de 24 horas mencionado acima era apenas um exemplo inicial, substituído por um padrão configurável).
- Uso único, cancelável (revogação) e reaproveitamento de token utilizado/revogado/expirado sempre rejeitado.
- Vinculado a e-mail, empresa e perfil; unidades autorizadas ainda não existem nesta fase (fora do escopo, ver `docs/MODELO_MULTIEMPRESA.MD`); o responsável pelo convite é registrado (`criado_por_usuario_id`).
- A aceitação revalida tudo no momento do uso: convite `PENDENTE`, não expirado, empresa `ATIVA`, e-mail sem `Usuario` prévio (ver `docs/DECISOES.md`, seção "Convites", para o tratamento de e-mail já cadastrado).
- Nenhum rate limiting implementado ainda; risco registrado em `docs/DECISOES.md`.
## Recuperação de senha

A recuperação deverá:

- apresentar resposta genérica;
- não confirmar se o e-mail existe;
- utilizar token temporário;
- permitir somente um uso;
- invalidar token após redefinição;
- invalidar tokens anteriores quando apropriado;
- registrar o evento;
- nunca enviar senha por e-mail.

Mensagem sugerida:

```text
Se o endereço estiver cadastrado, enviaremos as instruções para redefinição da senha.
```

## Alteração de e-mail e senha

A alteração de senha deverá exigir:

- sessão ativa;
- senha atual, exceto no fluxo válido de recuperação;
- confirmação da nova senha;
- auditoria;
- invalidação de sessões quando necessário.

A alteração de e-mail deverá exigir validação do novo endereço e poderá exigir reautenticação.

## Upload de arquivos

Arquivos enviados são considerados não confiáveis.

A aplicação deverá:

- permitir apenas extensões necessárias;
- verificar extensão;
- verificar tipo de conteúdo;
- não confiar apenas no `Content-Type`;
- limitar tamanho;
- gerar nome interno aleatório;
- impedir caminhos fornecidos pelo usuário;
- armazenar fora da pasta pública;
- impedir execução;
- validar propriedade da empresa;
- proteger o download pelo backend;
- considerar antivírus ou análise adicional futuramente.

Extensões serão liberadas por módulo e por necessidade.

Não será permitido upload irrestrito de:

```text
.exe
.bat
.cmd
.ps1
.sh
.jar
.war
.html
.js
```

A lista final será uma allowlist, não apenas uma lista de bloqueio.

Referência: [OWASP — File Upload](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html).

## Downloads

Para baixar um arquivo, o backend deverá validar:

```text
usuário
empresa
vínculo
módulo
permissão
registro relacionado
propriedade do arquivo
```

O endereço físico do armazenamento não deverá conceder acesso público permanente.

## Validação de entrada

Toda entrada deverá possuir validação no backend.

Exemplos:

- tamanho mínimo e máximo;
- formato;
- valores permitidos;
- datas válidas;
- relacionamentos da mesma empresa;
- UUID válido;
- paginação limitada;
- ordenação permitida;
- arquivo permitido.

A validação do frontend melhora a experiência, mas não substitui o backend.

## Mass assignment

DTOs de entrada deverão conter somente campos permitidos.

Não receber entidades JPA diretamente em formulários.

Exemplo de risco:

```text
usuário envia manualmente:
- empresa_id;
- administrador = true;
- status = ATIVO;
- plano_id.
```

Esses valores deverão ser definidos ou validados pelo backend.

## Segredos e credenciais

São considerados segredos:

```text
senha do banco
senha SMTP
chaves de API
tokens
chaves de armazenamento
segredos de sessão
credenciais de produção
```

Segredos deverão ficar em:

- variáveis de ambiente;
- serviço de segredos da hospedagem;
- configuração local ignorada pelo Git.

Nunca incluir segredos em:

```text
application.properties versionado
README
prints
logs
mensagens de erro
commits
código-fonte
```

Referência: [OWASP — Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html).

## Logs

Logs deverão ajudar no diagnóstico sem expor informações sensíveis.

Registrar quando necessário:

- identificador da requisição;
- usuário, quando permitido;
- empresa;
- evento;
- resultado;
- horário;
- erro técnico controlado.

Não registrar:

- senhas;
- tokens completos;
- cookies;
- chaves de API;
- dados bancários desnecessários;
- conteúdo integral de arquivos;
- informações pessoais sem necessidade.

Referência: [OWASP — Logging](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html).

## Auditoria

Auditoria de negócio não é o mesmo que log técnico.

A auditoria deverá registrar ações como:

- cadastro de empresa;
- convite;
- alteração de perfil;
- alteração de permissão;
- ativação de módulo;
- troca de plano;
- suspensão;
- troca de empresa;
- suporte temporário;
- arquivamento;
- exportação sensível.

Registros de auditoria não poderão ser alterados por telas comuns.

## Tratamento de erros

Erros apresentados ao usuário deverão ser claros, mas não revelar:

- stack trace;
- SQL;
- nomes internos de tabelas;
- caminhos do servidor;
- credenciais;
- versões desnecessárias;
- existência de dados de outra empresa.

Erros técnicos completos ficarão restritos aos logs protegidos.

## Dependências

O projeto deverá:

- utilizar versões suportadas;
- evitar bibliotecas abandonadas;
- remover dependências desnecessárias;
- revisar vulnerabilidades;
- atualizar dependências com controle;
- executar testes após atualizações;
- manter o `pom.xml` organizado.

Atualizações de segurança não deverão ser adiadas sem avaliação.

## Banco de dados

O usuário do banco utilizado pela aplicação deverá possuir apenas as permissões necessárias.

A aplicação não deverá conectar utilizando superusuário do PostgreSQL.

Produção deverá utilizar:

```text
usuário exclusivo da aplicação
senha forte
conexão protegida
acesso de rede limitado
backup protegido
```

## Cache e tarefas assíncronas

Quando cache for adicionado, suas chaves deverão incluir o contexto da empresa.

Exemplo:

```text
empresa:{empresa_id}:tarefa:{tarefa_id}
```

Tarefas agendadas e assíncronas deverão receber explicitamente a empresa e validar seu estado antes da execução.

## E-mail

E-mails deverão:

- evitar conteúdo sensível desnecessário;
- utilizar links HTTPS;
- ter tokens com validade;
- não incluir senha;
- não revelar dados de outras empresas;
- possuir remetente autorizado;
- registrar apenas o necessário para diagnóstico.

## Backup

Backups deverão:

- ser automáticos;
- possuir retenção;
- ser protegidos;
- ter acesso restrito;
- ser restaurados em testes periódicos;
- nunca ficar expostos em pasta pública.

## Testes de segurança obrigatórios

Antes da conclusão do MVP, testar:

1. acesso sem autenticação;
2. acesso sem permissão;
3. acesso a UUID de outra empresa;
4. alteração manual de `empresa_id`;
5. usuário bloqueado;
6. vínculo inativo;
7. unidade não autorizada;
8. módulo desativado;
9. CSRF inválido;
10. upload proibido;
11. token expirado;
12. token reutilizado;
13. sessão após logout;
14. recuperação de senha;
15. tentativa de SQL injection;
16. tentativa de XSS;
17. exposição de stack trace;
18. segredos no repositório;
19. arquivos públicos;
20. isolamento de logs e auditoria.

## Prioridades posteriores ao MVP

- autenticação multifator;
- alertas de login suspeito;
- gestão centralizada de segredos;
- antivírus para uploads;
- rate limiting distribuído;
- política de retenção;
- testes automatizados de segurança;
- análise de dependências no pipeline;
- Content Security Policy mais restritiva;
- avaliação de Row-Level Security do PostgreSQL;
- revisão externa de segurança antes de maior escala.

## Regra final

Nenhuma conveniência de desenvolvimento justifica desativar silenciosamente uma proteção.

Quando uma medida de segurança impedir uma funcionalidade, a causa deverá ser compreendida e corrigida sem remover a proteção global.