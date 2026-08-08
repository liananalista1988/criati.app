# CRIATI Protocol 2.0 — Protocolo operacional multiagente compacto

Tarefa de origem: `CRIATI-OPS-001`. Compactado e reestruturado em camadas por
`CRIATI-ENG-002`.

## Finalidade

Este documento é a fonte central das regras operacionais para pessoas, GPT,
Codex, Claude Code e outros agentes que trabalhem no Criati.app. Ele permite
descrever tarefas futuras com comandos curtos (Camada 5) mantendo as regras
permanentes de Git, testes, auditoria, migrations, multiempresa, segurança e
relatórios centralizadas e sem cópias divergentes.

`AGENTS.md` e `CLAUDE.md` são apenas pontos de entrada; regras operacionais só
mudam aqui. `docs/engenharia/CRIATI_COMMANDS.md` define o contrato conceitual
dos comandos operacionais futuros (Seção "Comandos operacionais futuros").

## Precedência e conflitos

As instruções explícitas da tarefa definem o escopo. Este protocolo governa a
execução; os documentos abaixo governam produto, arquitetura, dados e
segurança — leia o que for aplicável antes de alterar código, banco,
dependências, estrutura ou documentação técnica:

- [Visão do Produto](../VISAO_DO_PRODUTO.md) · [Decisões](../DECISOES.md) ·
  [Arquitetura](../ARQUITETURA.MD) ·
  [Modelo Multiempresa](../MODELO_MULTIEMPRESA.md) ·
  [Escopo do MVP](../ESCOPO_MVP.md) ·
  [Banco de Dados](../BANCO_DE_DADOS.md) · [Segurança](../SEGURANCA.MD) ·
  [Roadmap](../ROADMAP.md) · documentação específica do módulo afetado.

Nenhum agente redefine silenciosamente uma decisão aprovada. Se houver
contradição, ambiguidade estrutural ou risco de sobrescrever instrução
importante, pare antes de editar e solicite decisão.

---

## Camada 1 — Regras inegociáveis

Válidas em qualquer tarefa, qualquer nível de autonomia, qualquer agente:

1. **Main intocável** — nunca altere `main` diretamente.
2. **Sem force push** — nunca use `push --force`.
3. **Sem operação externa sem autorização explícita** — push, merge, PR, tag
   ou deploy só com autorização explícita para aquela ação específica.
4. **Isolamento multiempresa** — toda operação empresarial deriva contexto da
   sessão autenticada; nunca confia em `empresa_id`/`unidade_id` do cliente.
5. **Segurança** — autenticação, autorização, CSRF, segredos e dados sensíveis
   seguem sempre as regras detalhadas abaixo; negar por padrão.
6. **Dinheiro e dados** — valores monetários e integridade de dados nunca são
   sacrificados por conveniência de teste ou prazo.
7. **Migrations** — mudança estrutural sempre por migration Flyway nova,
   pequena e cumulativa; migration aplicada nunca é editada.
8. **Critérios de parada** — pare e peça decisão diante de qualquer gatilho
   listado abaixo; não prossiga por conta própria.
9. **Working tree limpo e rastreável** — sem sobra de alteração fora do
   escopo; sem `git reset --hard`, descarte, limpeza, stash ou qualquer
   operação destrutiva sem autorização e confirmação exata do alvo.
10. **Rastreabilidade** — branch/worktree próprios quando houver trabalho
    concorrente; commits pequenos, coesos e descritivos, sem misturar
    tarefas nem alterações incidentais; `git diff --check` antes de
    concluir; toda decisão relevante registrada no relatório.

Critérios de parada (gatilhos completos):

- conflito estrutural ou entre regras/documentos;
- mais de uma interpretação materialmente relevante ou decisão de negócio
  ausente;
- migration inesperada, fora de sequência, destrutiva ou que exija reescrever
  migration aplicada;
- regra financeira ou de negócio indefinida;
- risco de perda ou corrupção de dados;
- falha de segurança, autorização, privacidade ou vazamento em logs;
- risco de violar isolamento multiempresa;
- dependência insegura ou mudança arquitetural não autorizada;
- trabalho concorrente ou autoria de alterações que não possa ser
  determinada;
