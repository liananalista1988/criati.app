# Registro de Decisões — Criati

Este documento registra as decisões oficiais do projeto. Mudanças relevantes devem ser discutidas e documentadas antes da implementação.

## Produto

- A Criati será uma plataforma SaaS genérica, modular e multiempresa.
- A plataforma atenderá empresas de diferentes segmentos.
- O público inicial será formado principalmente por pequenos e médios negócios.
- A proposta é transformar processos manuais, planilhas e rotinas desorganizadas em soluções digitais.
- Cada empresa utilizará somente os módulos contratados e autorizados.

## Multiempresa

- Um usuário poderá acessar várias empresas com o mesmo login.
- O usuário poderá ter perfil e permissões diferentes em cada empresa.
- Cada empresa poderá possuir unidades ou filiais opcionais.
- Empresas sem filiais utilizarão uma Unidade Principal.
- O banco será compartilhado entre as empresas.
- Os dados empresariais serão isolados obrigatoriamente por `empresa_id`.
- Dados relacionados a unidades também utilizarão `unidade_id`.
- O isolamento será aplicado pelo backend e validado por testes automatizados.

## Cadastro de empresas e usuários

- No MVP, somente a equipe da Criati poderá cadastrar novas empresas.
- O primeiro administrador da empresa receberá um convite por e-mail.
- O convite terá token de uso único e validade limitada.
- A senha do usuário nunca será enviada por e-mail.
- Um convite poderá ser reenviado ou cancelado.
- Após criar a senha, o convite perderá a validade.

## Perfis e permissões

- A Criati fornecerá perfis padrão.
- Cada empresa poderá criar perfis personalizados.
- As permissões serão vinculadas ao usuário dentro de cada empresa.
- As permissões poderão incluir ações como visualizar, cadastrar, editar, excluir, exportar, aprovar e administrar.
- O administrador da empresa distribuirá os acessos aos módulos contratados.
- Somente a administração da Criati poderá ativar ou desativar módulos contratados.

## Planos e assinaturas

- A gestão de planos e assinaturas será manual no MVP.
- Pagamentos automáticos serão implementados em uma fase futura.
- Os planos poderão controlar módulos, usuários, unidades, armazenamento, IA e outros limites.
- Uma empresa suspensa não perderá seus dados.
- Os estados iniciais de empresa serão:
  - Em implantação;
  - Ativa;
  - Suspensa;
  - Cancelada.
- Os estados iniciais de assinatura serão:
  - Teste;
  - Ativa;
  - Atrasada;
  - Suspensa;
  - Cancelada.

## Suporte

- A Criati terá acesso interno de suporte.
- O acesso será temporário, autorizado e auditado.
- O suporte não utilizará a senha do cliente.
- A interface indicará claramente quando o modo de suporte estiver ativo.
- O sistema registrará operador, empresa, início, fim, motivo e ações realizadas.
- Essa funcionalidade será implementada em uma fase posterior do MVP.

## Arquitetura

- A aplicação utilizará uma arquitetura de monólito modular.
- O backend será desenvolvido com Spring Boot.
- As telas utilizarão Thymeleaf, HTML, CSS e JavaScript.
- A aplicação será responsiva para computador e celular.
- APIs poderão ser adicionadas para integrações e aplicativos futuros.
- Os módulos deverão permanecer organizados e independentes dentro da mesma aplicação.

## Banco de dados

- O banco principal será PostgreSQL.
- O banco local foi criado com o nome `criati_db`.
- O JPA/Hibernate será utilizado para mapear e acessar os dados.
- O Flyway será responsável pela criação e evolução da estrutura do banco.
- O Hibernate utilizará `ddl-auto=validate`.
- Scripts do Flyway já executados nunca deverão ser alterados.
- Toda evolução do banco deverá ser feita por um novo script versionado.
- As entidades principais utilizarão UUID.
- Códigos amigáveis poderão ser usados nas telas e documentos.

## Exclusão e histórico

- A operação normal utilizará desativação, cancelamento ou arquivamento.
- Registros importantes não serão excluídos definitivamente pelas telas comuns.
- Usuários desativados perderão o acesso, mas seu histórico permanecerá.
- Exclusões definitivas dependerão de processo administrativo específico.
- Uma política própria para dados pessoais será definida posteriormente.

## Datas e auditoria

- Instantes serão armazenados em UTC.
- Cada empresa poderá configurar seu fuso horário.
- O fuso inicial padrão será `America/Fortaleza`.
- Datas sem horário permanecerão como datas.
- Entidades relevantes deverão registrar:
  - criação;
  - responsável pela criação;
  - última atualização;
  - responsável pela atualização;
  - desativação, quando aplicável;
  - responsável pela desativação.

## Escopo do MVP

O MVP será dividido em duas etapas:

### Etapa 1 — Núcleo SaaS

- autenticação;
- recuperação de senha;
- empresas;
- unidades;
- usuários;
- convites;
- vínculos multiempresa;
- perfis;
- permissões;
- módulos;
- planos;
- assinaturas manuais;
- configurações;
- auditoria;
- painel administrativo da Criati;
- painel da empresa.

### Etapa 2 — Tarefas e Processos

