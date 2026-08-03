# Criati Engineering 2.0

Última atualização: **2026-08-02**  
Progresso oficial: **18%**

## Finalidade

Esta é a referência operacional de evolução da Criati Engineering 2.0. O
[CRIATI Protocol](CRIATI_PROTOCOL.md) continua sendo a fonte normativa; este
documento apenas organiza medição, classificação e orçamento. O registro por
tarefa está em [Engineering Metrics](ENGINEERING_METRICS.md).

## Baseline do processo atual

Uma tarefa passa, conforme seu risco, por planejamento e escolha do agente,
diagnóstico, implementação, testes focados, suíte completa, PostgreSQL real
quando aplicável, auditoria, commit, integração e revisão humana. Push, merge e
PR somente ocorrem mediante autorização. A ordem, os critérios de parada e as
responsabilidades são os definidos no protocolo, sem regras paralelas aqui.

## Classificação

| Nível | Escopo típico | Gatilhos |
|---|---|---|
| **PEQUENA** | CSS, texto, documentação, teste isolado ou correção localizada sem domínio | Um arquivo ou conjunto pequeno, impacto local e reversível |
| **MÉDIA** | Endpoint, tela, serviço, funcionalidade sem migration ou vários arquivos relacionados | Impacto funcional delimitado, sem gatilho crítico |
| **CRÍTICA** | Mudança de alto risco ou integração estrutural | Migration, dinheiro, autenticação, autorização, multiempresa, dados, concorrência, infraestrutura ou produção |

A classificação pode ser promovida automaticamente `PEQUENA → MÉDIA → CRÍTICA`
quando o diagnóstico revelar risco maior. Nunca há rebaixamento automático de
uma tarefa crítica; eventual reclassificação exige justificativa humana
registrada.

## Orçamento operacional inicial

| Nível | Agentes e validação esperada |
|---|---|
| **PEQUENA** | Um agente, testes focados e suíte final somente quando o protocolo exigir; sem auditoria cruzada por padrão |
| **MÉDIA** | Um implementador, testes focados, suíte completa final e auditoria independente quando o risco justificar |
| **CRÍTICA** | Implementação, testes focados, suíte completa, auditoria independente e PostgreSQL real quando aplicável; intervenção humana nos critérios de parada |

Um ciclo de correção é uma tentativa completa de corrigir a mesma causa seguida
da repetição da validação que falhou. Após **dois ciclos sem solução**, o agente
para e apresenta diagnóstico, evidências e opções. Critérios de parada do
protocolo continuam imediatos e não aguardam dois ciclos. O limite pode ser
recalibrado depois de dados reais; não autoriza reduzir testes ou segurança.

## Medidor Engineering 2.0

| Etapa | Peso | Crédito atual | Estado |
|---|---:|---:|---|
| 1. Baseline e métricas | 10% | 6% | Em andamento: modelo e baseline histórico criados; falta uso prospectivo e calibração |
| 2. Controle de custo/eficiência | 10% | 2% | Em andamento: score e orçamento definidos; falta coleta e calibração reais |
| 3. Protocolo 2.0 compacto | 15% | 6% | Em andamento: protocolo central existe, mas ainda não é a versão 2.0 compacta |
| 4. Skills/comandos Codex + Claude | 15% | 0% | Próxima |
| 5. Níveis de autonomia | 10% | 2% | Parcial: autorizações e paradas existem, sem níveis formalizados |
| 6. Auditoria independente | 10% | 2% | Parcial: auditoria está no ciclo, sem capacidade independente padronizada |
| 7. CI automatizada | 10% | 0% | Próxima |
| 8. Playwright E2E essencial | 10% | 0% | Próxima |
| 9. Teste em tarefas reais | 5% | 0% | Bloqueada pelas capacidades anteriores |
| 10. Comparação antes × depois | 5% | 0% | Bloqueada por amostra futura suficiente |
| **Total** | **100%** | **18%** | — |

O baseline oficial anterior de 12% foi mapeado conservadoramente em: etapa 1
(2%), etapa 3 (6%), etapa 5 (2%) e etapa 6 (2%). Esta tarefa acrescenta quatro
pontos verificáveis à etapa 1 e dois à etapa 2. Ela não conclui nenhuma das duas
etapas e, por isso, o total não sobe automaticamente para 22%.

Percentual só aumenta quando a capacidade existe e, quando aplicável, foi
testada. Criar mais documentação, por si só, não gera crédito.

### Próximas evidências necessárias

- usar o registro em tarefas pequenas, médias e críticas reais;
- calibrar score e limite de ciclos com uma amostra comparável;
- automatizar coleta sem alterar o significado das métricas;
- implementar protocolo compacto, níveis de autonomia e auditoria independente;
- somente depois avançar CI, Playwright e comparação antes × depois.

Bloqueio atual: não existe telemetria confiável de tokens, interações e duração
de ponta a ponta.

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
