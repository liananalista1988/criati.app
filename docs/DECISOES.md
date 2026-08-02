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

- No MVP, somente o Superadministrador pode cadastrar novas empresas, e faz isso já com o primeiro administrador em uma única operação atômica (`POST /api/admin/empresas`) — o primeiro administrador não é convidado, é criado diretamente por quem tem autoridade para criar a empresa. Ver seção "Superadministrador e administração inicial".
- Os demais usuários de uma empresa (GESTOR, USUARIO ou outro ADMINISTRADOR) são adicionados exclusivamente por convite, criado por um ADMINISTRADOR da própria empresa. Ver seção "Convites".
- O convite tem token de uso único e validade limitada.
- A senha do usuário nunca é enviada por e-mail; é definida pelo próprio convidado ao aceitar.
- Um convite pendente pode ser revogado; convidar novamente o mesmo e-mail revoga automaticamente o convite pendente anterior e cria um novo.
- Após aceito, o convite perde a validade (status UTILIZADO), de uso único.

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
- Superadministrador da Criati (papel global, distinto dos perfis por empresa) está implementado; ver seção "Superadministrador e administração inicial". Contexto de empresa ativa e autorização inicial por perfil (ADMINISTRADOR/GESTOR/USUARIO) já estavam implementados (ver seção "Contexto multiempresa e autorização inicial").

## Contexto multiempresa e autorização inicial

- A empresa ativa é armazenada na sessão HTTP somente como identificadores (`CONTEXTO_EMPRESA_ID`, `CONTEXTO_USUARIO_EMPRESA_ID`); nenhuma entidade JPA é guardada na sessão.
- Toda leitura do contexto revalida o vínculo (`UsuarioEmpresa`) e a empresa no banco a cada uso; um vínculo ou empresa desativados após a seleção invalidam o contexto no próximo uso, sem exigir novo login.
- O perfil do usuário na empresa vem exclusivamente do vínculo (`UsuarioEmpresa.perfil`), nunca de dado enviado pelo cliente.
- Selecionar uma empresa exige vínculo ATIVO e empresa ATIVA; qualquer outra condição (sem vínculo, vínculo inativo, empresa inativa ou inexistente) responde com a mesma mensagem genérica 403 ("Acesso negado"), sem revelar qual delas se aplica.
- `POST /api/empresas` e `POST /api/usuarios` agora exigem papel Superadministrador (ver seção seguinte); deixaram de estar bloqueados para todo mundo, já que existe um papel legítimo para chamá-los.
- `POST /api/usuarios-empresas` exige empresa ativa selecionada e perfil ADMINISTRADOR nessa empresa; o `empresaId` do corpo da requisição é validado contra o contexto da sessão e a operação sempre usa a empresa do contexto, nunca o valor bruto do cliente.
- Unidades, permissões granulares, módulos, planos e assinaturas permanecem fora do escopo desta fase. Convites estão implementados (ver seção "Convites").

## Superadministrador e administração inicial

- Superadministrador é um papel global da plataforma, representado por um atributo booleano (`Usuario.superAdministrador`) na própria entidade `Usuario`, não por um vínculo em `UsuarioEmpresa`. Um Superadministrador não pertence a nenhuma empresa; ele é ortogonal ao modelo multiempresa (ver `docs/MODELO_MULTIEMPRESA.MD`). Alternativa descartada: uma "empresa especial" ou um perfil adicional dentro de `UsuarioEmpresa` — rejeitada porque misturaria um papel de plataforma com o modelo de vínculo empresa-usuário, que é sempre escopado a uma empresa concreta.
- Autorização: rotas globais (`POST /api/empresas`, `POST /api/usuarios`, `/api/admin/**`) exigem a autoridade `ROLE_SUPERADMIN`, concedida somente quando `Usuario.superAdministrador = true`, aplicada declarativamente em `SecurityConfig` (`hasAuthority(...)`), e não por checagem ad-hoc no controller.
- Bootstrap do primeiro Superadministrador: implementado como um `ApplicationRunner` (`SuperAdministradorBootstrapService`/`SuperAdministradorBootstrapRunner`), executado uma única vez na inicialização da aplicação, lendo nome, e-mail e senha de variáveis de ambiente (`CRIATI_BOOTSTRAP_NOME`, `CRIATI_BOOTSTRAP_EMAIL`, `CRIATI_BOOTSTRAP_PASSWORD`). Alternativas avaliadas e rejeitadas:
  - Endpoint público de bootstrap: rejeitado explicitamente — exporia criação de conta com privilégio máximo sem autenticação prévia, superfície de ataque inaceitável.
  - `CommandLineRunner`: equivalente em efeito a `ApplicationRunner`, mas `ApplicationRunner` recebe `ApplicationArguments` já tipados e é a opção recomendada pelo próprio Spring Boot para lógica de inicialização; não há vantagem prática do `CommandLineRunner` aqui.
  - Script SQL manual: rejeitado por exigir hashing de senha fora do processo da aplicação (fora do `PasswordEncoder` central) e por não ser idempotente nem auditável do mesmo jeito que código versionado.
- O bootstrap é idempotente e autodesabilitante: se já existir qualquer Superadministrador (`existsBySuperAdministradorTrue()`), a rotina não faz nada, mesmo que as variáveis de ambiente continuem configuradas. Isso evita a criação de múltiplos Superadministradores por reinício acidental e limita a janela de uso das credenciais de bootstrap.
- Não existe senha padrão: se nome, e-mail ou senha estiverem ausentes ou vazios, a rotina não cria nada e apenas registra um log informativo (sem citar valores). Nenhuma credencial (senha ou hash) é impressa em log em nenhum caminho.
- Migration: a coluna `usuario.super_administrador` (`BOOLEAN NOT NULL DEFAULT FALSE`) foi adicionada por `V2__adiciona_super_administrador_usuario.sql`; usuários existentes permanecem comuns, nenhuma promoção automática ocorre na migration.
- Bootstrap com e-mail já cadastrado: se o e-mail configurado em `CRIATI_BOOTSTRAP_EMAIL` já pertencer a um usuário existente (comum ou não), a rotina não promove esse usuário e não cria um duplicado — a checagem de e-mail ocorre antes de qualquer escrita. O bootstrap termina sem criar Superadministrador, registra um aviso no log (sem expor o valor do e-mail) e a aplicação continua iniciando normalmente. Nesse caso, a configuração (`CRIATI_BOOTSTRAP_EMAIL`) deve ser corrigida manualmente para um e-mail ainda não utilizado, ou o primeiro Superadministrador deve ser promovido por um processo administrativo direto no banco.
- Corrida entre instâncias no primeiro bootstrap: se múltiplas instâncias iniciarem simultaneamente antes de existir qualquer Superadministrador, há uma janela entre a checagem de e-mail e a gravação em que mais de uma instância pode tentar criar o mesmo Superadministrador. A restrição `UNIQUE` em `usuario.email` impede duplicidade de dados, mas a instância que perder a corrida falha de forma transitória na inicialização (erro de violação de unicidade não tratado no `ApplicationRunner`). Por isso, o primeiro deploy (antes de existir um Superadministrador) deve usar uma única réplica ou uma estratégia de subida gradual; após a criação do primeiro Superadministrador, o bootstrap fica permanentemente inativo (idempotência) e essa janela deixa de existir.
- `POST /api/admin/empresas` (empresa + primeiro administrador em uma única operação transacional) não é redundante com os endpoints já existentes: `POST /api/usuarios-empresas` (vincular um usuário a uma empresa) exige um contexto de empresa ativa já selecionado, que por sua vez exige um vínculo ATIVO pré-existente — impossível para uma empresa recém-criada, que ainda não tem nenhum vínculo. Sem um endpoint dedicado, não haveria caminho via API para uma empresa nova sair do zero sem intervenção manual no banco.
- `GET /api/admin/me` e `GET /api/admin/empresas` são endpoints administrativos mínimos (identidade do Superadministrador logado e listagem simples de empresas), propositalmente sem CRUD completo, edição ou exclusão de empresa — fora do escopo desta fase.
- Seguem fora do escopo: recuperação de senha, permissões granulares, unidades, módulos, planos, assinatura, cobrança, suspensão por inadimplência, auditoria persistida, telas Thymeleaf, JWT/OAuth2, exclusão e edição completa de empresa.

## Convites

- Convite é uma entidade própria (`Convite`, tabela `convite`), não reaproveita `StatusCadastro`: usa um enum dedicado `StatusConvite` (`PENDENTE`, `UTILIZADO`, `EXPIRADO`, `REVOGADO`) porque o ciclo de vida de um convite (pendente → utilizado/expirado/revogado) é conceitualmente diferente de ativo/inativo.
- Token: gerado com `SecureRandom` (256 bits, nunca um UUID simples), codificado em Base64 URL-safe sem padding. Apenas o hash SHA-256 (`token_hash`, coluna `UNIQUE`) é persistido; o token bruto nunca é gravado em nenhuma tabela. SHA-256 foi escolhido em vez de BCrypt para o hash do token porque o token já nasce com alta entropia (256 bits) — BCrypt é necessário para senhas de usuário (entropia baixa, precisa de custo computacional alto contra força bruta), não para um token já aleatório, onde um hash rápido e determinístico é suficiente e permite localizar o convite por igualdade indexada no banco.
- Expiração: configurável via `CRIATI_CONVITE_EXPIRACAO_HORAS` (padrão 72 horas, qualquer perfil pode sobrescrever por variável de ambiente). Tratada dinamicamente (`expiraEm < agora`): nenhum job de fundo marca convites como expirados; a checagem acontece a cada leitura (validação pública, aceitação, listagem para exibição). Isso simplifica a operação sem exigir agendador nem tarefa periódica.
- Uso único: aceitar marca o convite como `UTILIZADO` (com `utilizadoEm`) na mesma transação que cria o usuário e o vínculo; qualquer tentativa posterior com o mesmo token encontra um convite que não está mais `PENDENTE` e é tratada como inválida.
- Política de duplicidade: convidar novamente o mesmo e-mail na mesma empresa revoga automaticamente o convite `PENDENTE` existente antes de criar o novo (equivalente a "reenviar convite"). Alternativas descartadas: rejeitar com 409 (adiciona fricção sem benefício de segurança) e reaproveitar o token antigo (exigiria re-expor um token já entregue, pior do que gerar um novo).
- Usuário já existente (Caso B): fora do escopo desta fase, por decisão explícita. Aceitar um convite cujo e-mail já possui `Usuario` é rejeitado (reaproveita `EmailJaCadastradoException`, HTTP 409) em vez de permitir redefinir a senha da conta existente — permitir isso sem uma confirmação de identidade adicional abriria uma brecha de takeover de conta (qualquer pessoa com acesso ao convite poderia assumir uma conta alheia). Apenas o Caso A (e-mail sem `Usuario` prévio) está implementado.
- Ausência de e-mail real: não há integração de e-mail nesta fase. A entrega é abstraída por `ConviteNotificador`, com uma implementação temporária (`ConviteNotificadorTemporario`) que apenas registra em log que um convite foi criado (id do convite e da empresa), nunca o token. Uma implementação real (e-mail) poderá substituir essa classe sem alterar `ConviteService`.
- Exposição do token bruto: nunca em log. Na resposta de criação (`POST /api/contexto/convites`), o token bruto só é incluído quando `criati.convite.expor-token-bruto=true` — verdadeiro por padrão em `local` e `test` (não há outro mecanismo de entrega nestes ambientes), falso em qualquer outro perfil (`homolog`, `prod`), preparando o terreno para quando existir entrega por e-mail real.
- Endpoints públicos: apenas `GET /api/convites/{token}` (validação) e `POST /api/convites/{token}/aceitar` (aceitação). `GET` sempre responde `200` com `{"valido": false}` para qualquer motivo de invalidez (inexistente, expirado, utilizado, revogado ou empresa inativa) — nunca `404`/`410` — para não distinguir o motivo e não permitir enumeração. A aceitação inválida responde `404` genérico ("Convite invalido ou expirado"), pela mesma razão.
- CSRF: `POST /api/convites/{token}/aceitar` é isento de CSRF, registrado explicitamente em `SecurityConfig`, pelo mesmo motivo já documentado para `/api/auth/login` — o cliente ainda não tem sessão/cookie desta aplicação antes da chamada. CSRF continua habilitado globalmente para todas as demais rotas, incluindo as demais operações de convite (criação, listagem, revogação).
- Aceitar convite nunca autentica automaticamente: nenhuma sessão é criada; o convidado faz login normalmente depois pelo fluxo já existente.
- Rate limiting: não implementado (exigiria dependência nova, não autorizada nesta tarefa). Risco registrado: os endpoints públicos (`GET /api/convites/{token}` e `POST /api/convites/{token}/aceitar`) não têm limitação de tentativas; um atacante poderia tentar adivinhar tokens por força bruta (mitigado pela alta entropia do token, 256 bits) ou golpear a rota de aceitação. Prioridade futura, junto com CAPTCHA e limitação por IP/conta já previstos em `docs/SEGURANCA.MD`.
- Convites revogados: um convite revogado nunca pode ser aceito (mesma checagem de "efetivamente válido" usada para expiração/uso). Apenas convites `PENDENTE` podem ser revogados; revogar um convite `UTILIZADO`, `EXPIRADO` ou já `REVOGADO` é rejeitado com `400`.
- Política de senha: reaproveita o mínimo já documentado em `docs/SEGURANCA.MD` ("mínimo de 15 caracteres enquanto não houver MFA"), centralizada em `SenhaValidador` (`br.app.criati.shared.validacao`) para reaproveitamento futuro em redefinição de senha. Sem exigência de classes de caractere obrigatórias (maiúscula/número/símbolo) — política de comprimento e frase-senha, não de complexidade forçada. Máximo de 72 caracteres (limite prático de entrada para BCrypt).
- Endpoint global do Superadministrador (`POST /api/admin/empresas/{empresaId}/convites`) considerado e **não implementado**: redundante para o fluxo atual — o Superadministrador já cria a empresa com seu primeiro administrador via `POST /api/admin/empresas`; esse administrador então convida os demais usuários pelo fluxo empresarial. Um endpoint global duplicaria a mesma capacidade sem necessidade concreta imediata, ampliando a superfície de rotas globais sem justificativa de uso.
- Futuras integrações de e-mail: quando implementadas, devem substituir apenas `ConviteNotificador`/`ConviteNotificadorTemporario`; nenhuma outra classe deste fluxo precisa mudar.

