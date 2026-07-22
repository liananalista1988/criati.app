# Mapa de Telas — Financeiro LeS

Este documento transforma o domínio aprovado em um inventário navegável. Ele não define rotas,
endpoints ou implementação. Todas as telas operam dentro da empresa atual validada pela Criati;
nenhum identificador recebido da interface define o contexto empresarial.

## Legenda de prioridade

- **MVP**: indispensável ao primeiro uso real, conforme `MVP.md`.
- **Fase seguinte**: reduz trabalho manual ou aprofunda a consulta, mas não bloqueia o núcleo.
- **Futuro**: depende de integração, automação avançada ou validação prévia do núcleo.

## Hierarquia geral

```text
Financeiro LeS
├── Visão geral
│   ├── Dashboard Familiar [MVP]
│   ├── Agenda: próximos dias e atrasados [MVP, incorporada ao Dashboard]
│   ├── Agenda: calendário mensal [Fase seguinte]
│   └── Central de alertas [Fase seguinte; alertas contextuais no MVP]
├── Movimentações
│   ├── Lançamentos [MVP]
│   │   └── Cadastro rápido / edição / detalhe [MVP]
│   ├── Receitas e despesas [MVP, visões filtradas de Lançamentos]
│   ├── Transferências [MVP, fluxo de Lançamentos]
│   └── Conciliação [Fase seguinte]
├── Contas
│   ├── Contas bancárias [MVP]
│   │   └── Detalhe e extrato [MVP]
│   ├── Contas a pagar [MVP]
│   │   └── Detalhe, pagamento e comprovantes [MVP]
│   ├── Recorrências [MVP, geridas dentro de receitas/contas]
│   └── Central de comprovantes [Fase seguinte; acesso contextual no MVP]
├── Cartões
│   ├── Cartões [MVP]
│   │   └── Detalhe do cartão [MVP]
│   ├── Compras [MVP]
│   │   └── Detalhe da compra [MVP]
│   ├── Faturas [MVP]
│   │   └── Detalhe e pagamento [MVP]
│   ├── Parcelas [MVP]
│   └── Assinaturas [Fase seguinte; recorrência básica no MVP]
├── Planejamento
│   ├── Orçamento [MVP]
│   ├── Meta de economia [MVP]
│   ├── Simulador de Gastos [MVP]
│   │   └── Resultado e comparação de cenários [MVP]
│   └── Projeções detalhadas [MVP dentro do Simulador; tela dedicada na fase seguinte]
├── Terceiros
│   ├── Empréstimos concedidos [MVP]
│   ├── Compromissos a pagar [MVP]
│   ├── Compras para terceiros [MVP]
│   └── Valores a receber [MVP]
└── Configurações
    ├── Pessoas da residência [MVP]
    ├── Categorias [MVP]
    ├── Instituições e bandeiras [MVP, seletores com gestão contextual]
    ├── Regras de categorização [Fase seguinte]
    ├── Preferências [Fase seguinte]
    ├── Lixeira [MVP]
    └── Histórico básico [MVP]
```

## Inventário consolidado

