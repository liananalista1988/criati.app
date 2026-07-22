# Roadmap Técnico — Financeiro LeS

## Objetivo e limites

Este roadmap transforma as definições das tarefas LES-F1-001 a LES-F1-004 em entregas técnicas
incrementais. A arquitetura permanece um monólito modular: núcleo financeiro compartilhado para
contas, categorias e lançamentos; extensões específicas para cartões, faturas, parcelas,
orçamento, terceiros, conciliação e Simulador de Gastos.

Cada fase só começa após cumprir sua entrada e termina com testes automatizados, homologação no
navegador e isolamento por empresa comprovados. O dashboard evolui junto com os dados disponíveis.
OCR, PDF, integrações externas e automação avançada não antecedem um núcleo confiável.

## Princípios de execução

1. Fundação, contexto empresarial e invariantes vêm antes dos módulos dependentes.
2. Cada incremento entrega comportamento verificável, sem separar artificialmente as camadas.
3. Persistência e regra precedem ou acompanham a tela; não há tela sem caso de uso protegido.
4. Migrations são pequenas, coesas, cumulativas e nunca reescritas após aplicadas.
5. Toda entidade empresarial nasce com `empresa_id`; consultas usam recurso e empresa atual.
6. Testes são parte da tarefa, incluindo UUID válido pertencente a outra empresa.
7. Compra, consumo, fatura e saída de caixa permanecem conceitos distintos.
8. Transferência própria e pagamento de fatura não geram novo consumo.
9. Dados reais não entram em migration, fixture versionada ou evidência pública.
10. Simulação, importação e automação só usam regras e dados previamente homologados.

## Fases, entradas e saídas

### Fase 1 — Fundação do domínio Financeiro LeS

- **Entrada:** núcleo SaaS disponível; arquitetura funcional aprovada; LES-F2-001 definida.
- **Entregas:** fronteiras de pacote, convenções, matriz de reaproveitamento, estratégia
  multiempresa, preparação incremental para pessoa, parte financeira, auditoria e lixeira.
- **Testes mínimos:** arquitetura, contexto empresarial e padrão de acesso cruzado.
- **Homologação:** revisão técnica sem criar regra financeira complexa ou tabelas em massa.
- **Riscos:** duplicar o módulo existente ou acoplar o núcleo às extensões residenciais.
- **Saída:** contratos e limites aprovados; primeira migration funcional delimitada.

### Fase 2 — Empresa e dados iniciais

- **Entrada:** fundação concluída; fluxo seguro de empresa, usuário e aplicação disponível.
- **Entregas:** empresa Financeiro LeS criada pelo fluxo autorizado; dois vínculos; pessoas,
  partes financeiras, contas, categorias e configurações cadastráveis.
- **Testes mínimos:** vínculos, autorização, validações e segregação integral entre empresas.
- **Homologação:** primeiro acesso e cadastro inicial com dados fictícios controlados.
- **Riscos:** seed com dados pessoais ou vínculo indevido entre usuário e pessoa.
- **Saída:** cadastros-base utilizáveis sem dado real versionado.

### Fase 3 — Receitas, despesas e transferências

- **Entrada:** contas e categorias ativas; contexto empresarial validado.
- **Entregas:** lançamentos previstos/realizados, competência/liquidação, filtros, histórico,
  lixeira, transferência de duas pontas e resumo inicial.
- **Testes mínimos:** saldos, transações, cancelamento, dupla contabilização e acesso cruzado.
- **Homologação:** registrar receita, despesa e transferência e conferir saldos manualmente.
- **Riscos:** confundir transferência com receita/despesa ou competência com caixa.
- **Saída:** saldos por conta e consolidado reproduzíveis por cálculo independente.

### Fase 4 — Contas a pagar e recorrências

- **Entrada:** lançamentos e partes financeiras confiáveis.
- **Entregas:** obrigação única, fixa, variável e anual; ocorrências; pagamento integral/parcial;
  vencimentos, comprovantes e agenda básica.
- **Testes mínimos:** geração idempotente, mês sem dia configurado, pagamentos e anexos privados.
- **Homologação:** cenários de conta única, recorrente, atrasada e parcialmente paga.
- **Riscos:** duplicar ocorrências ou misturar obrigação com sua liquidação.
- **Saída:** agenda e saldos refletem obrigações sem duplicidade.

### Fase 5 — Cartões de crédito

- **Entrada:** pessoas e contas funcionando; isolamento comprovado.
- **Entregas:** cartão principal e virtual, titularidade, bandeira, conta pagadora, fechamento,
  vencimento, limite bancário e limite saudável.
- **Testes mínimos:** vínculo virtual/principal da mesma empresa, datas e limites.
- **Homologação:** cadastro e edição responsivos com valores fictícios.
- **Riscos:** permitir cartão virtual com limite ou fatura independentes indevidamente.
- **Saída:** cartões válidos e prontos para receber compras.

### Fase 6 — Compras, parcelas e faturas

- **Entrada:** cartões, categorias e pessoas homologados.
- **Entregas:** compras, parcelas determinísticas, faturas por cartão principal, fechamento,
  estornos, cancelamentos e pagamentos integral, parcial, mínimo e complementar.
- **Testes mínimos:** arredondamento, unicidade, ciclos, virtual/principal, estorno, saldo
  financiado e ausência de novo consumo no pagamento.
- **Homologação:** jornadas completas com totais recalculados manualmente.
- **Riscos:** fase crítica; dupla contagem, parcela em duas faturas ou recálculo destrutivo.
- **Saída:** consumo, compromisso e caixa conciliáveis e auditáveis.

### Fase 7 — Orçamento e meta de economia

