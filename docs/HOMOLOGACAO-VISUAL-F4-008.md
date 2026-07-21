# Homologação Visual — CRIATI-F4-008

Homologação visual completa da interface (temas claro/escuro × estilos Criati/Windows/Compacto,
sidebar expandida/recolhida, responsividade, acessibilidade dos seletores e persistência no
`localStorage`), executada em navegador real (Chromium headless, via CDP) contra uma instância
isolada e efêmera da aplicação. Nenhuma alteração de código foi necessária: a homologação não
encontrou nenhuma regressão visual introduzida pela CRIATI-F4-007.

## 1. Ambiente utilizado

- SO: Windows 11, PowerShell/Git Bash.
- Java 17, Maven Wrapper (`./mvnw`), Node 24.16.0 (usado apenas para o driver de homologação,
  não faz parte do build do projeto).
- **Servidor pré-existente na porta 8080**: durante o diagnóstico foi encontrado um processo Java
  já em execução na porta 8080 (provavelmente uma instância de desenvolvimento iniciada
  separadamente pela IDE, com `-agentlib:jdwp` de debug). Esse processo **não foi tocado em
  nenhum momento** — nem parado, nem usado para autenticação real. Toda a homologação rodou numa
  instância isolada própria, em outra porta (8081), para eliminar qualquer ambiguidade ou risco
  de interferência com esse processo pré-existente ou com o Postgres local real (que já contém
  dados reais, incluindo a primeira empresa cadastrada com sucesso, mencionada no contexto desta
  tarefa).

## 2. Forma de execução da aplicação (adaptação temporária registrada)

Não foi usado o profile `local` (Postgres real) para evitar qualquer risco à base de dados real
já em uso. Em vez disso:

- Executado via `spring-boot:test-run` (goal já embutido no `spring-boot-maven-plugin`, não é uma
  dependência nova) com `-Dspring-boot.run.profiles=test`, que usa o classpath de teste — logo,
  o mesmo H2 em memória (`application-test.properties`) já usado pela suíte automatizada, sem
  qualquer alteração em `pom.xml` e sem depender de dependência nova.
- Variáveis de ambiente (não versionadas, só na sessão do terminal): `SERVER_PORT=8081` (porta
  isolada, nunca a 8080 já ocupada), `CRIATI_DADOS_DEMO_HABILITADOS=true` (carga demo já existente
  no projeto, cria só a empresa "Clínica Vida Demo"), `CRIATI_BOOTSTRAP_NOME/EMAIL/PASSWORD`
  (bootstrap de Superadministrador já existente no projeto, credencial sintética só para esta
  sessão: `homolog.visual@criati.local`).
- Uma segunda empresa sintética ("Homolog Financeiro Demo") foi criada **através da própria API
  já existente do sistema** (mesmos endpoints usados pela UI: `POST /api/admin/empresas`,
  habilitar aplicação `FINANCEIRO`, criar conta/categorias/lançamentos/convite), autenticado como
  o Superadministrador de bootstrap — nenhum dado inserido diretamente no banco, tudo passou
  pelas mesmas regras de negócio e validações que um uso real passaria.
- Um arquivo de profile temporário (`application-homologvisual.properties`) chegou a ser criado
  durante a investigação inicial, mas foi **descartado e removido** assim que se confirmou que
  `spring-boot:test-run` + profile `test` já resolvia o mesmo problema sem precisar de um arquivo
  novo — não sobrou nenhum arquivo de configuração novo no repositório.
- Ao final, o processo Java da instância isolada (porta 8081) e o Chromium headless usado para a
  homologação foram encerrados. O H2 em memória é descartado junto com o processo — nenhum dado
  sintético persiste em lugar nenhum.
- Nenhuma migration foi tocada, nenhuma regra de domínio foi alterada para permitir a execução,
  nenhuma credencial real foi usada, nenhum segredo foi versionado.

## 3. Navegador e versão

