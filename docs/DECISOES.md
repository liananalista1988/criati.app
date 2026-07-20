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

## Regra de alteração

Nenhuma decisão estrutural registrada neste documento deverá ser alterada silenciosamente.

Caso uma mudança seja necessária:

1. explicar o motivo;
2. avaliar os impactos;
3. obter aprovação;
4. atualizar este documento;
5. somente depois implementar a alteração.