# Painel Administrativo do Superadministrador

Este documento descreve a área global exclusiva do Superadministrador (`/app/admin/**` e `/api/admin/**`), separada da gestão empresarial comum (`/app/usuarios`, `/app/convites`).

## Superadministrador vs. Administrador empresarial

| | Superadministrador | Administrador empresarial |
|---|---|---|
| Escopo | Toda a plataforma | Uma empresa (a empresa ativa da sessão) |
| Representado por | `Usuario.superAdministrador = true` (authority `ROLE_SUPERADMIN`) | `UsuarioEmpresa.perfil = ADMINISTRADOR` |
| Acesso aos dados internos da empresa (financeiro, clínica) | Nenhum, sem vínculo próprio | Sim, dentro da própria empresa |
| Cria empresas, gerencia usuários globais e vínculos entre empresas | Sim | Não |
| Gerencia usuários/convites da própria empresa | Não (sem vínculo) | Sim |

Um Superadministrador nunca recebe acesso implícito aos dados de uma empresa: para operar dentro de uma empresa (ex.: ver lançamentos financeiros) ele precisaria de um `UsuarioEmpresa` próprio, como qualquer outro usuário — o painel administrativo nunca cria esse vínculo automaticamente.

## Onboarding de empresa

`GET /app/admin/empresas/nova` reúne, em um formulário organizado (não um wizard com navegação por etapas em JavaScript — ver "Decisões"), tudo o que é necessário para deixar uma empresa pronta para uso:

1. **Dados da empresa**: nome, nome fantasia (opcional), CNPJ.
2. **Aplicações iniciais**: `FINANCEIRO` marcado por padrão, `CLINICA` desmarcado (demonstrativo).
3. **Administrador inicial**, duas opções:
   - **Usuário existente**: busca por nome/e-mail (`GET /api/admin/usuarios?busca=...`), seleciona um usuário global já ativo — cria o vínculo `ADMINISTRADOR` imediatamente, sem tocar em senha.
   - **Novo usuário (convite)**: nome + e-mail — gera um convite de `ADMINISTRADOR`; a senha é definida pelo próprio convidado ao aceitar (`/convites/{token}`). Em ambiente local/test, o link do convite é mostrado para cópia manual.

Tudo é enviado numa única chamada (`POST /api/admin/empresas/com-administrador-existente` ou `POST /api/admin/empresas/com-administrador-convidado`), transacional: se qualquer etapa falhar (CNPJ duplicado, usuário inexistente, e-mail já convidado), nada fica parcialmente criado.

## Empresas

`GET /app/admin/empresas`: busca por nome, filtro por status e por aplicação habilitada (aplicados no cliente), tabela/cards com nome, CNPJ, status, data de criação, usuários ativos, administradores ativos, aplicações habilitadas e ações (ver detalhes, ativar/inativar). Sem exclusão física em nenhum momento.

`GET /app/admin/empresas/{id}`: dados gerais (incluindo situação operacional), aplicações (habilitar/desabilitar), resumo de usuários/vínculos com link para a tela de vínculos já filtrada por essa empresa, e convites (listar, criar, revogar).

**Situação operacional** (`PRONTA`/`PENDENTE`), calculada em memória, nunca persistida:

- `PRONTA`: empresa ativa **e** ao menos um Administrador ativo **e** ao menos uma aplicação habilitada.
- `PENDENTE`: qualquer uma das condições acima ausente (inclui o caso de convite de Administrador ainda não aceito).

Ativar/inativar preserva todos os dados, usuários, vínculos, aplicações e convites — apenas bloqueia (ou libera) novos acessos.

## Usuários globais

`GET /app/admin/usuarios`: busca por nome/e-mail, filtro por status global, lista com contadores (empresas vinculadas, vínculos ativos) e detalhe (vínculos por empresa com perfil/status, convites pendentes relacionados ao e-mail). Nunca exibe senha, hash, token ou tokenHash.

**Limitação conhecida**: não há ativação/inativação de usuário global nesta fase. O campo `Usuario.status` existe e já é usado para negar login, mas não existe hoje uma regra consolidada sobre o que fazer com os vínculos de um usuário global inativado — inventar essa regra ficou fora do escopo desta tarefa. O painel implementa apenas consulta.

## Vínculos globais

`GET /app/admin/vinculos`: usuário → empresa → perfil → status, com filtros por empresa/perfil/status e busca textual por usuário/empresa. Ações: criar vínculo (usuário existente + empresa + perfil), alterar perfil, suspender, reativar, remover (lógico).

Reaplica as mesmas proteções já usadas na gestão empresarial comum:

- impede vínculo duplicado (usuário + empresa);
- impede que a empresa fique sem nenhum Administrador ativo;
- nunca exclui fisicamente usuário, empresa ou vínculo.

A única regra que não se aplica é a de autoalteração (o Superadministrador não é, ele mesmo, um vínculo da empresa que administra).

## Convites no painel global

Convites são sempre vistos/criados no contexto de uma empresa específica (`/api/admin/empresas/{id}/convites`), nunca listados globalmente sem esse contexto. O token bruto só é retornado na resposta de criação, e somente quando `criati.convite.expor-token-bruto=true` (padrão em `local`/`test`); nunca é possível recuperá-lo depois.

## Segurança

- `/app/admin/**` e `/api/admin/**` exigem `ROLE_SUPERADMIN` (`SecurityConfig`); perfil `ADMINISTRADOR` empresarial não é suficiente.
- Nenhum `empresaId` de rota empresarial é aceito nos endpoints administrativos; os IDs usados são sempre os do path administrativo.
- CSRF continua obrigatório em toda mutação.
- Nenhuma senha é criada pelo Superadministrador nos novos fluxos de onboarding; nenhum token é persistido além do hash já existente (`Convite.tokenHash`).
- A sidebar mostra a seção "Administração da plataforma" apenas para quem tem `ROLE_SUPERADMIN` — isso é conveniência visual, não controle de acesso (a autorização real está sempre no backend).

## Limitações registradas

- Sem paginação real em `GET /api/admin/empresas`, `GET /api/admin/usuarios` e `GET /api/admin/vinculos` (filtros aplicados em memória/cliente); adequado ao volume inicial, deve ser revisitado se o número de empresas/usuários crescer.
- Listagem de empresas enriquece cada linha com uma chamada adicional ao detalhe (`GET /api/admin/empresas/{id}`) para obter contadores agregados — um N+1 deliberado, aceitável apenas para volume pequeno.
- Sem ativação/inativação de usuário global (ver seção "Usuários globais").
- Integração de envio de convite por e-mail continua fora do escopo (prioridade futura já registrada desde o módulo de Convites); o link só é exposto para cópia manual em `local`/`test`.