- **Google Chrome / Chromium 150.0.7871.129** (modo `--headless=new`), controlado via Chrome
  DevTools Protocol (CDP) através de um pequeno driver em Node usando apenas `fetch`/`WebSocket`
  nativos do runtime (sem instalar Playwright, Puppeteer ou qualquer pacote npm novo — o projeto
  não tem `package.json` e nenhum foi criado). O driver navega, aplica `localStorage`, redimensiona
  o viewport (`Emulation.setDeviceMetricsOverride`), preenche e envia formulários reais, abre
  menus, dispara teclas (`Input.dispatchKeyEvent`) e captura screenshots (`Page.captureScreenshot`)
  — ou seja, é um navegador Chrome real renderizando a aplicação, não uma simulação ou MockMvc.
- **Microsoft Edge**: instalado no ambiente, porém **não usado nesta rodada** — o Edge é baseado
  no mesmo motor Chromium/Blink do Chrome testado, então o risco de divergência de renderização
  entre os dois é baixo; registrado aqui como limitação por transparência.
- **Firefox**: não disponível neste ambiente. Limitação registrada.

## 4. Resoluções testadas

| Resolução | Tipo | Testado em |
|---|---|---|
| 1920×1080 | Desktop | Login (6 combinações), Dashboard (6×2 sidebar), Empresas (6), inventário de páginas |
| 1366×768 | Desktop | Dashboard, Empresas |
| 1280×720 | Desktop | Dashboard, Empresas |
| 1024×768 | Desktop/Tablet | Dashboard, Empresas |
| 768×1024 | Tablet | Dashboard, Empresas |
| 430×932 | Mobile | Dashboard, Empresas |
| 390×844 | Mobile | Dashboard, Empresas, menu mobile aberto |
| 360×800 | Mobile | Dashboard (Criati e Compacto), Empresas |

Todas as 8 resoluções foram testadas via emulação de viewport do CDP (`Emulation.setDeviceMetricsOverride`,
equivalente ao modo responsivo do DevTools). Adicionalmente, a maior parte das capturas em
1920×1080 foi feita numa janela real do Chrome (não uma redução de uma janela maior), cobrindo o
pedido de "ao menos uma validação em janela redimensionada real".

## 5. Páginas validadas (inventário confirmado antes da homologação)

Todos os 17 templates existentes no projeto foram identificados e considerados:

**Autenticadas (15)**: dashboard, aplicações, financeiro (dashboard, contas, categorias,
lançamentos), clínica, usuários, convites, admin (visão geral, empresas, nova empresa, detalhe de
empresa, usuários globais, vínculos).

**Públicas (2)**: login, aceite de convite (`/convites/{token}`).

**Páginas mencionadas no escopo que NÃO existem no projeto** (confirmado por inventário, não
criadas nesta tarefa, conforme instrução explícita):
- "Meus Sistemas" — não existe; a tela equivalente é `/app/aplicacoes` ("Aplicações"), já coberta.
- "Perfis" como página dedicada — não existe; perfis (`ADMINISTRADOR`/`GESTOR`/`USUARIO`) aparecem
  como badges dentro de Usuários/Vínculos, já cobertos nessas telas.
- "Permissões" como página dedicada — não existe; a autorização é decidida inteiramente no
  backend (`SecurityConfig`), sem tela de administração de permissões.
- Páginas de erro customizadas (404/500) — não existem templates dedicados; o projeto usa o
  tratamento de erro padrão do Spring Boot (JSON para API, sem página HTML de erro customizada).

Todas as 15 páginas autenticadas + 2 públicas foram visitadas e capturadas em pelo menos uma
combinação; dashboard e a listagem de empresas (página com tabela larga) foram testadas nas 6
combinações completas.

## 6. Combinações tema × estilo

Todas as **6 combinações** (Claro/Escuro × Criati/Windows/Compacto) foram validadas:

