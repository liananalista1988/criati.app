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

## Regra de alteração

Nenhuma decisão estrutural registrada neste documento deverá ser alterada silenciosamente.

Caso uma mudança seja necessária:

1. explicar o motivo;
2. avaliar os impactos;
3. obter aprovação;
4. atualizar este documento;
5. somente depois implementar a alteração.