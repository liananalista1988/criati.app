# Experiência do Usuário — Financeiro LeS

## Princípios

1. Começar pela situação financeira atual e terminar em uma ação útil.
2. Prevenir decisões arriscadas, sem impedir o registro nem usar linguagem alarmista.
3. Separar consumo, movimento de caixa e exposição a terceiros.
4. Explicar alertas com números, período, origem e caminho até a causa.
5. Reduzir passos das ações frequentes no celular.
6. Preservar a visão familiar consolidada e oferecer filtro por pessoa para consulta.
7. Mostrar compra parcelada pelo valor total da decisão e pelo impacto mensal.
8. Rotular realizado, previsto, informado, calculado, estimado e simulado.
9. Usar divulgação progressiva para evitar telas densas.
10. Tratar acessibilidade como comportamento funcional, não acabamento visual.

## Dashboard Familiar

### Ordem de leitura

1. **Contexto**: empresa, competência, pessoa e qualidade dos dados.
2. **Situação atual**: saldo consolidado, renda recebida, gasto real e saída efetiva.
3. **Capacidade de decisão**: orçamento disponível, meta e risco da próxima fatura.
4. **Urgências**: atrasados, fatura em risco, recebíveis vencidos e limites.
5. **Compromissos**: contas, faturas, parcelas e terceiros.
6. **Explicações**: comparações, categorias, pessoas e cartões.

### Faixa principal

Em desktop, no máximo seis cards aparecem antes da primeira rolagem; os demais ficam em uma
segunda faixa ou painel “Ver composição”. Em mobile, a sequência é: saldo, risco/alerta mais
urgente, orçamento disponível, gasto real, meta e próxima fatura. `Simular nova despesa` permanece
visível como ação principal.

| Indicador | Valor e período | Comparação/estado | Ação |
| --- | --- | --- | --- |
| Saldo consolidado | saldo atual, agora | contas incluídas e conciliação | Ver contas |
| Renda recebida | competência atual | previsto × recebido | Ver receitas |
| Gasto real | competência atual | mês anterior e média de 3 meses | Ver composição |
| Saída efetiva | período atual | consumo × outros movimentos | Ver lançamentos |
| Orçamento disponível | competência | realizado + comprometido | Ajustar orçamento |
| Meta de economia | alvo, projetado e realizado | saudável/em risco | Ver meta |
| Risco da próxima fatura | valor projetado e capacidade | nível e causa | Abrir projeção |

Gasto real nunca inclui pagamento da fatura como nova despesa. Compra para terceiro aparece em
“Exposição a terceiros”, não em consumo residencial. Recebíveis não são somados ao saldo.

### Prioridade dos alertas

1. crítico: conta/fatura vencida com impacto imediato ou incapacidade projetada de pagamento;
2. alto risco: fatura ou meta com risco numérico relevante;
3. atenção: categoria/limite saudável próximo do limite, recebível atrasado;
4. informativo: variação, dado estimado ou ação de organização.

Cada alerta contém ícone, nível em texto, título, explicação, valor/período e ação para a causa.

## Indicadores explicáveis

Todo card informa:

- nome sem abreviação ambígua;
- valor com moeda e sinal compreensíveis;
- período ou instante de referência;
- origem (`calculado`, `informado pelo banco`, `previsto` ou `simulado`);
- comparação acompanhada da base;
- estado por texto e ícone, não apenas cor;
- ação que abre composição, fórmula ou registros.

Estimativas exibem “Estimado” junto ao valor e explicam a hipótese. Dados parciais informam o que
falta. Variação positiva de gasto não usa verde, pois “aumento” não é necessariamente bom.

## Formulários

- Uma pergunta por grupo; rótulo sempre visível, ajuda antes do erro.
- Campos obrigatórios identificados em texto; erros associados ao campo e resumo no topo.
- Campo monetário aceita digitação local, confirma moeda e nunca muda centavos silenciosamente.
- Tipo da operação controla os campos seguintes sem apagar conteúdo sem confirmação.
- Campos prioritários aparecem primeiro; recorrência, anexos e observações ficam em “Mais detalhes”.
- Pessoa, conta, cartão e categoria exibem contexto suficiente para evitar homônimos.
- Ao detectar possível duplicidade, o sistema mostra candidatos e permite comparar, voltar ou
  confirmar conscientemente; não bloqueia por mera semelhança.
- Saída com alterações não salvas exige confirmação.

## Tabelas, listas e cards

- Cabeçalhos persistentes apenas quando não cobrem conteúdo; ordenação anuncia coluna e direção.
- Valores monetários alinhados e acompanhados de natureza/status quando o sinal puder confundir.
- A ação principal fica visível; ações secundárias entram em menu rotulado.
- Seleção em massa informa quantidade e impacto; ação irreversível sempre requer confirmação.
- No mobile, cada card começa por descrição, valor e status; metadados secundários seguem em duas
  linhas e ações ficam em menu ou rodapé do card.
