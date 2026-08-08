---
name: criati-audit
description: Auditoria independente e somente leitura de uma tarefa Criati já implementada (branch, HEAD, diff). Confirma estado Git, escopo, qualidade, segurança, multiempresa, migrations e testes, e termina sempre com APROVADO PARA COMMIT SIM/NÃO. Use quando pedirem para auditar, revisar ou aprovar o diff de uma tarefa Criati antes do commit.
---

# criati-audit

Procedimento reutilizável de auditoria independente do Criati Engineering
2.0. As regras permanentes (Git, classificação de risco, autonomia,
migrations, segurança, multiempresa) são normativas em
`docs/engenharia/CRIATI_PROTOCOL.md` — leia-o antes de auditar; esta Skill
não repete esse conteúdo. O contexto específico de cada auditoria (ID da
tarefa, branch esperada, `HEAD` esperado, arquivos/diff esperados, objetivo
da alteração) vem sempre de quem invoca a Skill, nunca fica fixado aqui.

## Regra geral

Somente leitura. Nunca editar, stagear, comitar, dar push, merge ou abrir
PR — só relatar e aprovar/reprovar. Auditoria proporcional ao risco real do
diff (Camada 3 do protocolo); não presuma Java/Spring/PostgreSQL nem
qualquer stack específica — aplique só o que o diff realmente tocar. Pare e
reporte divergência material de estado ou autoria indeterminada, sem tentar
corrigir.

## Procedimento

1. **Estado Git** — branch atual, `HEAD`, `git status`, staging, working
   tree, `git diff --stat`; compare com o que a tarefa informou como
   esperado e aponte qualquer divergência antes de prosseguir.
2. **Escopo** — revise o diff integral; liste arquivos alterados fora do
   objetivo declarado; verifique indício de autoria concorrente ou
   incerta.
3. **Qualidade** — `git diff --check`; arquivos gerados por acidente;
   duplicação evidente; inconsistência documental quando aplicável.
4. **Segurança** — verifique, proporcionalmente ao que o diff tocar:
   isolamento multiempresa, autorização, exposição de dado sensível,
   segredos, operação destrutiva, dinheiro, upload/arquivo,
   autenticação/permissão (ver Camada 1 do protocolo).
5. **Banco/migrations** — qualquer migration, schema, tabela, coluna,
   constraint, índice estrutural ou relacionamento persistido classifica a
   tarefa como risco crítico (ver "Promoção automática de risco" do
   protocolo), mesmo que a migration já esteja autorizada.
6. **Testes** — confira o que o implementador executou e se é proporcional
   ao risco; não execute suíte pesada por conta própria; declare
   explicitamente validação ausente, sem presumir que passou.
7. **Git/autonomia** — a Skill nunca altera, stagea, comita ou executa
   operação externa; só relata e decide aprovação.
8. **Resultado** — termine sempre com decisão objetiva. Se reprovado, liste
   bloqueios concretos, separando críticos de não críticos, e indique a
   correção necessária sem executá-la.

## Saída

```text
CRIATI-AUDIT
Status:
Tarefa auditada:
Branch:
HEAD:
Working tree:
Staging:
Diff:
git diff --check:
Escopo:
Testes:
Segurança:
Multiempresa:
Migration/banco:
Git/autonomia:
Contradições:
Problemas críticos:
Problemas não críticos:
Riscos:
Correções necessárias:
APROVADO PARA COMMIT: SIM/NÃO
Justificativa:
```