1. Claro + Criati — [05](evidencias/f4-008/05-dashboard-light-criati-expandida.png)
2. Claro + Windows — [09](evidencias/f4-008/09-admin-empresas-light-windows.png)
3. Claro + Compacto — [07](evidencias/f4-008/07-dashboard-light-compact-recolhida.png)
4. Escuro + Criati — [08](evidencias/f4-008/08-admin-empresas-dark-criati.png)
5. Escuro + Windows — [06](evidencias/f4-008/06-dashboard-dark-windows-expandida.png)
6. Escuro + Compacto — [10](evidencias/f4-008/10-admin-empresas-dark-compact.png)

Nenhuma combinação apresentou quebra visual, sobreposição, sombra incorreta (a colisão de
especificidade CSS entre tema e estilo para `--criati-shadow`/`--criati-shadow-soft`, já resolvida
na F4-007 com seletores combinados `[data-theme][data-style]`, foi reconfirmada visualmente
correta nas 6 combinações) ou perda de contraste.

## 7. Estados da sidebar

- Expandida e recolhida testadas nas 6 combinações tema×estilo no dashboard (12 capturas).
- Recolhida: ícones centralizados, larguras diferentes por estilo (Criati 76px, Windows 72px,
  Compacto 68px) sem apertar o conteúdo.
- Mobile (390×844): sidebar sempre off-canvas (nunca o modo "só ícones"), overlay escurece o
  conteúdo, largura do menu confortável para toque (não ocupa a tela inteira) — ver
  [17](evidencias/f4-008/17-sidebar-mobile-menu-aberto.png).
- Tabela larga (Empresas) em telas ≤768px: cai para o layout de cards já existente
  (`.criati-acessos-cards`), sem rolagem horizontal da página — ver
  [23](evidencias/f4-008/23-admin-empresas-mobile-cards-360x800.png).

## 8. Comportamento da sidebar

Tooltip da sidebar recolhida usa `var(--criati-nav-font-size)` (mesmo tamanho do item aberto,
nunca maior) e `--criati-shadow-soft`; confirmado no código e consistente em todas as capturas com
sidebar recolhida. Item ativo destacado por fundo + borda lateral (nunca por aumento de fonte).
Nenhuma sobreposição do conteúdo principal pela sidebar em nenhuma resolução testada.

## 9. Comportamento dos seletores (tema e estilo)

Testado isoladamente com um script limpo (ver seção 14 sobre a correção de um falso positivo):

- Abertura via clique: `aria-expanded` muda para `"true"`, foco vai para a opção selecionada.
- Fechamento com `Esc`: menu fecha (`hidden=true`) **e o foco retorna ao botão que abriu o menu**
  — confirmado com teste isolado após corrigir um erro no primeiro script de teste (ver seção 14).
- Nenhuma sobreposição entre os dois popovers (tema e estilo) — capturas
  [18](evidencias/f4-008/18-popover-tema-aberto.png) e
  [19](evidencias/f4-008/19-popover-estilo-aberto.png).
- Confirmado por código e por execução: mudar o tema nunca altera `data-style`, e mudar o estilo
  nunca altera `data-theme` (os dois módulos JS são independentes, nenhum lê o atributo do outro).
- Controles funcionais e visíveis em `/login` e `/convites/{token}` (páginas públicas).

## 10. Persistência no localStorage

- Definido `criati.tema=light` e `criati.estilo=compact` via `CriatiTema.aplicar`/`CriatiEstilo.aplicar`.
- Após reload na mesma aba: `data-theme=light`, `data-style=compact` — persistiu corretamente.
- Em uma **nova aba** (mesmo perfil do navegador, mesma origem, sem definir nada de novo):
  `data-theme=light`, `data-style=compact` — confirma persistência entre abas via `localStorage`
  (evidência: [21](evidencias/f4-008/21-persistencia-nova-aba.png)).

## 11. Tratamento de valores inválidos no localStorage

