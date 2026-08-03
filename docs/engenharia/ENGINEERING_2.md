# Criati Engineering 2.0

Última atualização: **2026-08-02**
Progresso oficial: **38%** (anterior: 18%)

## Finalidade

Esta é a referência de objetivo, etapas, progresso, estado atual, próximos
passos e bloqueios do programa Criati Engineering 2.0. O
[CRIATI Protocol](CRIATI_PROTOCOL.md) continua sendo a fonte normativa
(classificação de risco, autonomia, ciclo); o registro por tarefa, o score e o
orçamento operacional estão em
[Engineering Metrics](ENGINEERING_METRICS.md).

## Baseline do processo atual

Uma tarefa passa, conforme seu risco, por planejamento e escolha do agente,
diagnóstico, implementação, testes focados, suíte completa, PostgreSQL real
quando aplicável, auditoria, commit, integração e revisão humana. Push, merge e
PR somente ocorrem mediante autorização. A ordem, os critérios de parada e as
responsabilidades são os definidos no protocolo, sem regras paralelas aqui.

## Classificação

PEQUENA, MÉDIA e CRÍTICA são normativas em
[CRIATI Protocol](CRIATI_PROTOCOL.md), Camada 3 ("Classificação de risco") e
"Promoção automática de risco" — inclusive a lista oficial de gatilhos que
tornam uma tarefa CRÍTICA e a regra de que crítica nunca é rebaixada
automaticamente. Este documento usa os três mesmos níveis apenas para medir
progresso; não os redefine.

Orçamento operacional (agentes e validação esperada por nível, limite de
ciclos de correção) está em
[Engineering Metrics](ENGINEERING_METRICS.md#orçamento-operacional).

## Medidor Engineering 2.0

| Etapa | Peso | Crédito atual | Estado |
|---|---:|---:|---|
| 1. Baseline e métricas | 10% | 7% | Em andamento: modelo, baseline histórico e primeiro uso prospectivo (registro da própria CRIATI-ENG-003); falta amostra comparável e calibração |
| 2. Controle de custo/eficiência | 10% | 3% | Em andamento: score e orçamento definidos, orçamento consolidado em local único; falta coleta e calibração em amostra real |
| 3. Protocolo 2.0 compacto | 15% | 15% | Concluída: `CRIATI_PROTOCOL.md` reestruturado em camadas, com formatos compactos de tarefa/relatório, integrado à branch de integração |
| 4. Skills/comandos Codex + Claude | 15% | 0% | Próxima: `CRIATI_COMMANDS.md` define o contrato conceitual dos comandos, mas nenhuma Skill foi implementada |
| 5. Níveis de autonomia | 10% | 10% | Concluída: Manual, Assistido e Automático controlado formalizados e alinhados com classificação e orçamento |
| 6. Auditoria independente | 10% | 3% | Parcial: critérios obrigatória/opcional agora formalizados no protocolo e um primeiro ensaio de auditoria em duas passagens foi registrado (CRIATI-ENG-003); segue sem capacidade real de revisão cruzada por outro agente |
| 7. CI automatizada | 10% | 0% | Próxima |
| 8. Playwright E2E essencial | 10% | 0% | Próxima |
| 9. Teste em tarefas reais | 5% | 0% | Bloqueada: um único registro documental não equivale a teste representativo em tarefa real de cada classe |
| 10. Comparação antes × depois | 5% | 0% | Bloqueada por amostra futura suficiente |
| **Total** | **100%** | **38%** | — |

Percentual anterior: 18%. Percentual novo: 38% (+20 pontos). A CRIATI-ENG-003
integrou o protocolo compacto e os níveis de autonomia da ENG-002 à branch
principal — documentação normativa implementada conclui as etapas 3 e 5
(+9 e +8). As etapas 1, 2 e 6 recebem incremento pequeno e explícito pelo
primeiro uso prospectivo do registro e pela formalização dos critérios de
auditoria (+1 cada). Comandos desenhados (etapa 4) não equivalem a Skill
implementada e CI/Playwright (etapas 7–8) seguem não implementados: todos
permanecem em 0%, sem elevação artificial.

Próximo marco: implementar ao menos um comando (`/criati-audit` ou
`/criati-task`) como Skill real e obter uma auditoria independente executada
por um segundo agente — as duas lacunas que hoje seguram as etapas 4 e 6.

Percentual só aumenta quando a capacidade existe e, quando aplicável, foi
testada. Criar mais documentação, por si só, não gera crédito.

### Próximas evidências necessárias

- usar o registro em tarefas pequenas, médias e críticas reais além da
  CRIATI-ENG-003, até formar amostra comparável;
- calibrar score e limite de ciclos com essa amostra;
- automatizar coleta sem alterar o significado das métricas;
- implementar comandos como Skill real e obter auditoria independente por um
  segundo agente (não simulada pelo mesmo agente);
- somente depois avançar CI, Playwright e comparação antes × depois.

Bloqueio atual: não existe telemetria confiável de tokens, interações e duração
de ponta a ponta; auditoria independente real depende de um segundo agente,
indisponível neste ambiente de execução.

## Preparação para automação

Coletores futuros devem preencher o mesmo registro manual, sem criar uma segunda
fonte:

- **Git:** branch, HEAD inicial/final, arquivos, diff e commit;
- **testes:** comandos, contagem, resultado, falhas e duração observada;
- **CI:** build, PostgreSQL, lint e Playwright;
- **agente:** tarefa, classificação, ciclos, retomadas e resultado.

Esta etapa não implementa CI, Playwright ou skills. Coletores nunca devem ler ou
persistir senha, token, cookie, chave, conteúdo bancário ou outra credencial.

## Uso responsável

As métricas apoiam comparação interna entre tarefas semelhantes. Não medem
produtividade absoluta, não substituem revisão de qualidade e não devem induzir
redução de testes, ocultação de falhas ou fragmentação artificial de tarefas.
Campos não observáveis permanecem `INDISPONIVEL`.