## Gestão de acessos por empresa

- Endpoints sob `/api/contexto/usuarios` (todos exigem empresa ativa selecionada e perfil ADMINISTRADOR nessa empresa, igual ao padrão já usado por `/api/contexto/convites`): `GET` (listar integrantes, com filtros opcionais `status`, `perfil` e `busca` por nome/e-mail), `GET /{usuarioEmpresaId}` (consultar), `PATCH /{usuarioEmpresaId}/perfil` (alterar perfil empresarial), `POST /{usuarioEmpresaId}/suspender`, `POST /{usuarioEmpresaId}/reativar` e `DELETE /{usuarioEmpresaId}` (remoção lógica). Todos operam exclusivamente sobre `UsuarioEmpresa` (o vínculo), nunca sobre `Usuario` (a entidade global): não apagam `Usuario`, não alteram senha, e-mail ou o papel de Superadministrador, e não afetam vínculos do mesmo usuário com outras empresas.
- Remoção lógica sem migration: `StatusCadastro` (`ATIVO`/`INATIVO`) já é suficiente para representar tanto a suspensão quanto a remoção lógica de um vínculo — ambas as operações resultam no mesmo status `INATIVO` (`UsuarioEmpresa.suspender()` e `UsuarioEmpresa.removerLogicamente()` fazem exatamente a mesma alteração de estado). Alternativa descartada: criar um novo enum ou um novo campo (`REVOGADO` distinto de `SUSPENSO`) — rejeitada porque o comportamento observável já pedido nesta fase (impedir seleção da empresa, permitir reativação, preservar o `Usuario` e os demais vínculos) é idêntico para os dois motivos, e distinguir o motivo da inatividade não tem nenhum consumidor nesta fase. Se um consumidor futuro precisar distinguir "suspenso por decisão administrativa" de "removido", uma migration dedicada deverá ser avaliada então.
- Idempotência: nenhuma das operações de transição de estado (`suspender`, `reativar`, remoção lógica via `DELETE`) é idempotente — repetir a mesma operação sobre um vínculo que já está no estado alvo responde `409 Conflict` (`VinculoStatusInvalidoException`, mensagens "Vinculo ja esta inativo"/"Vinculo ja esta ativo"), em vez de um `204`/`200` silencioso. Alternativa descartada: idempotência silenciosa (repetir sem erro) — rejeitada por consistência com o precedente já existente em `ConviteService.revogar` (revogar um convite não `PENDENTE` já respondia com erro, não silenciosamente); tornar essas operações idempotentes esconderia do administrador que a premissa da sua ação ("este vínculo está ativo") já não era mais verdadeira.
- Alteração de perfil: apenas ADMINISTRADOR, GESTOR e USUARIO (o enum `PerfilUsuario`, que não contém nenhum valor de papel global) podem ser atribuídos; não há como esse endpoint conceder `ROLE_SUPERADMIN` porque esse papel nunca fez parte de `PerfilUsuario` (é um campo booleano separado em `Usuario`, ver seção "Superadministrador e administração inicial"). O vínculo alvo precisa estar `ATIVO` para ter o perfil alterado.
- Proteção do último Administrador ativo: antes de rebaixar (`alterarPerfil` saindo de `ADMINISTRADOR`), suspender ou remover um vínculo que atualmente é `ADMINISTRADOR` e `ATIVO`, o serviço conta quantos vínculos `ADMINISTRADOR`/`ATIVO` a empresa ainda tem (`UsuarioEmpresaRepository.countByEmpresaIdAndPerfilAndStatus`); se a contagem for `<= 1`, a operação é rejeitada com `409` (`UltimoAdministradorAtivoException`). A contagem e a escrita ocorrem na mesma transação (`@Transactional` padrão do método de serviço, sem `REQUIRES_NEW`), mas sem lock pessimista.
  - Risco residual de concorrência (documentado, não mitigado nesta fase): como a API já exige que o chamador seja, ele mesmo, um Administrador `ATIVO` da empresa (`exigirContextoAtivo` + checagem de perfil), na prática essa proteção só é alcançável via autoalteração — que já é bloqueada antes, por uma regra mais específica (ver abaixo) — ou por uma corrida real entre duas requisições concorrentes envolvendo Administradores diferentes (ex.: dois Administradores tentando rebaixar um ao outro no mesmo instante). Sem lock pessimista, essa corrida específica poderia, em teoria, deixar a empresa sem nenhum Administrador ativo. Avaliado e não implementado: `@Lock(PESSIMISTIC_WRITE)` na contagem — não adotado nesta fase por não haver precedente de uso de locks pessimistas no restante do projeto e por essa corrida já ser do mesmo tipo (mesma severidade, mesma ausência de mitigação) do risco já aceito e documentado em "Corrida entre instâncias no primeiro bootstrap". A cobertura de teste desta regra usa chamada direta ao serviço (`GerenciarUsuarioEmpresaServiceTests`), que consegue simular o estado "só resta um Administrador ativo" sem depender da restrição de autorização da API.
- Proteção contra autoalteração: um Administrador não pode, sobre o próprio vínculo (`usuarioEmpresaId` igual ao do contexto da sessão), rebaixar o próprio perfil, suspender-se ou remover-se (`409`, `AutoAlteracaoNaoPermitidaException`). Pode, porém, consultar o próprio vínculo (`GET`) e ser alterado por outro Administrador. Essa regra é verificada antes da proteção do último Administrador e independe dela — vale mesmo quando a empresa tem vários Administradores ativos.
- Máscara de vínculo entre empresas: `GET/PATCH/POST/DELETE .../{usuarioEmpresaId}` sempre buscam por `id` + `empresaId` do contexto (`UsuarioEmpresaRepository.findByIdAndEmpresaId`); um `usuarioEmpresaId` inexistente e um `usuarioEmpresaId` de outra empresa recebem exatamente a mesma resposta (`403`, `AcessoNegadoException`, mensagem "Acesso negado") — mesmo padrão já usado por `ContextoEmpresaService` e `ConviteService.revogar`. Decisão explícita: não foi criada uma exceção "vínculo não encontrado" com `404` dedicado, para não revelar a um Administrador de uma empresa que um determinado `usuarioEmpresaId` pertence a outra empresa.
- Sem migration: nenhuma tabela ou coluna nova foi criada; `V1`, `V2` e `V3` permanecem intactas.

## Interface web