- falha real cuja correção amplie materialmente o escopo.

Não pare apenas porque a tarefa é difícil ou demorada — investigue as
alternativas seguras dentro do escopo primeiro.

### Detalhamento — Segurança e multiempresa

Valide sempre usuário, empresa atual, vínculo ativo, unidade, perfil,
permissão, módulo ativo e estado aplicável a partir da sessão. Consultas
empresariais usam recurso mais `empresa_id` atual; UUID isolado não é
autorização; recurso de outra empresa não pode ter existência ou dados
revelados. Autenticação usa Spring Security; senhas usam `PasswordEncoder`
(preferencialmente Argon2id), nunca reversíveis, por e-mail ou logadas. Nunca
registre senha, token, cookie, segredo, chave, documento integral ou conteúdo
financeiro/bancário bruto. Uploads validam nome, extensão, conteúdo, tamanho e
empresa, fora de diretório público. Toda funcionalidade empresarial testa UUID
válido de outra empresa e confirma zero vazamento.

### Detalhamento — Dados, dinheiro e migrations

Valores monetários usam `BigDecimal` com escala/arredondamento explícitos;
regras financeiras nunca mudam para satisfazer teste. Operações financeiras
exigem atomicidade, idempotência e rastreabilidade proporcionais ao risco.
Entidades principais usam UUID, tabelas empresariais usam `empresa_id`,
instantes usam `TIMESTAMPTZ`. Migrations preservam dados e incluem
constraints/chaves/índices necessários, inclusive isolamento multiempresa.
Havendo migration, valide V1..Vn em PostgreSQL real e banco confirmadamente
seguro (nunca H2 como substituto), confirme versão esperada e Hibernate
`validate`. Nunca inclua dado real ou de demonstração de cliente em migration
ou fixture.

### Detalhamento — Testes e definição de pronto

Testes proporcionais ao risco cobrem, quando aplicável: regra de negócio,
limites, erros e rollback; autenticação/autorização/CSRF/permissões;
isolamento multiempresa com recurso válido de outra empresa;
idempotência/concorrência/constraints; logs e respostas sem dado sensível;
regressão dos fluxos relacionados. Windows usa `mvnw.cmd test`; Linux/macOS
usa `./mvnw test`. Testes dependentes de tempo usam relógio controlável ou
datas relativas determinísticas. Tarefa só está pronta quando o requisito foi
atendido, o código compila, as validações aplicáveis passam, segurança e
multiempresa foram auditadas, a documentação está coerente, nenhum segredo
foi incluído e o diff contém apenas o escopo autorizado.

---

## Camada 2 — Ciclo padrão

1. **Diagnosticar** — ler tarefa e documentos relevantes; confirmar
   branch/`HEAD`/`status`/worktrees/commits recentes; registrar riscos,
   suposições e arquivos prováveis.
2. **Implementar** — alterar somente o necessário, preservando arquitetura,
   segurança e trabalho alheio.
3. **Testar focado** — testes a cada incremento relevante; corrigir a causa
   real da falha (máximo 2 ciclos automáticos de correção — ver Limites de
   consumo).
4. **Suíte final** — suíte completa antes do commit quando a mudança puder
   afetar a aplicação; mudança exclusivamente documental valida links,
   referências, diff e escopo em vez de rodar Maven.
5. **Auditar** — segurança, multiempresa, dinheiro, dados, logs, migrations,
   dependências, escopo e arquivos inesperados; ver Auditoria independente
   para quando a auditoria precisa ser cruzada.
6. **Comitar quando autorizado** — um único commit coeso, salvo instrução
   diferente; nunca comitar sem autorização explícita.
7. **Relatar** — formato compacto da Camada 6.

---

## Camada 3 — Classificação de risco