- **Entrada:** receitas, despesas, compras, parcelas e faturas confiáveis.
- **Entregas:** orçamento geral e por categoria, realizado, comprometido, disponível, limites,
  meta percentual/fixa, exceção mensal e alertas.
- **Testes mínimos:** fórmulas documentadas, exclusão lógica e limites de competência.
- **Homologação:** comparação com planilha de valores controlados.
- **Riscos:** usar caixa onde a regra exige consumo ou fixar percentuais como constantes.
- **Saída:** orçamento e meta explicáveis para uma competência.

### Fase 8 — Dashboard Familiar

- **Entrada:** indicadores de cada fase validados; esta fase consolida a evolução progressiva.
- **Entregas:** contas, consumo, caixa, cartões, faturas, parcelas, orçamento, meta, variação e
  alertas com filtros consistentes.
- **Testes mínimos:** consultas multiempresa, fórmulas, estados vazios e permissões.
- **Homologação:** desktop e mobile; números confrontados com as telas de origem.
- **Riscos:** criar agregações antes dos dados ou esconder divergências em gráficos.
- **Saída:** cada indicador é rastreável e possui explicação textual/tabela equivalente.

### Fase 9 — Terceiros e compromissos

- **Entrada:** núcleo de lançamentos, parcelas e partes financeiras estável.
- **Entregas:** compromissos a pagar, empréstimos concedidos, compras para terceiros, recebíveis,
  atrasos, juros, multas e exposição financeira separada do consumo residencial.
- **Testes mínimos:** saldos, classificação de excedentes, estados e isolamento.
- **Homologação:** conceder, receber parcial/integralmente e acompanhar atraso.
- **Riscos:** tratar reembolso como receita comum ou misturar dívida e consumo.
- **Saída:** total a pagar/receber e exposição conferíveis.

### Fase 10 — Simulador de Gastos

- **Entrada:** orçamento, meta, faturas, parcelas, compromissos, recebíveis e projeções confiáveis.
- **Entregas:** débito, crédito, parcelamento, empréstimo e terceiro; horizontes de 3, 6 e 12
  meses; risco e recomendação numérica explicada.
- **Testes mínimos:** cenários-limite, determinismo, níveis de risco e não persistência acidental.
- **Homologação:** comparação com cálculos manuais e cenários conhecidos.
- **Riscos:** recomendação opaca ou simulador anterior às regras que o alimentam.
- **Saída:** mesma entrada produz resultado reproduzível, explicado e auditável.

### Fase 11 — Conciliação e importação estruturada

- **Entrada:** volume e histórico confiáveis; lançamentos manuais estabilizados.
- **Entregas:** OFX, CSV e Excel, hash do arquivo, transações importadas, duplicidades,
  correspondência assistida, conciliação e desconciliação com histórico.
- **Testes mínimos:** parser, idempotência, arquivos inválidos, tenant e trilha histórica.
- **Homologação:** arquivos fictícios sanitizados; totais antes/depois conferidos.
- **Riscos:** importar antes de definir conciliação; duplicar movimentos. PDF/OCR fica fora.
- **Saída:** reimportação segura e nenhuma alteração silenciosa em lançamentos confirmados.

### Fase 12 — Categorização automática

- **Entrada:** dados conciliados suficientes para validar padrões.
- **Entregas:** regras por descrição/estabelecimento, prioridade, sugestão, aplicação futura e
  retroativa explicitamente confirmada.
- **Testes mínimos:** precedência, colisão, explicabilidade e reversão.
- **Homologação:** amostra anonimizada com revisão humana.
- **Riscos:** regra ampla alterar histórico ou ocultar baixa confiança.
- **Saída:** sugestões rastreáveis, reversíveis e sem cruzamento empresarial.

### Fase 13 — Exportações, agenda e integrações

- **Entrada:** contratos internos estáveis e necessidade validada.
- **Entregas:** exportações PDF/Excel, calendário, mensagens, backup e notificações em tarefas
  separadas, cada integração com autorização própria.
- **Testes mínimos:** segurança, privacidade, falhas externas, idempotência e formatos.
- **Homologação:** ambiente isolado e consentimento quando aplicável.
- **Riscos:** fornecedor externo, dados sensíveis e escopo excessivo.
- **Saída:** integração observável, reversível e documentada; nenhuma é requisito do primeiro MVP.

## Marcos

| Marco | Fases | Evidência de saída |
|---|---:|---|
| M1 — Base utilizável | 1–3 | dois usuários, cadastros e saldos básicos confiáveis |
| M2 — Compromissos e crédito | 4–6 | contas a pagar, compras, parcelas e faturas homologadas |
| M3 — Planejamento familiar | 7–8 | orçamento, meta e dashboard explicáveis |
| M4 — MVP Financeiro LeS | 9–10 | terceiros e simulador básico funcionais |
| M5 — Automação segura | 11–12 | importação, conciliação e categorização auditáveis |
| M6 — Ecossistema | 13 | exportações e integrações isoladas |

## Definição de MVP pronto

O primeiro MVP está pronto quando os dois usuários acessam a empresa; contas, pessoas e categorias
estão cadastradas; receitas, despesas, saldos, cartões, compras, parcelas e pagamentos de fatura
funcionam; orçamento, meta, dashboard e simulador básico refletem os mesmos dados; terceiros são
separados do consumo; histórico e lixeira funcionam; segurança, responsividade, testes e
homologação foram aprovados. Importação e integrações externas não bloqueiam esse marco.

## Primeira tarefa recomendada

`LES-F2-001 — Preparação técnica do domínio Financeiro LeS`: delimitar pacotes e fronteiras,
registrar convenções e reaproveitamento do núcleo existente, definir critérios multiempresa e a
base da estratégia de testes. Depende apenas da aprovação deste roadmap e não cria todas as
entidades, migrations ou regras financeiras.