- Primeira interface funcional da Criati: `GET /login` (pública), `GET /app` (autenticada, redireciona para `/app/dashboard`) e `GET /app/dashboard` (autenticada). Controlador dedicado `br.app.criati.pagina.web.PaginaController`, seguindo o mesmo padrão de pacote (`<dominio>.web`) já usado por `security.web`, `tenant.web` etc. A autorização das rotas de página continua declarada em `SecurityConfig` (`permitAll` para `/login`, `anyRequest().authenticated()` para o resto), nunca checada ad-hoc no controller.
- Stack: Thymeleaf + HTML semântico + CSS modularizado (`criati-base.css`, `criati-login.css`, `criati-app.css`, `criati-responsive.css`) + JavaScript puro (`criati-api.js`, `criati-ui.js`, `criati-auth.js`, `criati-contexto.js`), sem nenhuma dependência frontend nova (sem React/Vue/Angular/jQuery/Bootstrap/Tailwind, sem CDN externa, sem biblioteca de ícones — SVG inline). `thymeleaf-extras-springsecurity6` e `spring-boot-starter-thymeleaf` já estavam no `pom.xml` desde a fundação do projeto; nenhuma dependência foi adicionada nesta fase.
- A interface é só uma camada de apresentação sobre a API já existente: o JavaScript consome `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`, `GET /api/contexto/empresas`, `GET/POST /api/contexto/empresa-ativa`. Nenhum endpoint de negócio novo foi criado; nenhuma regra de autorização foi duplicada no frontend — o cliente só exibe o que o backend retorna e reage aos status HTTP (401/403/etc.), nunca decide por conta própria se um dado pode ou não ser mostrado.
- `SecurityFilterChain` passou a diferenciar o `AuthenticationEntryPoint` por rota (`DelegatingAuthenticationEntryPoint`): `/api/**` continua respondendo 401 em JSON (`JsonAuthenticationEntryPoint`, comportamento inalterado, coberto pelos testes já existentes); qualquer outra rota autenticada (`/app/**`) usa `LoginUrlAuthenticationEntryPoint("/login")`, que redireciona o navegador em vez de devolver um corpo JSON. `RequestCache` continua desabilitado globalmente (decisão anterior, ver seção "Autenticação"), então esse redirecionamento não cria sessão nem tenta "replay" da requisição original após o login.
- `/login`, `/css/**`, `/js/**`, `/img/**` e `/error` foram adicionados como `permitAll` — exatamente o conjunto já antecipado em `docs/SEGURANCA.MD`, seção "Rotas públicas". Nenhuma rota autenticada foi tornada pública.
- CSRF no navegador: token exposto via meta tags (`<meta name="_csrf">` / `<meta name="_csrf_header">`), renderizadas a partir do atributo de requisição `_csrf` do Thymeleaf (funciona independente do repositório de token configurado — hoje `CookieCsrfTokenRepository`). O nome do header nunca é hardcoded no JavaScript: `criati-api.js` lê o nome real da meta tag antes de anexar o header em requisições mutáveis (`POST`/`PUT`/`PATCH`/`DELETE`). Nenhuma rota nova foi adicionada à lista de exceções de CSRF (login continua sendo a única exceção relevante para o fluxo do navegador).
- Sessão: a interface nunca usa `localStorage`/`sessionStorage` para autenticação. O cookie de sessão é `HttpOnly` (gerenciado inteiramente pelo servidor); o cookie CSRF é o único legível por JavaScript, por design do próprio Spring Security (`CookieCsrfTokenRepository.withHttpOnlyFalse()`, decisão já registrada). Logout limpa apenas estado visual local (nome do usuário exibido, menu aberto/fechado); quem invalida a sessão é sempre o backend (`POST /api/auth/logout`).
- Tratamento de erro HTTP centralizado em `criati-api.js`: 401 fora do formulário de login redireciona para `/login?motivo=sessao` (mensagem genérica de sessão expirada); o próprio formulário de login desativa esse redirecionamento automático (`redirectOn401: false`) para poder mostrar "E-mail ou senha inválidos." inline, sem recarregar a página. Nenhuma resposta de erro (stack trace, SQL, classe Java) é exibida ao usuário — o cliente só usa `message`/`status` do `ApiErrorResponse` já padronizado, com mensagens genéricas de fallback por status.
- Seleção de empresa ativa: se o usuário tem exatamente um vínculo ativo e nenhuma empresa ainda selecionada na sessão, o frontend seleciona automaticamente essa única empresa (chamando `POST /api/contexto/empresa-ativa` já existente); com mais de uma empresa vinculada e nenhuma selecionada, a tela pede que o usuário escolha explicitamente. Em nenhum caso o `empresaId` enviado é inventado pelo cliente — é sempre um dos valores devolvidos por `GET /api/contexto/empresas`.
- Dashboard inicial mostra apenas dados reais já expostos pela API (empresa ativa, perfil na empresa, status da conta, quantidade de empresas vinculadas) — nenhum número fictício. Os atalhos de Usuários e Convites no dashboard e no menu lateral passaram a navegar para as telas reais (ver seção "Interface de Usuários e Convites"); o selo "Em breve" foi removido desses dois itens.
- Escopo explicitamente fora desta fase: recuperação de senha, qualquer tela de administração do Superadministrador além de `/app/admin/empresas`, PWA/offline, testes end-to-end de navegador (a cobertura desta fase é via `MockMvc`, que renderiza o HTML real através do Thymeleaf e verifica presença de elementos/rotas/segurança, mas não executa o JavaScript do navegador).

## Catálogo de aplicações e Clínica Vida Demo

- A definição técnica central dos módulos passa a ser `CodigoAplicacao`, com metadados de apresentação,
  rota, situação e ordem estável. `ModuloDisponibilidadeService` combina essa definição com os vínculos
  persistidos já existentes. Módulos futuros podem ser reconhecidos como `INDISPONIVEL`, sem seed, rota
  ou exposição para empresas. Habilitação técnica por `EmpresaAplicacao` não representa contratação,
  plano ou assinatura. Detalhes em `docs/PLATAFORMA_MODULOS.md`.

- Conceito novo: `Aplicacao` (tabela `aplicacao`, código único, nome, descrição, status) é o catálogo global de soluções que a Criati oferece — hoje `FINANCEIRO` (produto real) e `CLINICA` (demonstração), criado por `V4__criar_estrutura_aplicacoes.sql`. Uma aplicação inativa nunca pode ser utilizada por nenhuma empresa, mesmo que o vínculo esteja ativo.
- `Aplicacao` e perfil (`PerfilUsuario`, ver seção "Multiempresa") são conceitos ortogonais e não devem ser confundidos: perfil é "o que o usuário pode fazer dentro da empresa" (ADMINISTRADOR/GESTOR/USUARIO); aplicação é "qual solução a empresa contratou/habilitou" (Financeiro, Clínica). Uma empresa pode ter várias aplicações habilitadas ao mesmo tempo; nenhuma delas concede papel novo ao usuário, e nenhum papel novo (`ROLE_FINANCEIRO`/`ROLE_CLINICA`) foi criado para representar acesso a uma aplicação — o controle é feito checando o vínculo `EmpresaAplicacao`, não uma authority do Spring Security.
- `EmpresaAplicacao` (tabela `empresa_aplicacao`, `UNIQUE(empresa_id, aplicacao_id)`) é o vínculo entre uma empresa e uma aplicação do catálogo, com o mesmo modelo de estado já usado por `UsuarioEmpresa`: `StatusCadastro` (`ATIVO`/`INATIVO`), sem exclusão física — desabilitar uma aplicação para uma empresa marca o vínculo como `INATIVO` (`AplicacaoService.desabilitar`), nunca remove a linha. Reabilitar reutiliza o mesmo vínculo (`findByEmpresaIdAndAplicacaoId` antes de decidir entre criar ou reativar), nunca duplica.
- Habilitar/desabilitar são operações idempotentes por decisão explícita (diferente da política de `UsuarioEmpresa.suspender/reativar`, que rejeita repetir a mesma transição com `409`, ver seção "Gestão de acessos por empresa"): repetir `habilitar` ou `desabilitar` sobre um vínculo que já está no estado alvo apenas confirma o estado (`200`), sem erro. Alternativa descartada: replicar a rejeição `409` de `UsuarioEmpresa` — rejeitada porque não há o mesmo risco de "o Superadministrador acha que uma pré-condição vale e não vale mais" que motivou aquela decisão; aqui o Superadministrador está apenas garantindo um estado desejado (herói do próprio painel administrativo, que sempre relê a situação atual antes de agir), então uma operação repetida é inofensiva e mais simples de usar num painel que apenas alterna um botão.
- Regra explícita de negócio: habilitar exige empresa `ATIVA` (`EmpresaInativaException`, `409`) e aplicação `ATIVA` no catálogo (`AplicacaoInativaException`, `409`); desabilitar não tem essa exigência (sempre é seguro tirar acesso). Um código de aplicação inexistente no catálogo responde `404` (`AplicacaoNaoEncontradaException`), mesmo padrão de `EmpresaNaoEncontradaException`.
- Nenhuma regra de código depende do nome ou do CNPJ de uma empresa: a única empresa de demonstração (Clínica Vida Demo) é identificada apenas para fins de carga idempotente (evitar duplicar na reinicialização), nunca para alterar comportamento de autorização ou de negócio — o backend trata essa empresa exatamente como qualquer outra que tenha a aplicação CLINICA habilitada.
- `AplicacaoCatalogoSeedRunner` (`ApplicationRunner`, roda em todo perfil) garante que o catálogo (`FINANCEIRO`, `CLINICA`) exista de forma idempotente, complementando `V4__criar_estrutura_aplicacoes.sql`: em Postgres (local/homolog/prod) a migration já insere essas linhas (`ON CONFLICT (codigo) DO NOTHING`) e o runner apenas confirma que existem; no perfil `test`, o Flyway está desabilitado (`ddl-auto=create-drop`, ver `application-test.properties`) e é esse runner quem efetivamente semeia o catálogo antes dos testes rodarem. Sem ele, nenhuma aplicação existiria no banco de teste e toda a funcionalidade seria impossível de testar de ponta a ponta.
- Endpoints administrativos (`ROLE_SUPERADMIN`, mesmo padrão de `/api/admin/**`): `GET /api/admin/aplicacoes` (catálogo completo), `GET /api/admin/empresas/{empresaId}/aplicacoes` (catálogo com a situação do vínculo para essa empresa — `statusVinculo` nulo significa "nunca habilitada"), `POST .../{codigo}/habilitar` e `POST .../{codigo}/desabilitar` (exigem CSRF, como qualquer operação que altera estado).
- Endpoint empresarial: `GET /api/contexto/aplicacoes` — usuário autenticado, exige contexto de empresa ativa (`ContextoEmpresaService.exigirContextoAtivo`, mesmo `403` genérico já usado por outras rotas de contexto), retorna somente as aplicações `ATIVAS` no catálogo **e** com vínculo `ATIVO` para a empresa ativa da sessão. Nunca recebe `empresaId` do cliente — a empresa vem exclusivamente do contexto da sessão, mesmo princípio já aplicado a `/api/contexto/usuarios` e `/api/contexto/convites`. Cada item retorna `urlInicial` (mapeamento fixo `FINANCEIRO → /app/financeiro`, `CLINICA → /app/clinica`, calculado no backend em `AplicacaoContextoResponse`), nunca inventado pelo cliente.
- Rotas de página: `GET /app/aplicacoes` (autenticada, sem exigência de aplicação específica — apenas lista o que a empresa ativa tem), `GET /app/financeiro` e `GET /app/clinica` (autenticadas, cada uma exige contexto de empresa ativa **e** a aplicação correspondente habilitada). Sem contexto ativo ou sem a aplicação habilitada, ambas redirecionam para `/app/aplicacoes` — a mesma resposta para os dois motivos, para não revelar qual condição falhou (mesmo princípio já usado pelos `403` genéricos da API, adaptado para uma resposta navegável de página, já que lançar uma exceção de negócio em uma rota `@Controller` retornaria um corpo JSON via `GlobalExceptionHandler`, quebrando a experiência de navegação). O controle de acesso vive inteiramente no backend (`PaginaController`), nunca apenas ocultando um link no menu.
- `GET /app/admin/empresas` (interface administrativa mínima, apenas Superadministrador): autorização declarada em `SecurityConfig` (`hasAuthority(ROLE_SUPERADMIN)` para `GET /app/admin/**`), mesmo padrão já usado para `/api/admin/**`. A tela consome exclusivamente `GET /api/admin/empresas` e `GET/POST /api/admin/empresas/{id}/aplicacoes/**`; não há cadastro completo de empresa nem edição de dados cadastrais nesta tela.
- Clínica Vida Demo: carga opcional (`ClinicaVidaDemoRunner`/`ClinicaVidaDemoService`), desabilitada por padrão (`CRIATI_DADOS_DEMO_HABILITADOS=false`), seguindo o mesmo padrão arquitetural do bootstrap de Superadministrador (`ApplicationRunner` lendo `@Value` de variável de ambiente). Quando habilitada, cria de forma idempotente (busca por CNPJ antes de criar) apenas a empresa "Clinica Vida Demo" (CNPJ de demonstração `11.222.333/0001-81`, dígitos verificadores válidos, mas sem relação com nenhuma empresa real) com a aplicação `CLINICA` habilitada. Não cria: empresa de demonstração para `FINANCEIRO` (module fica apenas disponível no catálogo, para ser habilitado em empresas reais), nenhum `Usuario` nem senha, nenhum Superadministrador adicional. A ausência de usuário demo é deliberada: esta tarefa cobre apenas o catálogo de aplicações e a empresa demonstrativa, não dados fictícios de pacientes/atendimentos nem contas de acesso à Clínica Vida Demo.

## Financeiro (MVP do Gerenciador Financeiro)

