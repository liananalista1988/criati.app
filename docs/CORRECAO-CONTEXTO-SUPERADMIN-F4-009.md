# Correção — Contexto de empresa do Superadministrador (CRIATI-F4-009)

## Causa raiz

O bug identificado na homologação visual (`CRIATI-F4-008`) — o seletor de empresa da topbar
(`#criati-topbar-empresa`) permanecendo com o skeleton de carregamento indefinidamente para o
Superadministrador — tinha **duas causas distintas, ambas exclusivamente de frontend**:

1. **`criati-contexto.js`** (`fragments/topbar.html` + páginas que chamam `CriatiContexto.iniciar()`):
   a função `renderTopbarEmpresa(...)` — a única responsável por substituir o skeleton inicial do
   `#criati-topbar-empresa` — só era chamada no ramo de sucesso completo (`empresas.length > 0` **e**
   `contexto` presente). Nos ramos `estadoVazio()` (sem nenhuma empresa vinculada — o caso
   permanente do Superadministrador) e `.catch()` (erro de rede/servidor), o skeleton nunca era
   tocado e ficava preso para sempre.
2. **Páginas administrativas** (`/app/admin`, `/app/admin/empresas`, `/app/admin/empresas/nova`,
   `/app/admin/empresas/{id}`, `/app/admin/usuarios`, `/app/admin/vinculos`): nenhuma delas incluía
   `criati-contexto.js` nem chamava qualquer função equivalente — o nome do usuário
   (`#criati-user-name`, inicialmente "Carregando...") e o seletor de empresa nunca eram
   populados, independente de sucesso, erro ou vazio.

**Não havia nenhum problema de backend.** `GET /api/contexto/empresas` já retorna `200` com lista
vazia para quem não tem vínculo (nunca `403`); `GET /api/contexto/empresa-ativa` já retorna `204`
quando não há contexto selecionado (nunca `403`). Os únicos `403` observados durante a homologação
vinham de endpoints que **corretamente** exigem contexto de empresa ativo
(`ContextoEmpresaService.exigirContextoAtivo`, ex.: `/api/contexto/financeiro/**`,
`/api/contexto/usuarios/**`) — comportamento de autorização esperado e correto para um papel que,
por regra de domínio já documentada (`docs/PAINEL_ADMINISTRATIVO.md`), nunca tem vínculo de
empresa. Nenhum desses `403` foi removido ou contornado por esta correção.

## Comportamento anterior

- Dashboard/Aplicações/Financeiro/Usuários/Convites/Clínica (páginas que já chamavam
  `CriatiContexto.iniciar()`): nome do usuário aparecia corretamente, mas o seletor de empresa da
  topbar ficava com o skeleton preso quando o usuário não tinha nenhuma empresa vinculada.
- Páginas administrativas (`/app/admin/**`): nome do usuário **e** seletor de empresa ficavam
  presos no estado inicial (placeholder "Carregando...`/`-`" e skeleton), sempre, para qualquer
  Superadministrador.
- Quando havia mais de uma empresa vinculada e nenhuma ativa, o `<select>` da topbar não indicava
  visualmente que nenhuma opção tinha sido de fato escolhida (o navegador mostrava a primeira
  empresa como se já estivesse selecionada).
- A mensagem de "nenhuma empresa vinculada" no dashboard era genérica e não fazia sentido para o
  Superadministrador (para quem essa condição é permanente e esperada, não uma pendência).

## Comportamento implementado

- `renderTopbarEmpresa(...)` agora é chamada **em todo ramo** de `iniciar()` (sucesso, vazio,
  seleção pendente), logo após `renderUsuario(...)` — o skeleton é sempre substituído, mesmo
  quando não há nenhuma empresa (nesse caso, o container fica simplesmente vazio, refletindo
  corretamente "não aplicável a este papel", não um erro).
- No `.catch()` (falha de rede/servidor), uma nova função `limparTopbarEmpresa()` esvazia o
  container, evitando o skeleton preso também nesse caso.
- Quando existem empresas mas nenhuma está ativa (`Estado 4`), o `<select>` da topbar agora inclui
  uma opção desabilitada "Selecionar empresa" pré-selecionada, deixando claro que nenhuma escolha
  foi feita ainda (em vez de sugerir implicitamente a primeira da lista).