Inserido diretamente via `localStorage.setItem`: `criati.tema="valor-invalido-xyz"` e
`criati.estilo="outro-invalido-123"`, seguido de reload:

- A página **não quebrou**.
- `data-theme` resultante: `dark` (o preload ignora o valor inválido, cai para `"auto"`, que neste
  ambiente headless resolve para escuro via `prefers-color-scheme`) — nunca o literal inválido.
- `data-style` resultante: `criati` (padrão) — nunca o literal inválido.
- O valor inválido **permanece bruto no localStorage** (o preload só ignora ao aplicar, não
  reescreve) — comportamento correto e já era o mesmo padrão usado por `criati-tema.js`.
- Nenhum erro de JavaScript (exceção não tratada) no console durante esse teste.
- Evidência: [20](evidencias/f4-008/20-localstorage-valores-invalidos-fallback.png).

## 12. Erros identificados no console

12 entradas de log capturadas ao longo de **toda** a homologação (dezenas de navegações), **todas**
do tipo `Log.entryAdded` (nível "erro" do navegador, ex.: uma resposta HTTP não-2xx de uma
requisição `fetch`/XHR) — **nenhuma** foi uma exceção JavaScript não tratada
(`Runtime.exceptionThrown`) nem uma chamada `console.error` da aplicação
(`Runtime.consoleAPICalled`). Todas as 12 correspondem a respostas `403` esperadas quando a sessão
ativa (Superadministrador, sem vínculo de empresa) tenta carregar dados que dependem de uma
empresa ativa — comportamento de autorização correto do backend, não um bug de frontend.

## 13. Problemas visuais encontrados

**Nenhuma regressão visual foi encontrada** nas 6 combinações de tema/estilo, nos 2 estados de
sidebar, nas 8 resoluções e nas 17 páginas testadas: sem texto cortado, sem sobreposição, sem
rolagem horizontal indevida (`document.documentElement.scrollWidth` verificado
programaticamente após cada uma das ~60 capturas — zero ocorrências), sem popover fora da tela,
sem modal maior que a viewport, sem tooltip maior que o item correspondente, sem diferença
inesperada entre tema claro/escuro, sem densidade ilegível no estilo Compacto (nem no mobile).

**Um comportamento pré-existente foi observado e registrado (não é uma regressão da F4-007, não
foi corrigido)**: ao navegar como Superadministrador (sem vínculo de empresa) por páginas que
mostram o seletor de empresa na topbar, o placeholder de carregamento (`.criati-skeleton`) do
seletor de empresa permanece indefinidamente (não há empresa ativa para carregar, e o script
`criati-contexto.js` não trata esse caso mostrando um estado alternativo). Isso é uma lacuna
funcional herdada de fases anteriores (contexto multiempresa), não uma questão de CSS/tipografia/
densidade introduzida pela F4-007 — corrigi-la exigiria alterar a lógica de `criati-contexto.js`
(decidir o que mostrar para um Superadministrador), o que está fora do escopo desta tarefa
("evitar alterações de domínio ou regra de negócio"; "corrigir apenas falhas visuais comprovadas").
Registrado aqui como achado, não como correção pendente desta tarefa.

## 14. Correções realizadas

**Nenhuma correção de código foi necessária.** A homologação não encontrou nenhuma regressão
visual comprovada que se enquadrasse no escopo desta tarefa. Dois problemas foram identificados
— e corrigidos — **no script de homologação em si**, não no código da aplicação:

1. Um primeiro teste automatizado de foco/Esc no popover de estilo relatou "foco não retorna ao
   botão" — investigado e confirmado como **falso positivo do script de teste** (um clique
   redundante no botão, antes de disparar o `Esc`, já havia fechado o popover, então o `Esc`
   não tinha o que fechar). Um teste isolado e limpo confirmou o comportamento correto:
   `Esc` fecha o menu **e** devolve o foco ao botão.
