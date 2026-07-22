# Backlog Inicial — Financeiro LeS

## Modelo obrigatório de tarefa

Cada especificação futura conterá contexto, objetivo, escopo, módulos/arquivos afetados, regras,
restrições, testes, critérios de aceite, instruções Git e relatório final. O tamanho é relativo:
`pequena`, `média`, `grande` ou `crítica`; risco é `baixo`, `médio` ou `alto`.

## Backlog ordenado

| ID | Entrega coesa | Dependências | Tamanho | Risco |
|---|---|---|---|---|
| LES-F2-001 | Preparação técnica do domínio Financeiro LeS | roadmap aprovado | pequena | médio |
| LES-F2-002 | Pessoa da residência e parte financeira | F2-001, tenant/usuário | média | médio |
| LES-F2-003 | Contas financeiras e saldo inicial | F2-001, F2-002 | média | médio |
| LES-F2-004 | Categorias, hierarquia e preservação histórica | F2-001 | média | médio |
| LES-F2-005 | Lançamentos básicos: receita/despesa, previsto/realizado | F2-002–004 | grande | alto |
| LES-F2-006 | Transferências entre contas próprias | F2-003, F2-005 | média | alto |
| LES-F2-007 | Saldos e resumo financeiro básico | F2-005, F2-006 | média | alto |
| LES-F2-008 | Contas a pagar e pagamentos parcial/integral | F2-002, F2-004, F2-005 | grande | alto |
| LES-F2-009 | Recorrências mensais/anuais idempotentes | F2-008 | grande | alto |
| LES-F2-010 | Comprovantes e anexos privados | F2-005, infraestrutura de arquivos aprovada | grande | alto |
| LES-F3-001 | Cadastro de cartões principal/virtual | F2-002, F2-003 | média | alto |
| LES-F3-002 | Compras no cartão | F2-004, F3-001 | grande | alto |
| LES-F3-003 | Geração e estados de parcelas | F3-002 | crítica | alto |
| LES-F3-004 | Abertura, composição e fechamento de faturas | F3-003 | crítica | alto |
| LES-F3-005 | Pagamentos integral, parcial, mínimo e complementar | F2-003, F3-004 | crítica | alto |
| LES-F3-006 | Estornos e cancelamentos vinculados | F3-002–005 | grande | alto |
| LES-F3-007 | Limite saudável e comprometimento | F3-001–006 | grande | alto |
| LES-F4-001 | Orçamento mensal geral | F2-005–007, F3-004 | grande | alto |
| LES-F4-002 | Limites por categoria | F2-004, F4-001 | média | médio |
| LES-F4-003 | Meta de economia e exceção mensal | F4-001 | grande | alto |
| LES-F4-004 | Dashboard Familiar consolidado | F2-007, F3-007, F4-001–003 | crítica | alto |
| LES-F5-001 | Empréstimos concedidos | F2-002, F2-005 | grande | alto |
| LES-F5-002 | Compromissos a pagar | F2-002, F2-005 | grande | alto |
| LES-F5-003 | Compras para terceiros | F3-002–004, F5-001 | grande | alto |
| LES-F5-004 | Recebíveis e exposição financeira | F5-001–003 | crítica | alto |
| LES-F5-005 | Simulador de Gastos básico | F3-007, F4-001–003, F5-004 | crítica | alto |
| LES-F5-006 | Projeções de 3, 6 e 12 meses | F5-005 | crítica | alto |
| LES-F5-007 | Recomendações explicadas do simulador | F5-005, F5-006 | grande | alto |
| LES-F6-001 | Importação OFX | núcleo estável, modelo de conciliação definido | grande | alto |
| LES-F6-002 | Importação CSV e Excel | F6-001 (contratos comuns) | grande | alto |
| LES-F6-003 | Conciliação manual assistida | F6-001 ou F6-002 | crítica | alto |
| LES-F6-004 | Prevenção de duplicidades | F6-001–003 | grande | alto |
| LES-F6-005 | Regras de categorização | F6-003, F6-004 | grande | médio |

Total inicial: **33 tarefas**. O detalhamento de cada uma será criado apenas quando sua entrada
estiver próxima, evitando especificações obsoletas.

## Limites e observações

- LES-F2-010 só é antecipada se a infraestrutura de armazenamento privado estiver pronta; caso
  contrário, fica após o núcleo de contas a pagar sem bloquear lançamentos básicos.
- Dashboard recebe cards incrementais em F2-007 e F3-007; F4-004 é a consolidação, não seu início.
- PDF/OCR, Google Calendar, mensagens, backup operacional e outras integrações não fazem parte das
  33 tarefas e serão decompostos após o MVP e validação de necessidade.
- LES-F3-003 a F3-005 e LES-F5-004 a F5-006 exigem revisão financeira reforçada por serem críticas.

## Prioridade de negócio refletida

1. receitas e despesas;
2. contas e saldo;
3. cartões;
4. faturas e parcelas;
5. orçamento e meta;
6. risco e simulação;
7. terceiros;
8. importação, conciliação e automação;
9. integrações externas futuras.