| Risco | Características |
|---|---|
| **Pequena** | documentação, CSS/texto isolado, ajuste visual, correção pontual, teste isolado; sem migration; sem regra de negócio nova. |
| **Média** | funcionalidade backend/tela/service/integração interna nova ou alterada; sem dinheiro, sem migration nem alteração estrutural de banco (ver "Promoção automática de risco" — autorização não reduz o risco), sem tocar segurança/autenticação/autorização/multiempresa. |
| **Crítica** | qualquer gatilho de "Promoção automática de risco" abaixo: migration, dinheiro, autenticação, autorização, multiempresa, dados, concorrência relevante, integração estrutural, infraestrutura compartilhada ou produção. |

A classificação é proposta pela tarefa e confirmada pelo agente durante o
diagnóstico; discrepância segue "Promoção automática de risco" abaixo.

## Camada 4 — Níveis de autonomia

### Manual

Usar quando houver: produção; ação destrutiva; credencial; infraestrutura
compartilhada; migration ambígua; risco financeiro relevante; decisão de
produto; conflito de integração; dados reais; push/merge/PR.

O agente **para e pede autorização** antes de agir.

### Assistido

Usar para: funcionalidades médias; backend; telas; services; integrações
internas; migration já autorizada; tarefas de risco controlado.

O agente executa o ciclo completo — diagnóstico → implementação → testes →
auditoria → commit — e para nos critérios de parada da Camada 1.

### Automático controlado

Usar para: documentação; testes; lint; ajustes visuais simples; correções
pequenas; padronização; auditoria mecânica.

O agente pode concluir e comitar sozinho, **sem push**.

