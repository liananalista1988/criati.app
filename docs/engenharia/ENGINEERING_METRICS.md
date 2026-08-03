# Engineering Metrics

Formato manual mínimo para comparar tarefas semelhantes da Criati Engineering
2.0. Política, classificação, orçamento e medidor estão em
[Criati Engineering 2.0](ENGINEERING_2.md); regras normativas permanecem no
[CRIATI Protocol](CRIATI_PROTOCOL.md).

## Registro compacto por tarefa

Use datas ISO 8601 quando observáveis. Contagens começam em zero. Não estime
tokens, duração ou interações ausentes e nunca registre segredos ou credenciais.

```yaml
id: CRIATI-XXX-000
classificacao: PEQUENA|MÉDIA|CRÍTICA
agente: <nome>|INDISPONIVEL
inicio: <ISO-8601>|INDISPONIVEL
fim: <ISO-8601>|INDISPONIVEL
duracao_aprox: <minutos/faixa>|INDISPONIVEL
mudancas: {arquivos: 0, adicionadas: 0, removidas: 0}
testes_focados: [<comando e resultado>]|INDISPONIVEL
suite: {execucoes: 0, resultado_final: <aprovados/falhas>|NAO_APLICAVEL|INDISPONIVEL}
postgresql_real: SIM|NAO|NAO_APLICAVEL|INDISPONIVEL
migration: SIM|NAO
ciclos_correcao: 0|INDISPONIVEL
retrabalho: SIM|NAO|INDISPONIVEL
auditoria_independente: SIM|NAO|NAO_APLICAVEL|INDISPONIVEL
commit: <hash>|SEM_COMMIT|INDISPONIVEL
resultado: CONCLUIDA|PARCIAL|BLOQUEADA|FALHOU
bloqueios: []
contexto: {comando: CURTO|MEDIO|GRANDE|INDISPONIVEL, interacoes_humanas: 0|INDISPONIVEL, perguntas_agente: 0|INDISPONIVEL, retomadas: 0|INDISPONIVEL, agente_adicional: SIM|NAO|INDISPONIVEL}
TOKEN_REAL: INDISPONIVEL
score: 0..100|NAO_CALCULADO
```

`duracao_aprox` deve vir de timestamps observáveis e pode usar uma faixa. Horário
de commit isolado não informa início nem duração. `agente` também não deve ser
inferido do autor Git.

## Score de eficiência

Calcular somente quando os insumos necessários forem observáveis:

```text
score = conclusão + validação + execução + auditoria

conclusão (0–40): concluída 40; parcial 20; bloqueada/falhou 0
validação (0–25): todas as exigidas passaram 25; parcial 15; falha final 0
execução (0–20): começa em 20, piso zero
  -5 por ciclo de correção
  -5 se houve retrabalho
  -3 por execução de validação que falhou por defeito da implementação
  -3 por interação humana evitável além da instrução inicial
  -3 por retomada de contexto
auditoria (0–15): independente realizada quando exigida 15; autoauditoria
  suficiente quando a independente não era exigida 15; parcial 10;
  ausente quando exigida 0
```

Auditoria necessária não é penalidade: sua ausência é que perde pontos. Uma
parada correta por segurança pode ter score baixo sem representar trabalho ruim.
Testes negativos esperados, falhas externas e intervenções obrigatórias por
critério de parada são registrados, mas não recebem essas deduções.
O score compara tarefas de mesma classe e natureza; não é medida absoluta de
produtividade. Se um campo necessário estiver `INDISPONIVEL`, usar
`NAO_CALCULADO`, sem completar lacunas por suposição.

## Baseline histórico verificável

Estatísticas obtidas de commits/ranges Git. “Fim” e duração não são derivados da
data do commit; ela é mostrada apenas como evidência temporal do próprio commit.

| Tarefa | Classe | Evidência Git | Commit em | Arquivos | `+` / `-` | Migration | Resultado observável |
|---|---|---|---|---:|---:|---|---|
| CRIATI-FIN-014A | CRÍTICA (dinheiro) | `5919f978` | 2026-08-02T19:45:48-03:00 | 13 | 580 / 112 | Não | Commit presente |
| CRIATI-PLAT-001/002 | CRÍTICA (autorização, multiempresa e estrutura modular) | `5919f978..94f6fe8b` | 2026-08-02T20:42:42-03:00 | 25 | 727 / 169 | Não | Dois commits presentes |
| CRIATI-WRK-001/002 | CRÍTICA (dados, multiempresa e migration) | `5919f978..a69bd298` | 2026-08-02T21:14:16-03:00 | 50 | 4713 / 2 | Sim, V25 | Dois commits presentes |
| CRIATI-INT-001 | CRÍTICA (integração estrutural) | `5919f978..fb9a43e0` | 2026-08-02T22:01:28-03:00 | 76 | 5625 / 192 | Sim, V25 no lote | Cinco commits presentes |

Para essas tarefas, o Git atual não comprova agente executor, início, fim real,
duração, comandos e execuções de testes, PostgreSQL real, ciclos, retrabalho,
auditoria independente, tamanho do comando, interações, perguntas, retomadas,
agente adicional ou tokens. Esses campos ficam `INDISPONIVEL` e os scores
históricos ficam `NAO_CALCULADO`. A presença do commit prova integração no Git,
não aprovação de todas as validações operacionais.

## Revisão periódica

Após uma amostra mínima de tarefas reais comparáveis, revisar limites e score.
Não mudar fórmula retroativamente sem registrar a versão usada, e nunca otimizar
o número à custa de qualidade, segurança ou transparência sobre falhas.