- Primeiro módulo operacional real construído sobre o catálogo de aplicações (`FINANCEIRO`, ver seção anterior). Modelo: `ContaFinanceira`, `CategoriaFinanceira`, `LancamentoFinanceiro` (migration `V5__criar_estrutura_financeiro.sql`), todas escopadas por `empresa_id`, todas sem exclusão física (inativação/cancelamento lógicos apenas).
- Um único enum `TipoFinanceiro` (`RECEITA`/`DESPESA`) é usado tanto por `CategoriaFinanceira.tipo` quanto por `LancamentoFinanceiro.tipo`, em vez dos dois enums originalmente cogitados (`TipoCategoriaFinanceira`/`TipoLancamentoFinanceiro`) — os dois teriam exatamente os mesmos valores e precisam ser comparados por igualdade na regra de compatibilidade categoria↔lançamento; dois enums idênticos apenas duplicariam a mesma informação.
- Nenhuma role nova foi criada para o módulo (`ROLE_FINANCEIRO`/`ROLE_CLINICA` não existem): o controle de escrita reaproveita integralmente `PerfilUsuario` (`ADMINISTRADOR`/`GESTOR`/`USUARIO`), já existente para o restante do multiempresa. `reabrir` (desfazer um pagamento) é reservado ao `ADMINISTRADOR`; as demais operações de escrita (criar/editar/pagar/cancelar) são permitidas a `ADMINISTRADOR` e `GESTOR`; `USUARIO` tem acesso somente leitura no MVP.
- Acesso ao módulo exige, além do contexto de empresa ativa já existente, que a aplicação `FINANCEIRO` esteja habilitada para a empresa (`ContextoFinanceiroService.exigirAcesso`, reaproveitando `AplicacaoService.possuiAplicacaoAtiva`) — centralizado em um único serviço para não duplicar a checagem em cada controller do módulo.
- Saldo nunca é persistido: é sempre derivado por consulta (`SaldoFinanceiroService`), única fonte de verdade reaproveitada tanto pela tela de contas quanto pelo dashboard, evitando duas fórmulas divergentes para o mesmo número. Detalhes completos do modelo, cálculo de saldo, regras de status, permissões, endpoints, telas e limitações do MVP em `docs/FINANCEIRO.md`.

## Interface de Usuários e Convites

- Telas completas de gestão de usuários (`/app/usuarios`) e convites (`/app/convites`), consumindo integralmente os endpoints já existentes (`/api/contexto/usuarios/**`, `/api/contexto/convites/**`) — nenhum endpoint novo foi criado, nenhuma regra de backend foi alterada. O backend continua sendo a única fonte de verdade: o frontend não decide autorização, nunca envia `empresaId` (a empresa ativa vem exclusivamente da sessão no servidor) e reage às respostas 400/401/403/404/409 do backend em vez de antecipá-las.
- Autorização das duas páginas: como `ADMINISTRADOR`/`GESTOR`/`USUARIO` são perfis por empresa (`UsuarioEmpresa.perfil`), não authorities do Spring Security, a checagem não pôde usar `hasAuthority(...)` em `SecurityConfig` (que só conhece `ROLE_SUPERADMIN`). `PaginaController` exige contexto ativo (`ContextoEmpresaService.exigirContextoAtivo`) e perfil `ADMINISTRADOR`, lançando `AcessoNegadoException` caso contrário — capturada pelo `GlobalExceptionHandler` (`@RestControllerAdvice`), que responde `403` em JSON mesmo vindo de um `@Controller` de página (não um `@RestController`); esse é o mesmo mecanismo e a mesma resposta genérica ("Acesso negado") já usados por toda a API, sem distinguir "sem contexto" de "perfil insuficiente" — e sem authority nova. `GET /app/admin/empresas` continua sendo o único caso que usa `hasAuthority` diretamente (papel global, não perfil por empresa).
- Um Superadministrador sem vínculo empresarial nunca acessa essas páginas implicitamente: `exigirContextoAtivo` já falha antes de qualquer checagem de perfil, por não existir nenhum `UsuarioEmpresa` para validar.
- Modal acessível reutilizável: `criarModal`/`confirmarAcao` foram adicionados a `criati-ui.js` (não duplicados em `criati-usuarios.js`/`criati-convites.js`) — cuidam de foco inicial dentro do modal, ciclo de Tab preso ao modal (focus trap), fechar com Esc e devolver o foco ao elemento que abriu o modal. `confirmarAcao` substitui `window.confirm()` nativo por um diálogo acessível (`role="dialog"`, `aria-modal`, `aria-labelledby`) para as confirmações de suspender/remover acesso e revogar convite.
- `criati-acessos.css` é autocontido (toolbar, tabela, badges, cards mobile, modais) em vez de reaproveitar as classes equivalentes já existentes em `criati-financeiro.css`: as duas telas ficam desacopladas do módulo Financeiro, evitando que uma alteração futura em qualquer um dos dois precise considerar o outro. Layout: tabela no desktop, cards no mobile (breakpoint 768px, mesmo já usado pelo menu lateral), nunca as duas ao mesmo tempo.
- Ações sobre o próprio vínculo: os botões "Suspender" e "Remover acesso" aparecem desabilitados (com `title` explicando o motivo) na própria linha do usuário autenticado, e as opções `GESTOR`/`USUARIO` ficam desabilitadas no seletor de perfil ao editar o próprio vínculo — isso é só uma facilidade visual; a regra real (`AutoAlteracaoNaoPermitidaException`, `409`) continua sendo validada pelo backend a cada chamada, exatamente como antes.
- Filtros de usuários (`busca`, `perfil`, `status`) usam os parâmetros de consulta já suportados por `GET /api/contexto/usuarios`. Convites não têm essa capacidade no backend (`GET /api/contexto/convites` sempre retorna todos os convites da empresa ativa); os filtros de convites (e-mail, perfil, status) são aplicados inteiramente no cliente, sobre a lista já carregada — nenhuma regra de autorização nova, apenas conveniência de exibição.
- Token bruto do convite: exibido em modal de sucesso somente quando o campo `tokenBruto` está presente na resposta (isto é, apenas quando `criati.convite.expor-token-bruto=true`, hoje padrão em `local`/`test`; ver seção "Convites"). Mantido apenas numa variável local do módulo `criati-convites.js`, nunca em `localStorage`/`sessionStorage`, nunca em `console.log`; a variável é descartada ao fechar o modal (`fecharModalSucesso`). Em qualquer outro ambiente (produção), a resposta não traz o campo e a interface mostra apenas "Convite criado. A entrega por e-mail será integrada em etapa futura."
- Link de convite copiável: montado no cliente como `window.location.origin + "/convites/" + token` (nunca um domínio hardcoded). Diagnóstico confirmou que não existe hoje nenhuma página pública Thymeleaf para aceitar convite — apenas a API pública já existente (`GET/POST /api/convites/{token}[/aceitar]`, `br.app.criati.convite.web.ConvitePublicoController`). Criar essa página ficou fora do escopo desta tarefa (seria um endpoint público novo, proibido explicitamente); o link seguindo o padrão `/convites/{token}` fica preparado para quando essa página existir, e a limitação atual está documentada aqui e no `README.md`.
- Nenhum teste de execução real de JavaScript foi adicionado (o projeto não tem infraestrutura Node/Jest e nenhuma foi adicionada nesta tarefa). A cobertura automatizada é via `MockMvc` (presença de elementos, scripts, ids, ausência de exposição de segredo no HTML renderizado) mais uma auditoria estática dos dois arquivos JS (verifica ausência de `localStorage.setItem`/`sessionStorage.setItem` e de log do token). A revisão funcional do comportamento em runtime do JavaScript (cliques, fetch, foco de modal) foi feita manualmente durante o desenvolvimento, não por teste automatizado.

## Página pública de aceite de convite

- `GET /convites/{token}` (`br.app.criati.pagina.web.PaginaController`) é a primeira rota de página verdadeiramente pública além de `/login`: renderiza a view `convite/aceitar` sem ler o `token` no controller (nem como `@PathVariable`, nem em log) — toda a validação e a aceitação acontecem inteiramente no navegador, contra a API pública já existente (`GET/POST /api/convites/{token}[/aceitar]`, implementada antes desta tarefa). Autorização declarada em `SecurityConfig` (`requestMatchers(HttpMethod.GET, "/convites/*").permitAll()`), mesmo padrão já usado para `/login`; nenhuma outra rota foi liberada.
- Fluxo completo: administrador cria convite → link `{origem}/convites/{token}` (já implementado na tela de Convites, ver seção "Interface de Usuários e Convites") → convidado abre a página → frontend chama `GET /api/convites/{token}` para validar → convidado informa nome e senha → frontend chama `POST /api/convites/{token}/aceitar` → conta e vínculo são criados, convite marcado `UTILIZADO` → frontend mostra sucesso e leva ao login (`/login?motivo=convite-aceito`), **sem autenticar automaticamente** (o backend já não cria sessão nesse endpoint, decisão anterior documentada na seção "Convites"; a página não adiciona nenhuma autenticação client-side).
- Validação dupla, nunca confiada só ao GET: a página usa o resultado de `GET /api/convites/{token}` (`valido: true/false`) apenas para decidir o que EXIBIR (formulário ou estado indisponível); o `POST /aceitar` sempre revalida tudo de novo no backend (token, expiração, status, e-mail já cadastrado) — um convite que passou no GET mas foi revogado/expirado/utilizado um instante depois ainda é rejeitado corretamente no POST, e a página trata esse caso (`404`/`410`) transicionando para o mesmo estado "indisponível" do GET.
- Resposta genérica para convite inválido: como a API pública (`ConviteValidacaoPublicaResponse.invalido()`) nunca distingue o motivo (inexistente, expirado, utilizado, revogado ou empresa inativa), a página também não inventa o motivo — mensagem única "Este convite não está mais disponível." em todos os casos, e "Não foi possível validar o convite agora. Tente novamente." apenas para erro de rede/conexão (distinção que a própria página consegue fazer, já que é local: GET falhou de verdade vs. GET respondeu `valido:false`).
- 409 na aceitação (e-mail já cadastrado, `EmailJaCadastradoException`) nunca mostra a mensagem literal do backend ("E-mail ja cadastrado") na tela — o frontend substitui por um texto genérico ("Não foi possível concluir o convite. Entre em contato com o administrador."), para não confirmar a um visitante anônimo se um e-mail específico já está cadastrado na plataforma (mesmo princípio de "não revelar existência de usuário" já aplicado ao login, ver seção "Autenticação").
- Token bruto: lido exclusivamente da própria URL (`window.location.pathname`), nunca de `localStorage`/`sessionStorage`, nunca logado no console, nunca reenviado a nenhum serviço além da própria API de convites. Senha e confirmação de senha nunca vão para a URL, nunca são logadas, e são limpas do formulário (`value = ""`) após qualquer erro do backend (400/409/rede) e após sucesso.
- `/login?motivo=convite-aceito`: `criati-auth.js` passou a resolver a mensagem de `?motivo=` a partir de um mapa fixo de motivos conhecidos (`sessao`, `convite-aceito`); qualquer outro valor (incluindo tentativas de injeção via query string) é simplesmente ignorado — nunca vira texto na tela. O servidor nunca ecoa o parâmetro `motivo` de volta no HTML (o template de login não referencia `${param.motivo}`), então não há superfície de reflexão/injeção no lado do servidor; a mensagem é inteiramente decidida no cliente a partir do mapa fixo.
- CSRF: a página inclui a mesma meta tag `_csrf`/`_csrf_header` de toda página autenticada (fragmento `layout :: csrfMeta`), por consistência, mas a chamada real (`POST /api/convites/{token}/aceitar`) continua isenta de CSRF pela mesma exceção já registrada antes desta tarefa (`ignoringRequestMatchers` em `SecurityConfig`) — nenhuma isenção nova foi adicionada.
- JavaScript novo (`criati-convite-aceite.js`) reaproveita integralmente `criati-api.js` (nenhum cliente HTTP duplicado) e `criati-ui.js` (`initTogglePassword`, `setButtonLoading`); as duas chamadas usam `{ redirectOn401: false }` porque o redirecionamento automático padrão do cliente (`/login?motivo=sessao`) não faria sentido nesta página pública — na prática nunca é acionado, já que nenhum dos dois endpoints públicos retorna `401`, mas a opção evita qualquer redirecionamento incorreto caso isso mude no futuro.
- Limitação conhecida do ambiente de teste (não uma falha de segurança): em execução de suite completa (não isolada), MockMvc registra uma `MockHttpSession` em qualquer página que renderiza `${_csrf.token}` — inclusive `/login`, já existente antes desta tarefa — efeito colateral do uso extensivo de `.with(csrf())` no restante da suite (que sempre usa `HttpSessionCsrfTokenRepository` internamente, independente do repositório real da aplicação). Não é uma sessão autenticada nem um problema de segurança: confirmado isolando o teste e reproduzindo o mesmo efeito em `/login`. A garantia que importa de fato (nenhuma autenticação automática após aceitar convite) tem teste dedicado, documentado em `PaginaConviteAceitePublicaTests`.
- Integração de e-mail real segue como prioridade futura (já registrado desde a fundação do módulo de Convites): esta página consome o token retornado apenas em `local`/`test`; em produção, quem cria o convite ainda precisa entregar o link manualmente até essa integração existir.