- Nova função `CriatiContexto.iniciarIdentidade()`, usada apenas pelas 6 páginas administrativas:
  popula o nome do usuário a partir de `/api/auth/me` (endpoint já existente, sem alteração de
  contrato) e limpa o seletor de empresa imediatamente — sem reusar `iniciar()`, porque esse fluxo
  pressupõe o conceito de "empresa vinculada", que não existe para o Superadministrador nesse
  contexto (ele opera sobre empresas por id explícito, nunca por contexto de sessão).
- No dashboard, quando não há nenhuma empresa vinculada **e** o usuário é Superadministrador
  (detectado via DOM — presença do link `/app/admin` na sidebar, já renderizado condicionalmente
  pelo servidor a partir do model attribute `superAdministrador` existente desde a F4-006 — nenhum
  novo campo foi adicionado a `/api/auth/me`), a mensagem passa a ser "Superadministrador não
  possui empresa ativa - isso é esperado para este papel." com um link para `/app/admin/empresas`
  (rota já existente e autorizada). Para qualquer outro usuário sem vínculo, a mensagem original
  ("Você ainda não está vinculado a nenhuma empresa.") permanece inalterada.

## Estados tratados (topbar + dashboard)

| Estado | Antes | Depois |
|---|---|---|
| 1. Carregando | Skeleton visível | Sem mudança (correto) |
| 2. Empresas disponíveis | Lista/seleção funcionavam quando havia empresa ativa | Sem mudança de comportamento; placeholder de "não selecionada" adicionado quando aplicável |
| 3. Nenhuma empresa disponível | Skeleton preso na topbar; mensagem genérica no dashboard | Skeleton resolvido (container vazio); mensagem diferenciada para Superadministrador com ação para Empresas |
| 4. Empresa não selecionada (múltiplas, nenhuma ativa) | `<select>` sugeria a 1ª opção silenciosamente | Opção "Selecionar empresa" desabilitada e pré-selecionada |
| 5. Acesso negado (403 legítimo, ex.: Financeiro sem contexto) | Já tratado corretamente pelos scripts de cada página (`criati-aplicacoes.js` etc.) | Sem alteração — já estava correto |
| 6. Erro inesperado | Dashboard já tratava (`estadoErro` + retry); topbar ficava presa | Topbar também é limpa no erro |

## Tratamento do 403

Nenhuma mudança de contrato ou de autorização. Os `403` de endpoints que exigem empresa ativa
continuam sendo retornados exatamente como antes (`AcessoNegadoException` via
`ContextoEmpresaService.exigirContextoAtivo`) e continuam sendo tratados pelas páginas que já os
esperavam (`criati-aplicacoes.js`, `criati-usuarios.js` etc., que já escondiam o "carregando" e
mostravam a caixa de erro correta antes desta tarefa — confirmado por revisão de código, sem
necessidade de alteração).

## Tratamento de ausência de empresa

`empresas.length === 0` (verdadeiro sempre para o Superadministrador, por regra de domínio) deixou
de ser um "beco sem saída" visual: a topbar é resolvida e o dashboard mostra uma mensagem
apropriada, com uma ação para a tela de Empresas quando aplicável.

## Tratamento de empresa inválida

Nenhuma mudança no backend — `ContextoEmpresaService.obterContextoAtual` já revalida o vínculo
(status ativo do vínculo **e** da empresa) a cada leitura do contexto e já limpa a sessão
automaticamente (`limparContexto`) se o vínculo salvo deixou de ser válido (empresa/vínculo
desativado após a seleção). Confirmado por leitura de código e pelos testes já existentes
(`vinculoDesativadoAposSelecaoInvalidaContextoNoProximoUso`,
`empresaDesativadaAposSelecaoInvalidaContextoNoProximoUso`, ambos em
`ContextoEmpresaControllerTests`, inalterados). Nenhuma alteração foi necessária aqui.

## Preservação do isolamento multiempresa

Nenhuma linha de backend foi alterada. `ContextoEmpresaService.selecionarEmpresaAtiva` continua
validando o `empresaId` recebido contra os vínculos reais do usuário autenticado
(`buscarVinculoValido`) antes de gravar qualquer coisa na sessão — um `empresaId` arbitrário (de
outra empresa, inexistente, ou de um vínculo inativo) continua resultando em `403` sem revelar se
a empresa existe. Validado nesta tarefa com um teste novo específico para Superadministrador
(`superAdministradorNaoConsegueSelecionarEmpresaSemVinculoProprio`) e reconfirmado manualmente em
navegador real.