- tarefas;
- responsáveis;
- participantes;
- prazos;
- prioridades;
- categorias;
- comentários;
- anexos;
- lista;
- Kanban;
- histórico;
- filtros;
- alertas;
- indicadores;
- tarefas recorrentes.

## Tarefas e processos

- Cada empresa poderá personalizar seus fluxos e status.
- A Criati fornecerá um fluxo padrão.
- Status poderão ser criados, ordenados, renomeados, coloridos e desativados.
- Status utilizados não serão apagados definitivamente.
- Tarefas recorrentes entrarão no MVP.
- Modelos completos de processos serão implementados depois do Kanban.

## Notificações

- O MVP terá notificações dentro do sistema.
- O MVP utilizará e-mail para convites, recuperação de senha e eventos importantes.
- Usuários poderão configurar preferências de notificação.
- Resumos poderão ser enviados para reduzir excesso de mensagens.
- Integração com WhatsApp será implementada futuramente.

## Arquivos

- Arquivos não serão armazenados diretamente no PostgreSQL.
- O banco armazenará apenas metadados e vínculos.
- Em desenvolvimento, os arquivos poderão ficar em armazenamento local protegido.
- Em produção, será utilizado armazenamento de arquivos em nuvem.
- O backend controlará a autorização de upload e download.
- Os arquivos serão separados logicamente por empresa.
- Planos poderão definir limites de armazenamento.

## Autenticação

- A autenticação do MVP utilizará API REST com sessão HTTP controlada pelo servidor (cookie de sessão), sem JWT e sem OAuth2/OIDC nesta fase.
- A senha é recebida em texto puro apenas no DTO de entrada de cadastro e de login; o hash é sempre gerado pelo backend, nunca aceito pronto do cliente.
- O algoritmo de hashing é BCrypt (fator de trabalho 12): Argon2id foi avaliado primeiro, conforme preferência de `docs/SEGURANCA.MD`, mas exigiria adicionar a biblioteca externa BouncyCastle (ausente do projeto), incompatibilidade comprovada em execução (`NoClassDefFoundError`). BCrypt é o fallback previsto no próprio `docs/SEGURANCA.MD` para essa situação.
- O acesso é negado por padrão; somente o endpoint de login é público.
- O login troca o ID da sessão após autenticação bem-sucedida (`ChangeSessionIdAuthenticationStrategy`), prevenindo session fixation; o e-mail é normalizado (trim + minúsculas) tanto no cadastro quanto no login.
- CSRF permanece habilitado globalmente. A única exceção é `POST /api/auth/login`, porque o cliente ainda não possui um token CSRF antes do primeiro login; logout e as demais operações que alteram estado continuam exigindo o token. Nenhuma outra rota deverá ser adicionada à lista de exceções sem uma decisão arquitetural explícita registrada aqui.
- Superadministrador da Criati fica fora desta fase e será tratado em decisão futura; contexto de empresa ativa e autorização inicial por perfil (ADMINISTRADOR/GESTOR/USUARIO) já estão implementados (ver seção "Contexto multiempresa e autorização inicial").

## Contexto multiempresa e autorização inicial

- A empresa ativa é armazenada na sessão HTTP somente como identificadores (`CONTEXTO_EMPRESA_ID`, `CONTEXTO_USUARIO_EMPRESA_ID`); nenhuma entidade JPA é guardada na sessão.
- Toda leitura do contexto revalida o vínculo (`UsuarioEmpresa`) e a empresa no banco a cada uso; um vínculo ou empresa desativados após a seleção invalidam o contexto no próximo uso, sem exigir novo login.
- O perfil do usuário na empresa vem exclusivamente do vínculo (`UsuarioEmpresa.perfil`), nunca de dado enviado pelo cliente.
- Selecionar uma empresa exige vínculo ATIVO e empresa ATIVA; qualquer outra condição (sem vínculo, vínculo inativo, empresa inativa ou inexistente) responde com a mesma mensagem genérica 403 ("Acesso negado"), sem revelar qual delas se aplica.
- `POST /api/empresas` e `POST /api/usuarios` ficam bloqueados para qualquer usuário autenticado nesta fase: ainda não existe Superadministrador da Criati nem fluxo de convite, então não há papel legítimo para chamar esses endpoints. Isso significa que hoje não existe caminho via API para cadastrar a primeira empresa ou usuário — lacuna de bootstrap conhecida, a ser resolvida quando o Superadministrador for implementado.
- `POST /api/usuarios-empresas` exige empresa ativa selecionada e perfil ADMINISTRADOR nessa empresa; o `empresaId` do corpo da requisição é validado contra o contexto da sessão e a operação sempre usa a empresa do contexto, nunca o valor bruto do cliente.
- Unidades, permissões granulares, módulos, planos, assinaturas, convites e Superadministrador permanecem fora do escopo desta fase.

## Regra de alteração

Nenhuma decisão estrutural registrada neste documento deverá ser alterada silenciosamente.

Caso uma mudança seja necessária:

1. explicar o motivo;
2. avaliar os impactos;
3. obter aprovação;
4. atualizar este documento;
5. somente depois implementar a alteração.