## Sidebar recolhível com menu hambúrguer

- O botão hambúrguer já existente (`.criati-menu-toggle`, em `fragments/topbar.html`, `aria-controls="criati-sidebar"`) passou a ficar visível também no desktop (antes só aparecia no mobile) e ganhou um segundo comportamento: no desktop alterna a sidebar entre expandida e recolhida (compacta); no mobile continua controlando apenas abrir/fechar o menu off-canvas, sem nenhuma sobreposição entre os dois modos. Nenhum HTML novo foi necessário no botão — o mesmo elemento, o mesmo ícone (hambúrguer), e a mesma relação `aria-controls`/`id="criati-sidebar"` já cobriam os dois casos; apenas a regra CSS que escondia o botão no desktop (`display: none`) foi removida.
- Estado controlado inteiramente por classes, sem estilo inline: `.criati-app.is-sidebar-open` (padrão já existente, mobile, alterna aberto/fechado) e `html.criati-sidebar-collapsed` (novo, desktop, alterna expandida/compacta). Os dois nunca se sobrepõem: as regras CSS do modo compacto ficam dentro de `@media (min-width: 769px)`, e o JavaScript (`CriatiUI.initSidebarToggle`, em `criati-ui.js`) remove a classe compacta sempre que a viewport está em largura mobile, inclusive ao redimensionar a janela.
- Preferência visual persistida em `localStorage` (chave `criati.sidebar.recolhida`, valor `"true"`/`"false"`) — **somente** essa preferência; nenhum dado de autenticação, sessão, token, empresa, usuário ou permissão é lido ou gravado por este recurso (mesmo princípio já registrado na seção "Interface web": a interface nunca usa `localStorage`/`sessionStorage` para nada sensível). A preferência só se aplica no desktop; no mobile ela é ignorada mesmo que exista salva (o menu mobile sempre começa fechado).
- Para evitar o "flash" de sidebar expandida seguida de recolhimento no carregamento da página, um novo fragmento (`fragments/layout :: sidebarPreload`) injeta um script inline mínimo no `<head>` de cada página autenticada (as mesmas 10 páginas que já incluem `fragments/layout :: csrfMeta`), que aplica a classe `criati-sidebar-collapsed` em `<html>` antes da primeira pintura, só quando a viewport já é desktop e a preferência salva é `"true"`. O restante da lógica (clique no botão, redimensionamento, aria) roda depois, em `criati-ui.js`, já em cima do mesmo estado.
- Modo compacto: largura controlada por variáveis CSS (`--sidebar-width-expanded: 248px`, `--sidebar-width-collapsed: 76px`, em `criati-base.css`), ícones centralizados, texto/nome da seção/rodapé detalhado ocultos (`.criati-nav-label`, `.criati-nav-badge`, `.criati-sidebar-footer`, `.criati-sidebar-brand span` com `display: none` dentro do próprio `@media (min-width: 769px)`). O item ativo continua marcado (mesmo `box-shadow`/fundo de `.is-active`, inalterado). Nenhum novo ícone ou logo foi criado — o símbolo "C" já usado na marca (`.criati-sidebar-logo`) continua sendo o único elemento visível da marca quando recolhida.
- Tooltip acessível em CSS puro (pseudo-elemento `::after` com `content: attr(data-tooltip)`, visível em `:hover` e `:focus-visible`), sem nenhum JavaScript de tooltip. Cada link de navegação (estático em `fragments/sidebar.html` e os dinâmicos de Financeiro/Clínica inseridos por `criati-aplicacoes.js`) ganhou `aria-label` e `data-tooltip` com o mesmo texto do rótulo visível; como `aria-label` sempre está presente (independente do modo), o nome acessível do link nunca muda entre expandido e recolhido e o texto oculto visualmente (`.criati-nav-label` com `display: none`) nunca é lido em duplicidade por leitor de tela.
- Os itens dinâmicos (Financeiro/Clínica, inseridos por `CriatiAplicacoes.carregarSidebar`) passaram a ter um ícone (reaproveita o glifo já usado por "Aplicações" — nenhum ícone novo por aplicação) e o mesmo rótulo em `<span class="criati-nav-label">`, para se comportarem de forma idêntica aos itens estáticos no modo compacto; antes desta tarefa eram apenas texto puro, o que teria deixado esses itens em branco/sem tooltip quando a sidebar fosse recolhida.
- Fechar o menu mobile ao selecionar um item: como a navegação é sempre uma troca de página real (sem framework de rotas no cliente), isso já acontecia "de graça" via recarregamento; ainda assim, `criati-ui.js` fecha o menu explicitamente antes da navegação (delegação de clique em `.criati-nav`, cobre também os itens dinâmicos inseridos depois da inicialização), evitando qualquer sobreposição visual durante a transição de página. `Esc` continua fechando o menu mobile (comportamento já existente), devolvendo o foco ao botão hambúrguer.
- `prefers-reduced-motion: reduce` desativa a transição de largura (recolher/expandir no desktop) e a transição do tooltip; a transição de deslizar do menu mobile (`transform`, já existente antes desta tarefa) também passou a respeitar essa preferência. Nenhuma paleta de cor foi alterada; nenhuma dependência nova foi adicionada.
- Nenhum teste de execução real de JavaScript foi adicionado (mesma decisão já registrada nesta fase para a página de convite: o projeto não tem infraestrutura Node/Jest). A cobertura automatizada (`PaginaSidebarRecolhivelTests`) é via `MockMvc`: presença e atributos do botão/sidebar, rótulos de texto preservados, ausência total de marcação de sidebar em `/login` e em `/convites/{token}` (as duas únicas páginas sem o layout autenticado), e ausência de dado sensível no HTML e no JavaScript servido. O comportamento interativo (clique, redimensionamento, `Esc`, foco, tooltip) foi revisado manualmente lendo o código e validado via `curl` contra o servidor local rodando (`./mvnw spring-boot:run`), sem navegador real disponível neste ambiente.

## Temas claro, escuro e automático

- Três opções de aparência: `auto` (padrão), `light`, `dark`. Estratégia: atributo `data-theme="light"|"dark"` em `<html>`, nunca classes divergentes por página. A preferência do usuário (que pode ser `auto`) fica em `localStorage["criati.tema"]`; o atributo `data-theme` sempre contém o tema **já resolvido** (`light`/`dark`), nunca o literal `"auto"` — quando a preferência é `auto`, o valor de `data-theme` é recalculado a partir de `prefers-color-scheme` e reaplicado sempre que o sistema operacional muda de tema, sem recarregar a página. Apenas os valores `auto`/`light`/`dark` são aceitos; qualquer outro valor salvo (corrompido, editado manualmente) é ignorado e tratado como `auto`.
- `criati-tema.js` (novo módulo central, mesmo padrão de `criati-ui.js`) concentra toda a lógica: validar a preferência, detectar a preferência do sistema (`matchMedia("(prefers-color-scheme: dark)")`), aplicar o tema (`data-theme` + `style.colorScheme` em `<html>`), persistir a escolha, reagir à mudança do sistema **somente** quando a preferência é `auto` (uma preferência explícita `light`/`dark` nunca é sobrescrita por uma mudança do sistema operacional), atualizar os controles visuais e disparar o evento `criati:tema-alterado` (`detail: { preferencia, temaAplicado }`, sem nenhum dado de usuário/empresa/sessão) sempre que o tema muda — pensado para qualquer gráfico/indicador dinâmico futuro reagir sem recarregar a página; hoje os indicadores financeiros são CSS/HTML puro (barras, badges), já resolvidos pelas variáveis de tema, sem necessidade de ouvir o evento.
- Preload: novo fragmento `fragments/layout :: themePreload`, incluído no `<head>` das mesmas 12 páginas que renderizam layout (10 autenticadas + `/login` + `/convites/{token}`), executa antes da primeira pintura, lê apenas `criati.tema`, valida contra `auto/light/dark`, consulta `matchMedia` quando `auto`, e aplica `data-theme`/`color-scheme` em `<html>` — mesmo princípio já usado por `sidebarPreload` (ver seção "Sidebar recolhível"), evitando o "clarão" de um tema errado ao carregar. Não depende de sessão nem de backend; o servidor nunca sabe qual tema o navegador vai aplicar.
- Removida a tag estática `<meta name="color-scheme" content="dark">` de todas as 12 páginas (antes fixava sempre o esquema escuro para os controles nativos do navegador, ex.: scrollbar, independente do tema real da página). A propriedade CSS `color-scheme` em `:root`/`:root[data-theme="light"]`, mais o `style.colorScheme` aplicado dinamicamente pelo preload/`criati-tema.js`, assumem esse papel corretamente para os dois temas.
- Variáveis centralizadas em `criati-base.css`: `:root` mantém os valores escuros atuais como padrão (referência visual preservada); `:root[data-theme="light"]` sobrescreve somente o que muda entre os temas (fundo, superfícies, texto, borda, sombra, overlay, e os pares `*-bg`/`*-fg` de sucesso/aviso/perigo/info usados por alertas, badges e valores financeiros). `--criati-accent`/`--criati-accent-2` (azul/roxo) e os tokens de forma (`--criati-radius*`, `--sidebar-width-*`) são os mesmos nos dois temas — a identidade visual da Criati não muda, só o fundo. Novas variáveis: `--criati-info`, `--criati-input-bg`, `--criati-overlay`, `--criati-sidebar-bg`, `--criati-topbar-bg`, `--criati-success-bg`/`-fg`, `--criati-warning-bg`/`-fg`, `--criati-danger-bg`/`-fg`, `--criati-info-bg`/`-fg`.
- Tema claro: fundo geral cinza muito claro (`#eef1f6`), cards brancos (`--criati-surface: #ffffff`), texto grafite (`#1e2530`), bordas cinza translúcidas (antes eram brancas translúcidas — invisíveis sobre fundo claro), sombra bem mais discreta, sidebar/topbar num cinza levemente diferenciado do card branco (`--criati-sidebar-bg`/`--criati-topbar-bg: #eef1f7`). Os pares `*-bg`/`*-fg` (badges, alertas, valores financeiros positivos/negativos) usam tons escuros e saturados sobre fundo bem claro (ex.: verde `#15803d` sobre `rgba(22,163,74,.12)`), em vez dos tons pastel claros usados no tema escuro — o padrão antigo (pastel sobre fundo escuro) teria contraste insuficiente sobre um fundo claro.
- Consolidação: `criati-financeiro.css`, `criati-acessos.css` e `criati-convite.css` tinham cada um sua própria cópia de `rgba(2, 6, 14, 0.65)` para o overlay de modal, e cada badge de status tinha sua própria cor de texto fixa (`#fde68a`/`#bbf7d0`/`#fecaca`) em vez de uma variável — tudo migrado para `--criati-overlay` e para os pares `*-bg`/`*-fg` únicos, eliminando a duplicação (o overlay do mobile em `criati-responsive.css` também passou a usar `--criati-overlay`). `criati-valor-positivo`/`criati-valor-negativo` (Financeiro) passaram a reaproveitar `--criati-success-fg`/`--criati-danger-fg` em vez de um tom próprio — pequena unificação de tom (mesma família de cor, mesmo papel semântico), documentada aqui para não ser confundida com regressão visual.
- Cores mantidas fixas deliberadamente (não são "incompatibilidades" — são decorativas ou já neutras nos dois temas): gradiente da marca (`--criati-gradient`, texto branco por cima, sempre sobre a própria superfície colorida); gradiente da barra "despesa" no resumo financeiro; badge neutro (cancelado/inativo, cinza sobre `--criati-text-muted`, que já muda de tom por tema); halos decorativos do fundo de `/login` e `/convites/{token}` (radial-gradient translúcido sobre `--criati-bg`, que já muda por tema).
- Controle de tema: um único fragmento reutilizável (`fragments/tema :: temaControle`), incluído sem duplicar lógica em três lugares — dentro de `fragments/topbar.html` (páginas autenticadas, inline na barra) e envolvido por `.criati-tema-controle-flutuante` (posição fixa, canto superior direito) em `/login` e `/convites/{token}`. Botão (`aria-haspopup`, `aria-expanded`, `aria-controls`) abre um popover (`role="menu"`) com três opções (`role="menuitemradio"`, `aria-checked` refletindo a seleção atual) — Automático (ícone de computador), Claro (sol), Escuro (lua); o ícone do próprio botão também reflete a preferência atual (troca via CSS, sem manipular `innerHTML`). Fecha ao clicar fora, com `Esc` (devolvendo o foco ao botão) e por navegação com setas dentro do menu; nenhuma biblioteca de ícones — todo SVG inline, mesmo padrão já usado no resto do projeto.
- Sidebar recolhível (ver seção anterior): totalmente compatível, sem alteração de comportamento — a sidebar/tooltip/botão hambúrguer já eram construídos inteiramente sobre variáveis CSS, então herdam os dois temas automaticamente; a preferência de tema e a preferência de sidebar são independentes (`criati.tema` e `criati.sidebar.recolhida`, nenhuma removida ou lida pela outra).
- Segurança: `localStorage` guarda somente `criati.tema` (`auto`/`light`/`dark`) e `criati.sidebar.recolhida` (`true`/`false`) — nenhum dado de autenticação, sessão, token, empresa, usuário, e-mail, senha ou permissão. Nenhum endpoint novo; nenhuma alteração em `SecurityConfig`; nenhuma autorização decidida no frontend.
- Nenhum teste de execução real de JavaScript foi adicionado (mesma decisão já registrada nas fases anteriores: sem infraestrutura Node/Jest). A cobertura automatizada (`PaginaTemaTests`) é via `MockMvc`: presença e atributos do controle de tema em página autenticada/`/login`/`/convites/{token}`, presença do script de preload e de `criati-tema.js`, ausência da meta `color-scheme` fixa, ausência de dado sensível no HTML e no JavaScript servido, e confirmação de que nenhuma rota privada nova foi liberada. A sintaxe de `criati-tema.js` foi verificada com `node --check` (Node já disponível no ambiente; nenhuma infraestrutura de teste JS foi adicionada).

