# CRIATI Commands — Contrato conceitual dos comandos operacionais

Tarefa de origem: `CRIATI-ENG-002`.

Este documento define o **contrato operacional** de comandos futuros do
protocolo Criati (Camada "Comandos operacionais futuros" de
[`CRIATI_PROTOCOL.md`](CRIATI_PROTOCOL.md)). Nenhum destes comandos está
implementado como script ou Skill — este arquivo apenas fixa finalidade,
entradas mínimas, comportamento, critérios de parada e saída esperada, para
que a implementação futura (se e quando decidida) não precise redefinir a
política.

Todos os comandos herdam integralmente as Camadas 1 (regras inegociáveis), 2
(ciclo padrão) e 3–4 (risco e autonomia), além das seções "Auditoria
independente" e "Limites de consumo" de `CRIATI_PROTOCOL.md`. Este arquivo
não repete essas regras — apenas descreve o comportamento específico de cada
comando.

## `/criati-task`

- **Finalidade**: iniciar a execução de uma tarefa descrita no formato
  compacto (Camada 5).
- **Entradas mínimas**: ID, objetivo, escopo, fora de escopo, migration
  (proibida/permitida com número), commit (autorizado/não), risco.
- **Comportamento**: confirma ou classifica o risco (Camada 3); resolve o
  nível de autonomia (Camada 4); executa o ciclo padrão (Camada 2) até onde a
  autonomia permitir; aplica promoção automática de risco quando um gatilho
  aparecer durante a execução.
- **Critérios de parada**: qualquer gatilho da Camada 1, ou promoção de risco
  para um nível que exija autonomia manual.
- **Saída esperada**: relatório no formato da Camada 6, com hash de commit
  quando aplicável.

## `/criati-audit`

- **Finalidade**: executar auditoria independente sobre um diff, branch ou
  worktree já produzido, sem reimplementar o trabalho.
- **Entradas mínimas**: identificador da tarefa ou branch/worktree; commit(s)
  ou intervalo a revisar; se a auditoria é obrigatória ou opcional (seção
  "Auditoria independente" do protocolo).
- **Comportamento**: começa sem editar; lê o diff completo; valida riscos
  (multiempresa, segurança, dinheiro, migrations, escopo); executa as
  verificações necessárias (testes, consultas); só corrige após diagnóstico
  claro e apenas com autorização; nunca reescreve por preferência pessoal.
- **Critérios de parada**: achado crítico sem correção possível dentro do
  escopo auditado; autoria incerta; necessidade de decisão humana.
- **Saída esperada**: lista de achados (achado, severidade, evidência,
  recomendação) e veredito — aprovado, aprovado com ressalvas ou reprovado.

## `/criati-migration`

- **Finalidade**: criar e validar uma migration Flyway isolada.
- **Entradas mínimas**: número de migration autorizado; objetivo da mudança
  estrutural; tabelas/colunas afetadas; indicação de dado sensível envolvido.
- **Comportamento**: confirma que o número é o próximo real antes de criar
  (nunca renumera); aplica V1..Vn em PostgreSQL real, em banco previamente
  confirmado como isolado; roda Hibernate `validate`; executa testes SQL
  positivos e negativos de constraint; nunca edita migration já aplicada.
- **Critérios de parada**: número de migration já existente ou conflitante;
  banco de validação não confirmado como isolado; indício de dado real no
  banco de validação; qualquer dúvida sobre destrutividade.
- **Saída esperada**: relatório com versão aplicada, resultado do Hibernate
  `validate`, testes SQL executados e confirmação de que o banco de
  validação ficou limpo ao final.

## `/criati-integrate`

- **Finalidade**: integrar um branch/worktree de tarefa já concluída e
  auditada à branch de integração, sem reimplementar trabalho.
- **Entradas mínimas**: branch/worktree de origem; branch de destino;
  commits a integrar; confirmação de que a tarefa de origem já foi relatada
  e, quando exigido, auditada de forma independente.
- **Comportamento**: compara commits e diff antes de integrar; verifica
  ausência de sobreposição ou autoria incerta; aplica merge/rebase somente
  com autorização explícita para essa operação externa; roda a suíte
  completa após integrar; audita novamente o resultado combinado.
- **Critérios de parada**: conflito estrutural; autoria incerta; falha de
  teste pós-integração; alteração fora do escopo relatado pela tarefa de
  origem.
- **Saída esperada**: relatório de integração (origem, destino, commits,
  conflitos resolvidos, resultado da suíte pós-integração, hash final).

## `/criati-release`

- **Finalidade**: avaliar a prontidão de uma release. Nunca executa
  push, merge, PR, tag ou deploy por conta própria.
- **Entradas mínimas**: branch candidata; versão/tag proposta; lista de
  tarefas/changelog incluídos.
- **Comportamento**: confirma suíte completa aprovada; confirma migrations
  aplicadas e validadas em PostgreSQL real; confirma auditoria de segurança,
  multiempresa e dinheiro para tudo incluído; monta changelog compacto.
- **Critérios de parada**: qualquer item de auditoria pendente; migration não
  validada; suíte com falha; ausência de autorização explícita para a
  operação externa correspondente.
- **Saída esperada**: relatório de prontidão (aprovada ou pendências
  listadas). A operação externa em si (push/tag/deploy) permanece sempre
  manual, separada e fora deste comando.