## Segurança

- Nenhum dado sensível foi adicionado a `localStorage` (a correção não introduziu nenhuma escrita
  nova nesse mecanismo).
- Nenhuma alteração em `SecurityConfig`, em nenhum endpoint, nem no contrato de `/api/auth/me`.
- A detecção de "é Superadministrador" no frontend é puramente cosmética (decide qual mensagem
  mostrar) — nunca decide autorização; o backend continua validando tudo de novo em cada chamada,
  como já documentado em `docs/SEGURANCA.MD`.
- O Superadministrador não ganhou nenhum acesso novo a dados de empresa: ele continua sem contexto
  de empresa ativa (por design) e continua dependendo de `/app/admin/**`/`/api/admin/**` (que já
  exigem `ROLE_SUPERADMIN` e operam por `empresaId` explícito) para qualquer operação
  administrativa.

## Arquivos alterados

- `src/main/resources/static/js/criati-contexto.js` — correção principal (ver acima).
- `src/main/resources/templates/app/dashboard.html` — elementos `criati-dashboard-vazio-texto` e
  `criati-dashboard-vazio-acao` (mensagem/ação diferenciadas).
- `src/main/resources/templates/app/admin-dashboard.html`,
  `admin-empresas.html`, `admin-empresa-nova.html`, `admin-empresa-detalhe.html`,
  `admin-usuarios.html`, `admin-vinculos.html` — inclusão de `criati-contexto.js` e chamada de
  `CriatiContexto.iniciarIdentidade()`.
- `src/test/java/br/app/criati/pagina/web/PaginaContextoSuperadminTests.java` (novo).
- `docs/DECISOES.md` — registro da decisão de detectar o papel via DOM em vez de alterar o
  contrato de `/api/auth/me`.
- `docs/CORRECAO-CONTEXTO-SUPERADMIN-F4-009.md` (este documento).

Nenhum arquivo de backend (`src/main/java/**`) foi alterado.

## Limitações

- A diferenciação de mensagem de estado vazio foi aplicada apenas ao dashboard
  (`#criati-dashboard-vazio`, o único lugar onde esse elemento específico existe). Outras páginas
  (Aplicações, Financeiro, Usuários, Convites) têm seus próprios estados vazio/erro, já tratados
  corretamente por seus scripts (`criati-aplicacoes.js` etc.) antes desta tarefa — não foram
  revisitadas por estarem fora da causa raiz diagnosticada (o skeleton da topbar).
- A caixa de seleção de empresa própria do dashboard (`#criati-selecao-empresa`, elemento
  pré-existente e distinto do seletor da topbar) continua pré-selecionando a primeira opção do seu
  próprio `<select>` interno — comportamento anterior, não alterado por não fazer parte da causa
  raiz (o "skeleton preso" era exclusivamente do seletor da topbar).

## Testes executados

- `PaginaContextoSuperadminTests` (7 testes novos): páginas administrativas carregam
  `criati-contexto.js` e chamam `iniciarIdentidade()`; dashboard contém os elementos de estado
  vazio diferenciado; `criati-contexto.js` resolve a topbar antes do ramo vazio e nunca usa
  `console.log`; Superadministrador sem vínculo recebe lista vazia e `204`; Superadministrador não
  consegue selecionar empresa sem vínculo próprio; `/api/auth/me` funciona para Superadministrador.
- Suíte completa (421 testes, incluindo os 19 de `ContextoEmpresaControllerTests` já existentes e
  inalterados): `mvn clean test` e `mvn clean verify`, ambos `BUILD SUCCESS`.
- Validação manual em navegador real (Chrome headless via CDP, instância isolada e efêmera,
  mesmo método usado na F4-008): confirmado visualmente que o skeleton nunca fica preso em
  `/app/admin` nem em `/app/dashboard`, que a mensagem diferenciada aparece para o
  Superadministrador com o link para Empresas, que o placeholder "Selecionar empresa" aparece
  corretamente quando há múltiplas empresas sem nenhuma ativa, e que trocar de empresa pelo
  seletor da topbar atualiza o conteúdo do dashboard corretamente.