| Área | Tela | Prioridade | Entrada principal | Saída/ação central |
| --- | --- | --- | --- | --- |
| Visão geral | Dashboard Familiar | MVP | Início do Financeiro | Entender situação e agir sobre risco |
| Visão geral | Agenda em lista | MVP incorporado | Dashboard | Pagar ou abrir compromisso |
| Visão geral | Agenda mensal | Fase seguinte | Menu | Consultar calendário |
| Visão geral | Alertas | Fase seguinte | Topbar/menu | Abrir causa do alerta |
| Movimentações | Lançamentos | MVP | Menu/atalho | Consultar e registrar movimento |
| Movimentações | Cadastro rápido | MVP | `Lançar` | Salvar receita, despesa, transferência ou ajuste |
| Contas | Contas bancárias | MVP | Menu | Ver saldo e extrato individual |
| Contas | Contas a pagar | MVP | Menu/agenda | Registrar e pagar compromisso |
| Cartões | Cartões | MVP | Menu | Ver comprometimento e próxima fatura |
| Cartões | Detalhe do cartão | MVP | Lista de cartões | Operar compra, fatura e limite |
| Cartões | Compras | MVP | Menu/cartão | Ver decisão total e impacto mensal |
| Cartões | Faturas | MVP | Menu/dashboard | Ver composição e registrar pagamento |
| Cartões | Parcelas | MVP | Menu/compra/fatura | Consultar compromissos futuros |
| Planejamento | Orçamento | MVP | Menu/dashboard | Definir limites e acompanhar consumo |
| Planejamento | Meta de economia | MVP | Menu/dashboard | Acompanhar meta projetada |
| Planejamento | Simulador de Gastos | MVP | Ação destacada | Comparar cenários e decidir |
| Planejamento | Projeções | Fase seguinte | Simulador/menu | Explorar horizonte mensal detalhado |
| Terceiros | Empréstimos concedidos | MVP | Menu | Registrar e receber valores |
| Terceiros | Compromissos a pagar | MVP | Menu | Pagar ou renegociar dívida não residencial |
| Terceiros | Compras para terceiros | MVP | Menu/compras | Acompanhar pagamento e reembolso separados |
| Terceiros | Valores a receber | MVP | Menu/dashboard | Consolidar recebíveis sem somá-los ao saldo |
| Operação | Conciliação | Fase seguinte | Conta/cartão | Importar, comparar e confirmar manualmente |
| Configuração | Categorias | MVP | Menu/seletores | Criar, editar e desativar |
| Configuração | Pessoas | MVP | Menu | Vincular pessoa ao contexto familiar |
| Configuração | Anexos contextuais | MVP | Detalhe do registro | Visualizar, baixar ou excluir comprovante |
| Configuração | Central de anexos | Fase seguinte | Menu | Pesquisar documentos |
| Configuração | Histórico | MVP básico | Menu/detalhes | Consultar alterações críticas |
| Configuração | Lixeira | MVP | Menu | Restaurar ou excluir definitivamente |
| Configuração | Regras de categorização | Fase seguinte | Menu/conciliação | Testar e priorizar regras |

## Decisões de agrupamento do MVP

- Receitas, despesas e transferências são visões/filtros de **Lançamentos**, não três itens fixos
  de menu.
- Recorrências são configuradas no registro de origem; não ganham item principal no MVP.
- A agenda do MVP é uma lista de próximos 7/30 dias e atrasados dentro do Dashboard, com acesso
  ampliado; o calendário mensal fica para a fase seguinte.
- Alertas são blocos contextuais e acionáveis no MVP; uma central dedicada fica para depois.
- Instituições e bandeiras são geridas a partir dos respectivos seletores no MVP.
- Comprovantes ficam no registro relacionado; a central documental não é necessária no MVP.
- Projeções essenciais ficam no Simulador; uma área analítica própria fica para a fase seguinte.
- Conciliação manual simples pode ser preparada no domínio, mas a experiência de importação e
  sugestões entra na fase seguinte, coerente com `MVP.md`.

## Evoluções futuras

- importação de PDF/OCR e integrações bancárias;
- Google Calendar;
- envio automático de cobranças;
- exportações avançadas e backup dedicado;
- recomendações adaptativas, sempre explicáveis;
- automação de conciliação apenas após validação do fluxo confirmado manualmente.

## Regras transversais do mapa

- O seletor da empresa atual é da plataforma e permanece visível.
- O filtro por pessoa nunca transforma a renda familiar ou o orçamento em cálculos segregados.
- Compra residencial e compra para terceiro nunca compartilham totais de consumo.
- Pagamento de fatura afeta caixa, mas não cria nova despesa de consumo.
- Valores previstos, realizados e simulados usam rótulos textuais próprios.
- Todo alerta abre a lista ou o registro que explica seu valor.

## Documentos relacionados

- `NAVEGACAO.md` — como percorrer este mapa.
- `ESPECIFICACAO-DAS-TELAS.md` — conteúdo e ações de cada tela.
- `JORNADAS-DO-USUARIO.md` — caminhos completos entre as telas.
- `MVP.md` — fundamento do corte de prioridade.