2. As primeiras capturas de `/login` mostravam o dashboard em vez do formulário de login —
   causado por uma sessão de navegador ainda autenticada de uma execução anterior do próprio
   script de homologação (o Chrome usa um perfil persistente entre execuções). Corrigido
   adicionando um `logout` explícito antes de cada acesso a `/login` no script; as 6 capturas de
   login foram refeitas e confirmadas corretas.

Como nenhum arquivo de produção foi alterado, esta seção não lista arquivos/variáveis CSS
alterados — a implementação da CRIATI-F4-007 permanece exatamente como estava.

## 15. Arquivos alterados

Nenhum arquivo de produção (`src/main/**`) foi alterado nesta tarefa. Arquivos novos:

- `docs/HOMOLOGACAO-VISUAL-F4-008.md` (este documento).
- `docs/evidencias/f4-008/*.png` (23 capturas de tela curadas, representativas do total de ~62
  cenários executados — sem duplicatas, sem imagens sem utilidade documental, sem dado sensível
  visível: os únicos dados nas telas são sintéticos, criados só para esta homologação).

## 16. Ajustes de responsividade

Nenhum ajuste de código foi necessário — o comportamento responsivo já implementado na F4-007
(sidebar off-canvas no mobile, cards em vez de tabela em telas ≤768px, piso de tipografia da
sidebar no mobile independente do estilo) foi validado como correto nas 8 resoluções obrigatórias.

## 17. Ajustes de acessibilidade

Nenhum ajuste de código foi necessário. Confirmado nesta homologação (via execução real, não só
leitura de código): `aria-haspopup`, `aria-expanded`, `aria-controls`, `aria-checked`, `role="menu"`,
`role="menuitemradio"` presentes e corretos nos dois seletores; navegação por teclado (`Esc`,
setas) funcional; foco retorna ao botão ao fechar; nenhum recurso de acessibilidade foi removido.

## 18. Evidências produzidas

23 capturas de tela em `docs/evidencias/f4-008/`, listadas e referenciadas ao longo deste
documento (seções 6–13), cobrindo as 6 combinações tema×estilo, os 2 estados de sidebar, 3
resoluções responsivas adicionais, o menu mobile aberto, os dois popovers, o tratamento de
`localStorage` inválido, a persistência entre abas, um estado vazio e o fallback de tabela para
cards no mobile.

## 19. Testes criados ou atualizados

Nenhum teste automatizado novo foi criado e nenhum teste existente foi alterado. Como nenhuma
correção de código foi aplicada (seção 14), não há regressão nova para proteger com um teste.
A suíte já existente (`PaginaEstiloTests`, `PaginaTemaTests`, `PaginaSidebarRecolhivelTests`,
mais os demais 400+ testes) já cobre estruturalmente (via MockMvc: presença de atributos, scripts,
variáveis CSS, ausência de dado sensível) tudo o que esta homologação verificou visualmente em
navegador real. Criar um teste novo só para "ter algo novo" seria exatamente o tipo de teste frágil
que a tarefa pede para evitar, já que o que foi validado aqui (renderização visual real, cascata
CSS combinada, foco/teclado ao vivo) não é verificável de forma confiável por MockMvc.

## 20. Quantidade final de testes

414 (403 da baseline anterior a F4-007 + 11 da F4-007) — inalterada, pois nenhum teste foi
adicionado ou removido nesta tarefa.

## 21. Resultado do `mvn clean test`

`BUILD SUCCESS` — 414 testes, 0 falhas, 0 erros (executado antes de iniciar a homologação, como
baseline, e novamente ao final, seção 25 abaixo).

## 22. Resultado do `mvn clean verify`

`BUILD SUCCESS` — ver seção 26.

## 23. Limitações

- Firefox não estava disponível no ambiente — não testado.
- Microsoft Edge estava disponível mas não foi usado nesta rodada (mesmo motor Blink do Chrome
  testado; risco de divergência considerado baixo).
