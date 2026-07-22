# Navegação — Financeiro LeS

A navegação reutiliza layout, topbar, seletor de empresa, estilos e comportamento responsivo da
Criati. Não cria uma identidade paralela. O contexto da empresa é derivado da sessão validada;
filtros apenas refinam a visualização e nunca substituem autorização.

## Menu principal do MVP

Para evitar uma sidebar excessivamente longa, apenas destinos frequentes ficam no primeiro nível.

```text
Visão geral
  Dashboard

Movimentações
  Lançamentos
  Contas a pagar

Cartões
  Cartões
  Compras
  Faturas
  Parcelas

Planejamento
  Orçamento
  Meta de economia
  Simulador de Gastos

Terceiros
  Empréstimos
  Compromissos
  Compras para terceiros
  Valores a receber

Configurações
  Contas bancárias
  Pessoas
  Categorias
  Lixeira
  Histórico
```

Agenda em lista, anexos, recorrências e detalhe de entidades são acessados contextualmente.
Conciliação, regras de categorização, agenda mensal, assinaturas dedicadas e central documental
são adicionadas aos respectivos grupos somente nas fases em que forem entregues.

## Navegação desktop

### Sidebar expandida

- Exibe marca da Criati, nome do aplicativo e grupos recolhíveis.
- Mostra ícone e texto; grupo atual inicia expandido.
- Item ativo tem marcador visual, contraste e `aria-current="page"` na futura implementação.
- Submenus abrem por acionamento explícito e preservam estado durante a sessão.
- `Simulador de Gastos` recebe destaque sem competir com alertas críticos.

### Sidebar recolhida

- Mantém ícones reconhecíveis e texto em tooltip acessível por foco e ponteiro.
- O item ativo continua perceptível sem depender só de cor.
- Submenu abre em painel adjacente; não muda o destino ao mero passar do ponteiro.

### Topbar

Ordem proposta: botão da sidebar, breadcrumb/título, filtros globais aplicáveis, ações rápidas,
alertas, seletor de empresa e perfil. Em larguras menores, filtros migram para um painel sem
ocultar a indicação de que estão ativos.

### Breadcrumbs

- Usados em detalhes e fluxos com mais de um nível: `Cartões > Cartão Inter > Fatura jul/2026`.
- Não repetem `Dashboard` como nível artificial.
- Cada nível anterior é navegável; o atual é texto.
- Retornar preserva filtros e paginação da lista de origem quando possível.

### Ações rápidas

- `Lançar` abre o cadastro rápido.
- `Simular` abre o Simulador.
- Ações contextuais aparecem junto ao título: `Pagar fatura`, `Registrar recebimento`.
- A ação principal é única; ações destrutivas ficam em menu secundário.

## Navegação mobile

### Estrutura

- Topbar compacta com empresa atual, alertas e menu off-canvas.
- Menu off-canvas contém todos os grupos e fecha após a escolha do destino.
- Conteúdo usa uma coluna, cards/listas e ações fixas somente quando não cobrem informação.
- Filtros abrem em painel inferior ou tela dedicada, com resumo dos filtros aplicados.

### Navegação inferior proposta

```text
Início | Lançar | Simular | Agenda | Mais
```

Vantagens: alcançável com uma mão, torna `Simular` sempre visível e reduz passos nas ações mais
frequentes. Riscos: competir com o teclado e ações fixas, ocupar altura em 360 × 800, sugerir que
`Lançar` e `Simular` são páginas de mesmo tipo e duplicar o off-canvas. A decisão final permanece
em `PENDENCIAS.md`; se adotada, `Simular` ocupa a posição central e tem rótulo, não só ícone.

### Regras para uso com uma mão

- Alvos de toque mínimos de 44 × 44 CSS px, espaçamento entre ações destrutivas e comuns.
- Ações primárias ficam no terço inferior quando isso não oculta conteúdo ou teclado.
- Botão Voltar do navegador e gesto do sistema preservam estado e não descartam formulário.
- Tabelas viram listas de cards; não dependem de rolagem horizontal para ações essenciais.

## Filtros globais

| Filtro | Escopo | Persistência | Não altera |
| --- | --- | --- | --- |
| Competência | Dashboard, lançamentos, compras, faturas, orçamento e meta | Sessão do aplicativo | saldo atual da conta; alertas críticos vencidos |
| Pessoa | Consultas de movimentos, compras e comparações | Sessão até remoção explícita | cálculo familiar de renda, orçamento, meta e saldo consolidado |
| Conta | Lançamentos, extrato, contas a pagar e projeções pertinentes | Por área | totais consolidados sem aviso |
| Cartão | Compras, faturas, parcelas e projeções de cartão | Por área | limite consolidado do cartão principal/virtual |

O Dashboard sempre exibe uma faixa “Visão filtrada” com chips removíveis. Totais críticos que não
respeitam um filtro exibem “Consolidado familiar” de forma explícita. Nenhum filtro pode esconder
silenciosamente conta atrasada ou risco crítico: o alerta permanece e informa que está fora do
recorte atual.

## Filtros locais

- Período e competência não são sinônimos: período filtra datas; competência filtra o ciclo.
- Filtros avançados ficam recolhidos, mas chips ativos permanecem visíveis.
- `Limpar filtros` restaura o padrão documentado da tela.
- Contagem de resultados e estado vazio refletem os filtros aplicados.
- Filtros não modificam dados, cálculos históricos nem contexto empresarial.

## Busca

A busca financeira cobre descrição, estabelecimento, favorecido, terceiro, categoria, valor e
documento. O usuário escolhe ou enxerga o escopo (`Nesta tela` ou `Financeiro`). Resultados são
agrupados por tipo, mostram valor/data/contexto e respeitam empresa e permissões no backend.
Documento sensível não é indexado pelo conteúdo integral; a busca pode usar metadados seguros.

## Padrões de retorno e continuidade

- Salvar com sucesso retorna ao detalhe ou à lista de origem com feedback e filtros preservados.
- Cancelar formulário com mudanças solicita confirmação antes de descartá-las.
- Abrir a causa de um alerta aplica filtro explícito, visível e removível.
- Links profundos exigem contexto autenticado e autorização; não revelam recurso de outra empresa.
- Operações longas têm estado de processamento e podem ser retomadas sem duplicar registros.

## Documentos relacionados

- `MAPA-DE-TELAS.md` — destinos e prioridades.
- `EXPERIENCIA-DO-USUARIO.md` — comportamento dos componentes e responsividade.
- `ESTADOS-DE-INTERFACE.md` — feedback de navegação e operações.