## Painel administrativo completo do Superadministrador

- Escopo: onboarding completo de empresa (`/app/admin/empresas/nova`), visão geral (`/app/admin`), listagem/detalhe de empresas (`/app/admin/empresas`, `/app/admin/empresas/{id}`), usuários globais (`/app/admin/usuarios`) e vínculos globais (`/app/admin/vinculos`). Superava o que `docs/SEGURANCA.MD`/README já registravam como "fora do escopo desta fase" (que hoje corresponde às seções deste documento) — atualizado aqui: essa fase amplia deliberadamente o painel além de `/app/admin/empresas`.
- `POST /api/admin/empresas` (criação direta empresa+administrador, com senha informada na mesma chamada) **não foi alterado nem removido** — continua exatamente como testado antes desta fase. As novas telas de onboarding não usam mais esse endpoint: `POST /api/admin/empresas/com-administrador-existente` (vincula usuário global já existente, sem tocar em senha) e `POST /api/admin/empresas/com-administrador-convidado` (gera convite de `ADMINISTRADOR`, senha definida pelo próprio convidado ao aceitar) passam a ser os únicos caminhos usados pela UI nova — mantendo o princípio "Superadministrador nunca define senha de cliente" para todo fluxo novo, sem quebrar o endpoint antigo (ainda coberto pelos testes originais).
- Ambos os novos endpoints de criação são transacionais (`@Transactional` em `AdminEmpresaService`): empresa, aplicações iniciais habilitadas e vínculo/convite do administrador nascem ou desfazem juntos — uma falha em qualquer etapa (CNPJ duplicado, usuário inexistente, e-mail já convidado etc.) desfaz tudo, sem empresa/aplicação/vínculo/convite órfão.
- Convite de Administrador a partir do painel global exigiu revisitar uma decisão anterior (seção "Interface de Usuários e Convites" original considerava um endpoint global de convite redundante, já que o Superadministrador criava o administrador diretamente). Com o onboarding por convite, esse endpoint passou a ser necessário: `ConviteService.criarComoSuperAdministrador(empresaId, email, perfil, criadoPorUsuarioId)` reaproveita a mesma lógica de `criar(...)` (extraída para um método privado comum), mas recebe o `empresaId` diretamente em vez de um `ContextoEmpresaAtual` de sessão — correto, já que o Superadministrador nunca tem vínculo/contexto de empresa ativa. Exposto em `AdminEmpresaConviteController` (`/api/admin/empresas/{empresaId}/convites`), espelhando o padrão de separação já usado por `AdminEmpresaAplicacaoController`.
- Ativação/inativação de empresa (`POST /api/admin/empresas/{id}/ativar|inativar`) reaproveita o mesmo enum `StatusCadastro` já usado pela entidade — nenhum novo enum de ciclo de vida (`SUSPENSA`/`CANCELADA`/etc., cogitados em documentos de visão de produto) foi criado, e nenhuma migration foi necessária. `Empresa.ativar()/inativar()` seguem o mesmo padrão não-idempotente de `UsuarioEmpresa.suspender()/reativar()` (lançam `EmpresaStatusInvalidoException`, 409, ao repetir o mesmo estado) em vez do padrão idempotente de `EmpresaAplicacao.habilitar()/desabilitar()` — decisão deliberada: ativar/inativar é uma ação operacional explícita do Superadministrador sobre o ciclo de vida da empresa, não um toggle de feature, então repetir a mesma ação é tratado como erro de uso (mesmo raciocínio já usado para vínculos).
- Nenhum dado é excluído ao inativar uma empresa: usuários, vínculos, aplicações habilitadas e convites são preservados; apenas o acesso é bloqueado (via `ContextoEmpresaService`, que já valida `empresa.status == ATIVO`, mecanismo existente desde antes desta fase). Reativar não recria nada e não reativa vínculos suspensos automaticamente (cada vínculo mantém seu próprio status, controlado separadamente).
- "Situação operacional" (`PRONTA`/`PENDENTE`) é inteiramente calculada em memória em `AdminEmpresaService.buscarDetalhe` — nenhum campo novo persistido. `PRONTA` exige simultaneamente: empresa `ATIVA`, ao menos um `ADMINISTRADOR` com vínculo `ATIVO`, e ao menos uma aplicação habilitada; qualquer ausência mantém `PENDENTE` (inclui o caso do convite de administrador ainda não aceito, já que nesse caso ainda não existe vínculo `ADMINISTRADOR`).
- Vínculos e usuários globais (`AdminVinculoService`/`AdminUsuarioService`) reaproveitam integralmente as regras já existentes de proteção do último Administrador ativo (`countByEmpresaIdAndPerfilAndStatus <= 1` → `UltimoAdministradorAtivoException`) e de duplicidade (`UsuarioEmpresaJaVinculadoException`) — a única regra que **não** se aplica ao painel global é a de autoalteração (`AutoAlteracaoNaoPermitidaException`), pois o Superadministrador não participa como vínculo da empresa que administra (não existe "próprio vínculo" a proteger nesse contexto).
- Sem paginação/busca no banco para as listagens administrativas (`GET /api/admin/empresas`, `GET /api/admin/usuarios`, `GET /api/admin/vinculos`): volume inicial pequeno, mesma decisão já registrada para `GET /api/contexto/convites`. Filtros de busca textual são aplicados em memória no serviço (`AdminUsuarioService`) ou no cliente (`criati-admin-vinculos.js`); filtros por enum (`status`, `perfil`, `empresaId`) são aceitos como parâmetros de consulta e aplicados no banco via `findAll()` + `Stream.filter` no serviço (ainda sem paginação real). Se o volume crescer, a próxima etapa é adicionar paginação real (`Pageable`) nesses três endpoints — documentado como limitação conhecida, não uma omissão silenciosa.
- A tela de listagem de empresas (`/app/admin/empresas`) precisa exibir, por empresa, contadores que só existem no endpoint de detalhe (`GET /api/admin/empresas/{id}`: usuários ativos, administradores, aplicações habilitadas) — o endpoint de listagem (`GET /api/admin/empresas`) não foi alterado (usado também pelo teste antigo `AdminEmpresaControllerTests.deveListarEmpresasQuandoSuperAdministrador`, que verifica apenas `200`). `criati-admin-empresas.js` busca o detalhe de cada empresa em paralelo (`Promise.all`) após a listagem básica — aceitável apenas para o volume pequeno esperado nesta fase; documentado aqui como um N+1 deliberado, a ser revisitado (endpoint de listagem enriquecido ou projeção dedicada) se o número de empresas crescer.
- Usuário global: sem endpoint de ativação/inativação nesta fase. `Usuario` já tem campo `status` (`StatusCadastro`) e `UsuarioPrincipal.isEnabled()` já usa esse campo para negar login, mas não existe hoje nenhum método `Usuario.ativar()/inativar()` nem regra consolidada sobre o que fazer com os vínculos de um usuário global inativado (suspender todos automaticamente? preservar?). Em vez de inventar essa regra, o painel (`/app/admin/usuarios`) implementa **somente consulta** (listagem, detalhe, vínculos, convites pendentes relacionados) — mutação de status global fica para uma decisão explícita futura, registrada como limitação em `docs/PAINEL_ADMINISTRATIVO.md`.
- Nenhum dado sensível exposto: `UsuarioAdminResponse`/`UsuarioDetalheAdminResponse`/`VinculoAdminResponse` nunca incluem `senha`, hash ou token; `superAdministrador` (booleano) é exposto no detalhe do usuário global por ser informação operacional relevante para o painel (não uma credencial), já publicamente inferível hoje pelo comportamento do sistema (usuário acessa `/app/admin/**`).
- Sidebar: a seção "Administração da plataforma" é renderizada condicionalmente (`th:if="${superAdministrador}"`) a partir de um `@ModelAttribute("superAdministrador")` novo em `PaginaController`, calculado a cada requisição a partir do principal autenticado (`false` para anônimo, via `@AuthenticationPrincipal` retornando `null` para `AnonymousAuthenticationToken`) — disponível em todo template renderizado por esse controller sem precisar alterar a assinatura do fragmento `sidebar(paginaAtiva)` nem os `th:replace` das páginas já existentes (o model attribute é visto por qualquer fragmento incluído na mesma renderização). Esse bloco só **exibe ou oculta o link**; a autorização real permanece inteiramente no `SecurityConfig` (`/app/admin/**` e `/api/admin/**` continuam exigindo `ROLE_SUPERADMIN`), então esconder o link não é, e nunca substitui, controle de acesso.
- Nenhuma dependência nova, nenhuma migration, nenhum framework frontend, nenhuma alteração em `pom.xml`.