- Paginação preserva filtros; listas longas não carregam tudo silenciosamente.

## Modais e painéis

Modais são reservados a confirmação curta, escolha do tratamento de excedente e ações simples.
Cadastro extenso e detalhe usam página ou painel amplo. O foco entra no título/primeiro campo,
fica contido no modal, retorna ao acionador e permite `Esc` somente quando o cancelamento é seguro.
A confirmação destrutiva nomeia o item e o efeito; excluir definitivamente nunca é a ação padrão.

## Componentes reutilizáveis

| Componente | Conteúdo mínimo | Variações |
| --- | --- | --- |
| Card de indicador | título, valor, período, origem, estado, ação | compacto/detalhado |
| Alerta | nível, ícone, título, explicação, valor, ação | inline/faixa/lista |
| Tabela/lista mobile | colunas semânticas, status, ações | seleção/paginação |
| Seletor de competência | mês/ano e atalho atual | global/local |
| Filtro por pessoa | Todas/Pessoa | chip/painel |
| Campo monetário | moeda, valor, erro | entrada/somente leitura |
| Seletores financeiros | rótulo e contexto | categoria/conta/cartão |
| Badge de status | texto e ícone opcional | neutro/atenção/risco |
| Confirmação | impacto e ações | comum/destrutiva |
| Painel de projeção | horizonte, séries e hipóteses | real/previsto/simulado |
| Anexo | nome seguro, tipo, tamanho, estado | upload/visualização |
| Histórico/timeline | ator, evento, instante e valores | compacto/completo |
| Calendário | data, itens e legenda textual | mês/lista |
| Comparador | cenários, diferenças e justificativa | 2 a 5 cenários |

## Acessibilidade

- Ordem de foco acompanha a leitura; link “Pular para o conteúdo”.
- Foco visível em todos os controles, inclusive cards acionáveis e menus.
- Rótulos programáticos, instruções e erros associados; placeholders não substituem rótulos.
- Contraste compatível com WCAG AA e informação nunca exclusiva de cor, posição ou animação.
- Tabelas usam título, cabeçalhos e escopo; versão mobile mantém os mesmos nomes de dados.
- Valores são lidos com moeda e sinal; abreviações têm nome acessível.
- Ícones decorativos são ignorados; ícones funcionais possuem nome textual.
- Gráficos têm resumo textual e tabela equivalente.
- Alertas dinâmicos não críticos usam anúncio não interruptivo; erros críticos recebem foco adequado.
- Movimento reduzido respeita preferência do sistema.
- Alvos de toque têm ao menos 44 × 44 CSS px.

## Responsividade

| Resolução | Comportamento |
| --- | --- |
| 1920 × 1080 | sidebar expandida; grade ampla, conteúdo com largura máxima legível |
| 1366 × 768 | 3–4 indicadores por linha; detalhes secundários recolhíveis |
| 1280 × 720 | topbar compacta; filtros menos usados em painel |
| 1024 × 768 | sidebar recolhida por padrão; tabelas preservam colunas essenciais |
| 768 × 1024 | layout de tablet; duas colunas de cards; filtros em painel |
| 430 × 932 | uma coluna; lista mobile; ações alcançáveis; simulador em etapas |
| 390 × 844 | títulos e filtros compactos; comparação com rolagem vertical |
| 360 × 800 | conteúdo mínimo prioritário; sem truncar valor, status ou ação principal |

O Simulador divide entrada, resultado e comparação em etapas no mobile. Gráficos nunca são a
única forma de leitura. Tabelas oferecem cards equivalentes, não uma versão funcionalmente menor.

## Riscos de experiência e mitigação

- **Dashboard denso**: limitar faixa inicial e oferecer composição progressiva.
- **Menu longo**: agrupar visões e usar entradas contextuais.
- **Consumo versus caixa**: rótulos e blocos distintos, sem somar pagamento de fatura ao gasto.
- **Falsa disponibilidade**: recebíveis e estimativas nunca entram no saldo sem explicação.
- **Alert fatigue**: priorizar por impacto, agrupar repetidos e manter causa/ação.
- **Filtros ocultando risco**: alertas críticos consolidados permanecem visíveis.
- **Recomendação opaca**: mostrar hipóteses, números e comparação usada.
- **Mobile sobrecarregado**: formulários em etapas e detalhes secundários expansíveis.

## Documentos relacionados

- `ESPECIFICACAO-DAS-TELAS.md` — aplicação destes padrões por tela.
- `ESTADOS-DE-INTERFACE.md` — mensagens, estados e alertas.
- `CALCULOS-E-INDICADORES.md` — origem dos números apresentados.