Nunca usar automático controlado para: dinheiro; autenticação; autorização;
multiempresa; migration; infraestrutura; dados reais; exclusão de dados;
produção — essas áreas sempre promovem a tarefa a crítica (ver "Promoção
automática de risco") e exigem, no mínimo, autonomia assistida.

Independentemente do nível de autonomia, push, merge e PR sempre exigem
autorização explícita (Camada 1) — nenhum nível, nem o automático
controlado, dispensa essa autorização.

## Camada 5 — Formato compacto de tarefa

Identificador canônico: `CRIATI-<DOMÍNIO>-<TIPO>-<NÚMERO>` (domínio e tipo em
maiúsculas, número de três dígitos; sufixo alfabético como
`CRIATI-FIN-FEAT-015A` só para extensão explicitamente relacionada à tarefa
original). IDs históricos (dois ou três blocos, ex.: `CRIATI-OPS-002`,
`CRIATI-WRK-001`) permanecem válidos como referência legada e não são
renomeados; tarefas novas sempre usam o formato canônico.

Vocabulário controlado de `<TIPO>`: `FEAT` funcionalidade · `FIX` correção ·
`DOC` documentação/protocolo · `AUDIT` auditoria · `TEST` testes · `SEC`
segurança · `DB` banco/migration · `UX` experiência/interface · `CI`
integração/qualidade · `OPS` operação/engenharia. Evite sinônimos arbitrários
para o mesmo significado; tipo não coberto é definido pelo GPT antes da
execução.

```text
ID: CRIATI-XXX-XXX-000
Objetivo: <resultado esperado em 1 frase>
Escopo: <arquivos/módulos autorizados>
Fora de escopo: <o que não deve ser tocado>
Migration: proibida | permitida (V<n>)
Commit: autorizado | não autorizado
Risco: pequena | média | crítica
Critérios de aceite: <o que precisa ser verdadeiro para concluir>
```

O protocolo fornece o restante automaticamente: ciclo (Camada 2), gates de
risco/autonomia (Camadas 3 e 4), critérios de parada e formato de relatório
(Camada 6). Campos adicionais (base/branch/`HEAD` esperados, decisões
obrigatórias, testes específicos) só são necessários quando divergem do
padrão.

## Camada 6 — Formato compacto de relatório

```text
Diagnóstico: <estado inicial e decisões de escopo>
Arquivos: <lista ou resumo do diff>
Testes: <focados + suíte completa, contagens e falhas; PostgreSQL/migration quando aplicável>
Auditoria: <segurança, multiempresa, dinheiro, dados, logs, escopo>
Commit: <hash, branch, HEAD final; operações externas realizadas ou não>
Riscos: <limitações e pendências reais>
Estado Git: <working tree, resumo do diff --check>
```

Nunca omita falha nem declare concluída uma validação que não pôde ser
executada.

---

## Promoção automática de risco

- Pequena pode virar média; média pode virar crítica.
- Crítica nunca é rebaixada automaticamente.
- Migration, estrutura persistente (schema, tabela, coluna, constraint,
  índice estrutural, relacionamento persistido), dinheiro, autenticação,
  autorização, multiempresa, dados, concorrência relevante, integração
  estrutural, infraestrutura compartilhada e produção sempre promovem a
  tarefa a crítica; autorização para executar migration não reduz essa
  classificação.
- Ao promover: registre o motivo, aplique os gates da nova classificação
  (Camadas 3 e 4) e **não continue silenciosamente** se a nova classificação
  exigir autorização humana — pare e informe.

## Templates de comando curto

**Pequena**

```text
Execute CRIATI-XXX-XXX-000.
Tipo: pequena.
Objetivo: corrigir alinhamento do cabeçalho.
Migration: proibida.
Commit: autorizado.
```

**Média**

```text
Execute CRIATI-XXX-XXX-000.
Tipo: média.
Objetivo: criar endpoint e tela de cadastro.
Migration: proibida.
Auditoria independente: não obrigatória.
Commit: autorizado.
```

**Crítica**

```text
Execute CRIATI-XXX-XXX-000.
Tipo: crítica.
Objetivo: adicionar persistência de regras.
Migration permitida: V26.
PostgreSQL real: obrigatório.
Auditoria independente: obrigatória.
Commit: autorizado.
Push: proibido.
```

## Comandos operacionais futuros

Contrato conceitual — sem script e sem Skill instalada ainda — definido em
[`CRIATI_COMMANDS.md`](CRIATI_COMMANDS.md): `/criati-task`, `/criati-audit`,
`/criati-migration`, `/criati-integrate`, `/criati-release`.

Mecanismo previsto: uma Skill encapsula o procedimento reutilizável (os passos
fixos de um dos comandos acima); a tarefa que a invoca fornece apenas contexto
e objetivo específicos, no formato compacto da Camada 5, sem repetir o
procedimento. Os cinco comandos de `CRIATI_COMMANDS.md` são as candidatas
atuais a Skill; nenhuma é implementada por este protocolo.

## Auditoria independente

**Obrigatória para**: migration; financeiro; autenticação; autorização;
multiempresa; concorrência; integração estrutural; infraestrutura; produção.

**Opcional para**: documentação; CSS; texto; teste isolado; ajuste visual
pequeno.

A auditoria independente:

- começa sem editar;
- revisa o diff completo;
- valida riscos (segurança, multiempresa, dinheiro, dados, escopo);
- executa as verificações necessárias (testes, consultas, queries);
- só corrige após diagnóstico claro, e apenas quando autorizado;
- não reescreve trabalho alheio por preferência pessoal.

## Limites de consumo

- Não repita contexto já documentado; não cole arquivos inteiros no
  relatório; não explique código óbvio — use hashes, diffs e resultados.
- Máximo de 2 ciclos automáticos de correção; após 2 falhas, pare e reporte.
- Suíte completa uma vez ao final, salvo regressão relevante; testes focados
  durante a implementação.
- Não use dois agentes na mesma tarefa sem justificativa.
- Não execute auditoria cruzada por padrão em tarefa pequena (ver Auditoria
  independente).
- Relatório compacto por padrão (Camada 6).
- Não invente medição real de tokens.

---

## Multiagente e compatibilidade

- GPT e Codex entram por `AGENTS.md`; Claude Code entra por `CLAUDE.md`.
  Ambos exigem leitura integral deste protocolo antes de qualquer tarefa.
  Todos os agentes obedecem às mesmas regras — nenhum modelo relaxa
  segurança, Git, testes, escopo ou decisões aprovadas.
- Responsabilidade é definida pela tarefa, não pelo modelo: um único agente é
  responsável ativo por tarefa, branch/worktree e conjunto de arquivos.
  Outro agente só assume após passagem de trabalho explícita ou tarefa
  independente e não sobreposta.
- Antes de assumir uma tarefa: confira branch/`HEAD`/worktrees/status/commits
  recentes; identifique o que está sob sua responsabilidade; verifique se a
  mudança já existe em outro commit/branch; evite editar o mesmo
  worktree/arquivos que outro agente; não reaplique correção já existente.
- Na passagem de trabalho, informe no mínimo os campos da Camada 6
  (diagnóstico, arquivos, testes, auditoria, commit, riscos, estado Git) mais
  o identificador da tarefa e o `HEAD` base. O agente receptor compara
  commits e diff antes de integrar.
- Conflitos só se resolvem com a intenção de ambos os lados plenamente
  compreendida; pare diante de conflito estrutural ou autoria incerta.
- As regras centrais não usam sintaxe exclusiva de uma ferramenta. Quando uma
  adaptação for necessária para um agente específico, ela fica documentada
  separadamente (ex.: `AGENTS.md`/`CLAUDE.md`), nunca reescrevendo a regra
  central.

## Arquitetura, persistência e integrações

- `Criati`, group `br.app`, artifact `criati`, package base `br.app.criati`,
  Java 17, Maven, Spring Boot 3.x, Thymeleaf/HTML/CSS/JS, PostgreSQL,
  monólito modular. Fluxo padrão `Controller → Service → Repository → Banco`;
  controllers não acessam repositories diretamente. DTO não é entidade;
  cliente não define campos administrativos internos. Não adicione
  dependência, tecnologia, serviço pago ou mudança de arquitetura sem
  aprovação; não altere identidade visual, módulos ou documentação aprovada
  fora do escopo.
- Repositories empresariais consultam por identificador mais empresa atual
  (`findById`/`findAll` sem escopo exige justificativa administrativa
  explícita). Entidades principais usam UUID; evite `FetchType.EAGER`,
  `CascadeType.ALL` e Lombok `@Data` sem análise; não inclua relacionamento
  sensível em `toString`. Use `@Transactional` conscientemente
  (`readOnly = true` em leitura) e avalie `@Version` em fluxo concorrente.
  Erros ao usuário não expõem stack trace, SQL, caminho local, tabela,
  credencial ou dado de outra empresa.
- Thymeleaf mantém escape de HTML, evita `th:utext`, inclui CSRF em
  formulário e nunca é a única camada de autorização. JavaScript não
  armazena segredo nem duplica regra de negócio; validação no navegador
  nunca substitui a do backend. E-mail nunca transporta senha; tokens são
  aleatórios, temporários, de uso único, não logados integralmente.
  Credenciais usam variáveis de ambiente, nunca versionadas. Mudança de
  comportamento relevante exige documentação consistente, sem alterar
  decisão aprovada silenciosamente.
- **Exigem aprovação prévia**: microsserviços, banco por empresa, comunicação
  distribuída, novo framework frontend, aplicativo móvel, gateway de
  pagamento, integração com WhatsApp, inteligência artificial,
  Row-Level Security, autenticação externa, domínio personalizado, serviço
  pago. Proposta explica problema, alternativas, recomendação, impactos,
  riscos e esforço.

## Princípios de execução

- Trabalhe em modo econômico, seguro e compacto; faça somente o escopo
  solicitado — melhorias adicionais são propostas separadamente.
- Preserve mudanças existentes do usuário e de outros agentes.
- Prefira inspeção objetiva, alterações pequenas e validação proporcional ao
  risco.
- Não esconda erro, não mascare teste, não declare sucesso sem evidência.
- Código e identificadores em português, salvo termo técnico consolidado;
  documentação, comunicação e commits em português claro.
- Nunca exponha credencial, token, dado pessoal, conteúdo bancário ou outro
  dado sensível em código, fixture, log, commit ou relatório.

## Onde alterar o quê

Regras operacionais mudam apenas neste arquivo. `AGENTS.md` e `CLAUDE.md`
continuam pontos de entrada compactos, sem duplicar o protocolo.
`docs/engenharia/CRIATI_COMMANDS.md` é o único arquivo adicional preferencial
para o contrato dos comandos operacionais futuros — evite criar mais
arquivos de protocolo.