## Estilos visuais (Criati, Windows, Compacto) e tipografia refinada

- Separação conceitual deliberada entre **tema** (cor/contraste/superfície: `auto`/`light`/`dark`, já registrado na seção "Temas claro, escuro e automático") e **estilo** (forma/tipografia/densidade/sombra/borda: `criati`/`windows`/`compact`, novo nesta fase) — as duas dimensões são independentes e combináveis (ex.: tema escuro + estilo Windows), cada uma com sua própria chave de `localStorage` e seu próprio atributo em `<html>` (`data-theme` e `data-style`, respectivamente). Nenhum dos dois lê ou altera o outro.
- Estratégia idêntica à já usada para tema: atributo `data-style="criati"|"windows"|"compact"` em `<html>`, preferência em `localStorage["criati.estilo"]`, apenas os três valores são aceitos (qualquer outro salvo é tratado como `criati`, o padrão). `criati-estilo.js` (novo módulo, mesmo padrão de `criati-tema.js`) concentra validar, aplicar, persistir, atualizar os controles visuais e disparar `criati:estilo-alterado` (`detail: { estilo }`, sem nenhum dado de usuário/empresa/sessão).
- Preload: novo fragmento `fragments/layout :: estiloPreload`, incluído no `<head>` das mesmas páginas que já têm `themePreload` (as 15 páginas autenticadas + `/login` + `/convites/{token}`), executa antes da primeira pintura, lê apenas `criati.estilo`, valida contra `criati/windows/compact` e aplica `data-style` em `<html>` — evita qualquer mudança visível de estilo após o carregamento, mesmo princípio já usado para tema e para sidebar recolhida.
- Controle de estilo: novo fragmento reutilizável (`fragments/estilo :: estiloControle`), com a mesma estrutura de interação do controle de tema (botão + popover `role="menu"`/`role="menuitemradio"`, `Esc` fecha, setas navegam, fecha ao clicar fora, foco retorna ao botão) — deliberadamente **não** foi fundido num único menu "Aparência" com o controle de tema: os dois seletores existentes (`fragments/tema.html`) já tinham cobertura de teste própria (`PaginaTemaTests`), e mantê-los como controles irmãos (lado a lado na topbar autenticada, ou lado a lado flutuantes em `/login`/`/convites/{token}`) evitou qualquer risco de regressão nesses testes sem ganho real de simplicidade.
- Variáveis centralizadas em `criati-base.css`, reaproveitando os tokens de forma já existentes (`--criati-radius`, `--sidebar-width-*`) em vez de criar uma segunda arquitetura paralela: `--criati-font-size-{xs,sm,base,lg,xl}`, `--criati-nav-{font-size,section-font-size,item-height,icon-size,gap,font-weight,padding-x}`, `--criati-radius-{md,lg}`, `--criati-space-{xs,sm,md,lg,xl}`, `--criati-control-height`, `--criati-control-padding-x`, `--criati-card-padding`, `--criati-table-cell-padding`, `--criati-sidebar-item-height` (alias de `--criati-nav-item-height`) e `--criati-shadow-soft` (sombra mais discreta que `--criati-shadow`, usada em popovers/toasts). Cada estilo define seu próprio bloco `:root[data-style="..."]` com os mesmos nomes de variável — nenhum CSS inteiro duplicado por estilo, só os tokens que mudam.
- Colisão de cascata resolvida explicitamente: `--criati-shadow`/`--criati-shadow-soft` dependem simultaneamente de tema (cor/opacidade) e de estilo (tamanho/blur), mas `:root[data-theme=x]` e `:root[data-style=y]` têm a mesma especificidade CSS — sem tratamento especial, a regra que aparecesse por último no arquivo sempre venceria, silenciosamente ignorando a outra dimensão em certas combinações (ex.: tema claro + estilo Compacto perderia a sombra mais discreta do Compacto). Resolvido com seis seletores combinados de maior especificidade (`:root[data-theme="dark"][data-style="criati"]`, etc., um por combinação tema×estilo), garantindo as 9 combinações citadas no objetivo (3 temas × 3 estilos) sem regressão.
- Redução tipográfica global (item obrigatório, independente do estilo ativo): uma única regra, `html { font-size: 93.75% }` em `criati-base.css`, reduz ~6,25% a escala inteira da interface (topbar, sidebar, cards, tabelas, formulários, dashboards, páginas públicas) de uma só vez, sem tocar seletor por seletor — possível porque praticamente todo `font-size` do sistema já era definido em `rem` (relativo ao `<html>`, nunca ao elemento pai). Paddings/alturas de controle (definidos em `px`, não `rem`) não são afetados, então botões e campos continuam com a mesma área de clique confortável — só o texto encolhe.
- Sidebar (requisito prioritário): tipografia e densidade agora controladas por variáveis `--criati-nav-*` (ver acima) em vez de valores fixos espalhados em `criati-app.css`. Valores por estilo — item de menu / título de seção / altura do item / ícone / peso:
  - **Criati** (padrão): 13,5px / 11px / 38px / 17px / 500.
  - **Windows**: 13px / 11px / 36px / 16px / 400 (peso mais leve, reforçando a sobriedade "desktop").
  - **Compacto**: 12,5px / 10,5px / 33px / 15px / 500 (piso deliberado: nenhum texto funcional abaixo de ~12,5px).
  - Sidebar recolhida: o tooltip (`::after` com `content: attr(data-tooltip)`) passou a usar `var(--criati-nav-font-size)` em vez de um tamanho fixo — nunca maior que o item de menu aberto — e `--criati-shadow-soft` em vez de `--criati-shadow`, mais discreto.
  - Mobile (`≤768px`): a sidebar é sempre off-canvas (nunca o modo compacto de ícones), então `criati-responsive.css` sobrescreve `--criati-nav-font-size: 14px`, `--criati-nav-item-height: 44px`, `--criati-nav-icon-size: 18px` dentro do próprio breakpoint, independente do estilo ativo — evita que o estilo Compacto deixe a navegação por toque pequena demais.
- Topbar refinada para combinar com a nova escala da sidebar: padding, seletor de empresa, avatar e nome do usuário passaram a usar as mesmas variáveis de espaçamento/tipografia/controle (`--criati-space-*`, `--criati-font-size-*`, `--criati-control-padding-x`), sem crescer mais que a sidebar em nenhum dos três estilos.
- Densidade de tabelas/modais (Usuários, Convites, Financeiro, painel administrativo): células de tabela e padding de modal passaram a usar `--criati-table-cell-padding`/`--criati-card-padding` em vez de valores fixos — no estilo Compacto isso reduz a altura de cada linha de tabela e o padding de cada modal, permitindo mais linhas visíveis por tela (objetivo explícito para o Financeiro) sem remover nenhum dado ou texto.
- Páginas públicas (`/login`, `/convites/{token}`): recebem o controle de estilo (`.criati-estilo-controle-flutuante`, ao lado do `.criati-tema-controle-flutuante` já existente) e a mesma redução tipográfica global, mas o padding do cartão de login/convite usa um piso deliberado (`calc(var(--criati-card-padding) + 14px)`) para nunca ficar "compacto demais" mesmo no estilo Compacto — preservando o foco no formulário, como pedido no objetivo.
- Windows e Compacto foram desenhados sem qualquer cópia de identidade proprietária (nem do Windows, nem do ChatGPT ou de qualquer outro sistema): nenhum logo, nenhum ícone de terceiros, nenhuma paleta de cor alterada (cor continua sendo responsabilidade exclusiva do tema) — a única referência usada foi a **escala tipográfica e a densidade** de interfaces modernas de produtividade, aplicada através de tokens já existentes na identidade Criati (`--criati-accent`/`--criati-accent-2`, gradiente da marca, símbolo "C").
- Segurança: `localStorage` guarda somente `criati.estilo` (`criati`/`windows`/`compact`) — nenhum dado de autenticação, sessão, token, empresa, usuário, e-mail, senha ou permissão. Nenhum endpoint novo; nenhuma alteração em `SecurityConfig`; nenhuma autorização decidida no frontend; nenhuma dependência ou migration.
- Nenhum teste de execução real de JavaScript foi adicionado (mesma decisão já registrada nas fases anteriores). A cobertura automatizada (`PaginaEstiloTests`) é via `MockMvc`: presença e atributos do controle de estilo em página autenticada/`/login`/`/convites/{token}`, presença do script de preload e de `criati-estilo.js`, presença das variáveis de navegação/densidade em `criati-base.css` para os três estilos, ausência de dado sensível no HTML e no JavaScript servido, independência total entre os scripts de tema e de estilo (nenhum referencia o atributo do outro), e confirmação de que nenhuma rota privada nova foi liberada. A sintaxe de `criati-estilo.js` foi verificada com `node --check`. Validação visual num navegador real não foi possível neste ambiente (sem acesso a um Postgres local configurado nem a uma ferramenta de automação de navegador) — a revisão de estilo foi feita lendo o CSS/HTML resultante e conferindo cada combinação tema×estilo manualmente no código-fonte.

## Correção do contexto de empresa do Superadministrador

- Causa raiz do "skeleton preso" no seletor de empresa da topbar, identificado na homologação
  visual (F4-008): puramente de frontend, não de autorização. `criati-contexto.js` só resolvia o
  skeleton (`renderTopbarEmpresa`) no ramo de sucesso completo, nunca nos ramos "sem empresa" ou
  "erro"; e as 6 páginas administrativas (`/app/admin/**`) nunca chamavam nenhum script que
  populasse a topbar. Detalhes completos em `docs/CORRECAO-CONTEXTO-SUPERADMIN-F4-009.md`.
- Decisão: detectar "é Superadministrador" no frontend por uma marcação já existente no DOM (o
  link `href="/app/admin"` na sidebar, renderizado condicionalmente pelo servidor a partir do
  model attribute `superAdministrador` já existente desde a F4-006) em vez de adicionar um campo
  `superAdministrador` ao contrato de `/api/auth/me`. Motivo: `/api/auth/me` é um endpoint estável
  já coberto por testes e consumido por múltiplas páginas; alterá-lo (mesmo de forma aditiva)
  para uma necessidade puramente cosmética (qual mensagem mostrar em um estado vazio) não se
  justificava frente à alternativa já disponível sem nenhuma mudança de contrato. Se uma
  necessidade mais ampla de expor o papel do usuário ao frontend surgir no futuro, essa decisão
  deve ser revisitada explicitamente aqui.
- O Superadministrador não ganhou nenhum acesso novo: continua sem contexto de empresa ativa (por
  regra de domínio, nunca tem vínculo próprio) e a correção é inteiramente de apresentação.

## Financeiro LeS — núcleo compartilhado com extensões específicas