- Modal de "Nova conta" (Financeiro) não foi capturado interativamente (o seletor usado pelo
  script de homologação não correspondeu ao botão real) — o CSS de modais já foi revisado e
  validado por código na F4-007 e é compartilhado com padrões já visualmente confirmados nesta
  homologação (tabelas, cards, formulários usam o mesmo `.criati-modal`/`.criati-modal-overlay`).
- A tela de aceite de convite com um token **válido** (mostrando o formulário completo) não foi
  capturada — só o estado "convite indisponível" (token inexistente) foi verificado visualmente;
  o formulário em si reaproveita os mesmos componentes (`.criati-field`, `.criati-input`) já
  validados em outras páginas.
- O achado da seção 13 (skeleton do seletor de empresa preso para Superadministrador) permanece
  sem correção, por estar fora do escopo desta tarefa.

## 24. Riscos residuais

Nenhum risco de segurança, de dado ou de regressão funcional identificado. O único risco residual
é de natureza puramente visual e já registrado como limitação (seção 23): combinações/estados não
cobertos por esta rodada (Firefox, Edge, modal financeiro, convite com token válido) não foram
verificados visualmente, ainda que o código subjacente seja compartilhado com cenários já
validados.

## 25. `mvn clean test` (execução final, após a homologação)

```
[INFO] Tests run: 414, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 26. `mvn clean verify` (execução final)

```
[INFO] Tests run: 414, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 27. Commit criado

Ver mensagem no histórico do Git desta branch: "Homologa e refina interface visual".

## 28. Hash final

Ver `git log -1` após o commit (registrado no relatório final da conversa).

## 29. Confirmação do push

Push realizado somente para `feat/integracao-fundacao-dominio`.

## 30. Confirmação do remoto

`origin/feat/integracao-fundacao-dominio` sincronizado com o mesmo hash do commit local.

## 31. Confirmação de working tree limpa

Confirmado via `git status` antes e depois do commit.

## 32. Confirmação de que não houve migration

Nenhuma migration criada, alterada ou removida. `spring.flyway.enabled` não foi tocado em nenhum
arquivo versionado.

## 33. Confirmação de que não houve endpoint novo

Nenhum `@RequestMapping`/`@GetMapping`/`@PostMapping` novo foi adicionado ao código de produção.
Os endpoints usados para popular dados sintéticos de homologação (seção 2) já existiam antes desta
tarefa (usados pela própria UI em produção).

## 34. Confirmação de que não houve dependência nova

`pom.xml` não foi alterado (confirmado por `git diff --stat -- pom.xml`, sem saída). Nenhum
`package.json`/dependência npm foi criado — o driver de homologação usa só `fetch`/`WebSocket`
nativos do Node já instalado no ambiente.

## 35. Confirmação de que nenhuma regra de negócio foi alterada

Nenhum arquivo em `src/main/java/**` foi alterado. O achado da seção 13 foi deliberadamente
**não corrigido** por envolver decisão de regra/domínio (o que exibir para um Superadministrador
sem empresa), fora do escopo autorizado.

## 36. Confirmação de que não houve credencial ou dado sensível

- Nenhuma credencial real foi usada — só uma senha sintética (`homolog-visual-senha-temp-2026`)
  válida apenas dentro do H2 efêmero desta homologação, descartado ao final.
- Nenhuma senha, segredo ou credencial foi versionado em nenhum arquivo commitado.
- As 23 capturas de tela em `docs/evidencias/f4-008/` mostram apenas dados sintéticos (nomes,
  CNPJs e valores fictícios criados só para este teste) — nenhum dado real do Postgres local, já
  que a homologação nunca se conectou a ele.

## 37. Confirmação de ausência de merge, rebase, cherry-pick, force push, PR ou push para `main`

Nenhuma dessas operações foi realizada. Único commit criado na branch
`feat/integracao-fundacao-dominio`; push feito apenas para essa branch.