- Decisão arquitetural (analisada em `docs/empresas/financeiro-les/REGRAS-DE-NEGOCIO.md` na
  LES-F1-002 e confirmada em `docs/empresas/financeiro-les/ARQUITETURA-FUNCIONAL.md` na
  LES-F1-003): o domínio financeiro residencial do Financeiro LeS (cartões, faturas, parcelas,
  compromissos, empréstimos, exposição a terceiros, orçamento, meta de economia, simulador) será
  construído como uma **extensão** que reaproveita um **núcleo financeiro compartilhado** —
  conta financeira, categoria, lançamento simples, pessoa, favorecido, anexo e recorrência — em
  vez de duplicar esses conceitos em um domínio isolado (Opção B) ou de embutir a complexidade
  residencial diretamente no módulo `FINANCEIRO` genérico já existente (Opção A, `docs/FINANCEIRO.md`).
- Motivo, reutilizável por qualquer futuro domínio financeiro da plataforma (não só o
  Financeiro LeS): o módulo `FINANCEIRO` genérico é um produto oferecido no catálogo de
  aplicações a qualquer empresa (`docs/MODELO_MULTIEMPRESA.md`) — forçar nele conceitos
  específicos de um único perfil de cliente acoplaria permanentemente o módulo genérico a um
  caso de uso particular. Um núcleo compartilhado com extensões habilitáveis por empresa (mesmo
  padrão de `EmpresaAplicacao` já existente) permite que a plataforma tenha múltiplos produtos
  financeiros no futuro (ex.: um financeiro empresarial mais avançado, distinto do residencial)
  sem duplicar contas, categorias e lançamentos a cada novo produto.
- Esta decisão **não foi implementada** nesta tarefa — nenhuma entidade, migration ou código foi
  criado. Ela orienta a modelagem técnica de uma etapa futura. Detalhes completos (entidades,
  relacionamentos, invariantes) em `docs/empresas/financeiro-les/MODELO-DE-DADOS.md` e
  `docs/empresas/financeiro-les/ARQUITETURA-FUNCIONAL.md`.

## Recorrências financeiras — regra de geração e idempotência (LES-F2-006)

- Decisão arquitetural, reutilizável por qualquer domínio que precise gerar registros periódicos
  a partir de uma regra (não só o Financeiro LeS): três conceitos distintos — **regra** (a
  recorrência, nunca movimenta saldo por si só), **ocorrência** (uma competência específica gerada
  pela regra) e **fato** (o `LancamentoFinanceiro` já existente, reaproveitado em vez de duplicado
  como um segundo tipo de lançamento paralelo). A regra apenas gera o fato; o fato mantém
  referência opcional à regra de origem (`recorrencia_id`) e uma origem explícita (`RECORRENCIA`,
  já reservada desde a V9).
- Idempotência de geração: unicidade `(recorrencia_id, data_competencia)` — NULL é tratado como
  distinto pelo SQL padrão, então lançamentos manuais (sem recorrência) nunca colidem entre si.
  Reexecutar a geração da mesma competência sempre retorna o registro já existente em vez de
  duplicar. Esse padrão (unicidade regra+competência, verificação antes de inserir) é reutilizável
  por qualquer futura geração periódica na plataforma.
- Regra de dia inválido em mês menor (dia 31 em mês de 30 dias, 29/30/31 de fevereiro): usa o
  último dia válido do mês, aplicada uniformemente a recorrências mensais e anuais. Resolve a
  pendência registrada em `docs/empresas/financeiro-les/PENDENCIAS.md` ("Critério de 'meses sem o
  dia configurado'"), adotando a proposta conservadora já sugerida ali.
- Geração automática: implementada como endpoint explícito
  (`POST /api/contexto/financeiro/recorrencias/gerar-automaticas`), processado sob demanda (ex.:
  ao acessar o módulo ou por acionamento manual), em vez de `@Scheduled`. Evita introduzir
  agendamento oculto e difícil de testar antes de existir uma necessidade real de execução em
  background; se essa necessidade surgir para este ou outro módulo, a decisão de adotar
  `@Scheduled` deve ser tomada e registrada separadamente.
- Escopo desta entrega: apenas periodicidade `MENSAL` e `ANUAL`. `SEMANAL` e `PERSONALIZADA` foram
  avaliadas e adiadas deliberadamente — não haveria uma competência mensal única para ancorar a
  regra de duplicidade acima sem um redesenho do conceito de "competência da ocorrência".

## Contas a pagar — compromisso, ocorrência e pagamento (LES-F2-007)

- Decisão arquitetural, reutilizável por qualquer domínio que precise separar uma obrigação
  recorrente/avulsa da sua liquidação parcial: três conceitos distintos — **compromisso**
  (`CompromissoFinanceiro`, a regra ou origem, nunca movimenta saldo), **ocorrência**
  (`OcorrenciaCompromisso`, a obrigação concreta de uma competência, com valor previsto/principal/
  juros/multa/desconto/total/pago/saldo e status calculado) e **pagamento**
  (`PagamentoOcorrenciaCompromisso`, a liquidação integral ou parcial, sempre gerando exatamente um
  `LancamentoFinanceiro`). Diferente da LES-F2-006 (onde a ocorrência não tem tabela própria, é
  apenas `recorrencia_id + data_competencia` no próprio lançamento), aqui a ocorrência precisa de
  tabela própria porque precisa suportar múltiplos pagamentos parciais e valores ajustáveis por
  competência — o que `LancamentoFinanceiro` (valor único, status de registro inteiro) não suporta.
- Integração com `RecorrenciaFinanceira` (reaproveitada integralmente, sem segundo motor de
  periodicidade): quando um compromisso é recorrente, ele referencia uma `RecorrenciaFinanceira` já
  existente; a geração de ocorrências reaproveita os métodos públicos já testados na LES-F2-006
  (`getProximaCompetencia`/`calcularVencimento`/`dentroDoPeriodo`/`avancarProximaCompetencia`), mas
  o resultado é uma `OcorrenciaCompromisso`, não um `LancamentoFinanceiro` direto. Uma recorrência
  vinculada a um compromisso fica bloqueada no fluxo antigo de geração direta de lançamento (guard
  em `RecorrenciaFinanceiraService`), para que a mesma competência nunca seja consumida duas vezes
  por dois caminhos concorrentes. Recorrências sem compromisso vinculado continuam gerando
  lançamento diretamente, sem nenhuma mudança de comportamento (zero regressão).
- `LancamentoFinanceiro` gerado por um pagamento de conta a pagar (`gerarDeContaAPagar`, nova origem
  `CONTA_A_PAGAR`) deliberadamente **não** referencia `recorrencia_id`, mesmo quando a ocorrência é
  recorrente: a constraint `uq_lancamento_financeiro_recorrencia_competencia` (LES-F2-006) pressupõe
  um único lançamento por competência, o que quebraria com múltiplos pagamentos parciais da mesma
  ocorrência. A rastreabilidade fica no pagamento (`PagamentoOcorrenciaCompromisso.ocorrencia`), não
  no lançamento.
- Estorno de pagamento reaproveita `LancamentoFinanceiro.cancelar(...)` (método já existente) em vez
  de criar um mecanismo de reversão paralelo; o pagamento nunca é apagado fisicamente, apenas
  marcado `ESTORNADO` com motivo/usuário/instante. Exige perfil `ADMINISTRADOR`, mesmo precedente já
  usado por `LancamentoFinanceiroService.desliquidar`.
- Comprovantes (anexos de ocorrência/pagamento) **não foram implementados**: nenhuma infraestrutura
  de upload existe em nenhum domínio do projeto, e construí-la de forma segura é um esforço de base
  já reservado para `LES-F2-010` em `docs/empresas/financeiro-les/PENDENCIAS.md`. Implementar um
  upload improvisado nesta tarefa violaria a diretriz explícita de não introduzir armazenamento
  inseguro sem a base adequada.
- Detalhes completos (cálculos, migration, endpoints, testes, limitações) em
  `docs/empresas/financeiro-les/IMPLEMENTACAO-F2-007.md`.

## Cartões de crédito — bandeira como enum, limite delegado ao principal (LES-F3-001)

- **Bandeira é um enum Java (`VISA, MASTERCARD, ELO, AMERICAN_EXPRESS, HIPERCARD, OUTRA`), não uma
  tabela de domínio**, apesar de `docs/empresas/financeiro-les/MODELO-DE-DADOS.md` propor originalmente
  replicar para bandeira o mesmo padrão de catálogo global-com-extensão-local já usado por
  `InstituicaoFinanceira` (`empresa_id` nulo = global). Decisão consciente de divergir dessa proposta:
  instituições financeiras variam genuinamente por família/empresa (cooperativas locais, fintechs) e por
  isso precisam de cadastro local; bandeiras de cartão são um conjunto pequeno e globalmente padronizado
  que nunca varia por empresa e não tem nenhuma regra de negócio anexada (explicitamente pedido pelo
  enunciado da tarefa) — uma tabela para um enum fechado sem regra de negócio seria arquitetura em
  excesso; o valor `OUTRA` já dá a mesma extensibilidade prática de um catálogo. Reutilizável: qualquer
  domínio futuro que precise de um "conjunto pequeno e padronizado, sem regra de negócio, nunca
  customizado por tenant" deve preferir enum a tabela de domínio, reservando o padrão de tabela
  (`empresa_id` nulo = global) para conceitos que realmente variam por empresa.
- **`CartaoCredito` introduz um eixo de bloqueio (`bloqueado` + `motivoBloqueio`) independente de
  `StatusCadastro`** (que continua representando só ativo/inativo em todo o módulo, sem alteração). É a
  primeira entidade do financeiro com dois eixos de situação ortogonais. Reutilizável: qualquer entidade
  futura que precise diferenciar "desativado" (encerramento, reversível, preserva histórico) de
  "bloqueado" (suspensão temporária de uso, também reversível, mas com motivo e semântica distintos) deve
  seguir o mesmo padrão de dois campos independentes, em vez de sobrecarregar um único enum de status com
  mais um valor.
- **Limite total, limite saudável, dia de fechamento e dia de vencimento de um cartão virtual são
  fisicamente `NULL` na própria linha** — nunca uma cópia do principal — e os valores efetivos são sempre
  obtidos por delegação (`CartaoCredito.getLimiteTotalEfetivo()` etc., que consultam `cartaoPrincipal`
  quando `tipo = VIRTUAL`). Diferente de titular/instituição/bandeira, que são copiados do principal como
  conveniência na criação mas armazenados independentemente em cada linha (podem divergir depois, por
  decisão explícita do usuário). Essa distinção é deliberada: o enunciado exige que "cartão virtual não
  gere limite independente" de forma estrutural, não apenas por convenção de interface — armazenar `NULL`
  torna impossível um virtual acumular um limite próprio através do fluxo normal do serviço, e o
  consolidado (`CartaoCreditoService.resumir`) soma apenas cartões físicos, eliminando por construção o
  risco de duplicar o limite concedido.
- **Bloquear o cartão principal nunca escreve no campo `bloqueado` de um cartão virtual vinculado.**
  `CartaoCredito.estaBloqueadoEfetivo()` combina o campo próprio com o do principal
  (`bloqueado || cartaoPrincipal.isBloqueado()`) apenas para leitura/exibição. Alternativa descartada:
  cascatear o bloqueio do principal para os virtuais no momento do bloqueio — rejeitada porque o
  enunciado pede explicitamente "impedir uso e exibir alerta, sem alterar status silenciosamente", e uma
  cascata automática tornaria impossível distinguir depois "este virtual foi bloqueado diretamente" de
  "este virtual está bloqueado só porque o principal foi bloqueado".
- Detalhes completos (campos, validações, migration, endpoints, testes, limitações) em
  `docs/empresas/financeiro-les/IMPLEMENTACAO-F3-001.md`.

## Regra de alteração

Nenhuma decisão estrutural registrada neste documento deverá ser alterada silenciosamente.

Caso uma mudança seja necessária:

1. explicar o motivo;
2. avaliar os impactos;
3. obter aprovação;
4. atualizar este documento;
5. somente depois implementar a alteração.
