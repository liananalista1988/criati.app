# Especificação das Telas — Financeiro LeS

Especificação funcional para prototipação e divisão futura de implementação. Não define HTML,
rotas, APIs ou estrutura técnica. Regras de cálculo e transição continuam pertencendo aos
documentos de domínio citados ao final.

## Padrão de especificação

Toda tela apresenta título, contexto empresarial, filtros ativos, ação principal, conteúdo,
origem/competência dos valores e estados descritos em `ESTADOS-DE-INTERFACE.md`. Detalhes abertos
a partir de listas preservam filtros e paginação ao retornar.

## 1. Dashboard Familiar — MVP

**Objetivo:** explicar a situação atual e priorizar ações. **Filtros:** competência e pessoa;
conta/cartão são locais aos blocos. **Ação principal:** `Simular nova despesa`.

Conteúdo: faixa de saldo, renda, gasto real, saída efetiva, orçamento, meta e risco de fatura;
alertas; próximos 7/30 dias e atrasados; faturas/cartões; terceiros; comparações de mês anterior,
média de três meses, planejado × realizado, categoria e pessoa. Recebíveis e terceiros ficam
separados do saldo e consumo. Todo card abre sua composição.

```text
[Topbar] Empresa | Competência | Pessoa | Alertas | Perfil
[Situação] Saldo | Renda | Gasto real | Saída efetiva
[Decisão] Orçamento | Meta | Risco da próxima fatura
[Alertas prioritários — valor, causa e ação]
[Próximos compromissos] [Cartões e parcelas]
[Comparações por categoria e pessoa]
[Ação principal] Simular nova despesa
```

No mobile, cards em uma coluna, alerta urgente após saldo e ação `Simular` persistente sem cobrir
conteúdo. Estado vazio orienta o cadastro inicial, não exibe zeros como se fossem dados reais.

## 2. Contas bancárias — MVP

**Lista:** nome, titular, instituição, saldo atual, saldo conciliado, última movimentação, situação
e ações. **Ações:** cadastrar, editar, abrir extrato, desativar; importar/conciliar aparecem apenas
quando a fase correspondente existir. O consolidado precede a lista, mas cada conta é acessível.

**Detalhe/extrato:** saldo, origem e atualização; movimentos com período, natureza, conciliação e
busca. Desativação explica que o histórico e saldo permanecem no consolidado. Transferência entre
contas próprias é rotulada e soma zero no consolidado.

```text
[Topo] Contas bancárias | Saldo consolidado | Cadastrar conta
[Filtros] Titular | Instituição | Situação
[Lista] Conta | Titular | Saldo atual/conciliado | Último movimento | Ações
[Detalhe] Resumo da conta | Extrato | Lançar | Transferir
```

Mobile: card por conta com saldo e status antes dos metadados; extrato em lista compacta.

## 3. Lançamentos e cadastro rápido — MVP

**Lista:** receitas, despesas, transferências, ajustes, pagamentos e recebimentos. **Filtros:**
período, pessoa, conta, categoria, natureza, status, conciliado, origem e texto. **Dados:** data,
descrição, categoria, conta, pessoa, valor, natureza, status, origem e conciliação.

**Cadastro rápido:** tipo, valor, data, conta ou cartão, categoria, descrição, pessoa e terceiro
quando aplicável; `Mais detalhes` contém observação, recorrência e anexo. Transferência solicita
origem/destino e não oferece categoria de renda/despesa. Candidato duplicado abre comparação.
Registro conciliado não é sobrescrito: a interface orienta desconciliação ou ajuste permitido.

```text
[Topo] Lançamentos | Lançar
[Filtros básicos] Período | Pessoa | Natureza | Status | Busca
[Filtros avançados] Conta | Categoria | Origem | Conciliação
[Tabela/lista] Data | Descrição | Categoria | Conta | Pessoa | Valor | Status

[Cadastro rápido]
Tipo | Valor | Data
Conta/cartão | Categoria | Pessoa
Descrição | Terceiro, se aplicável
[Mais detalhes] Recorrência | Observação | Anexo
[Ações] Cancelar | Salvar
```

Mobile: lista de cards; formulário em uma coluna, teclado monetário e ação salvar após resumo.

## 4. Contas a pagar — MVP

Visões: lista, próximos vencimentos, por status e recorrentes (mensais, anuais, variáveis).
Filtros: competência/período, favorecido, categoria, pessoa, recorrência e status. Indicadores:
total a vencer, vencido, pago e previsto × realizado.

Ações: cadastrar, pagar, pagar parcialmente, anexar comprovante, editar, cancelar e enviar à
lixeira. Pagamento parcial mostra saldo posterior. Juros/multa ficam separados do principal.
Recorrência variável gera ocorrência prevista a confirmar, não inventa valor realizado.

```text
[Topo] Contas a pagar | Competência | Cadastrar
[Indicadores] A vencer | Vencido | Pago | Previsto × realizado
[Abas] Lista | Próximos vencimentos | Recorrentes
[Filtros] Status | Favorecido | Categoria | Pessoa
[Itens] Vencimento | Descrição | Valor/saldo | Status | Pagar
```

## 5. Agenda financeira — MVP incorporado / calendário na fase seguinte

No MVP, painel/lista com próximos 7 dias, 30 dias e atrasados; calendário mensal é fase seguinte.
Itens: contas, faturas, parcelas, receitas, compromissos, recebíveis e assinaturas básicas. Cada
item exibe tipo, descrição, valor, vencimento, status, pessoa e ação. Recebíveis usam semântica de
entrada prevista, nunca saldo disponível.

```text
[Topo] Agenda | 7 dias | 30 dias | Atrasados | Tipo
[Agrupamento por data]
  Tipo | Descrição | Pessoa | Valor | Status | Ação
[Fase seguinte] Alternar Lista / Calendário mensal
```

## 6. Cartões e detalhe — MVP

**Lista:** nome, titular, banco, bandeira, físico/virtual, principal, limite bancário e saudável,
percentual comprometido, próxima fatura, fechamento, vencimento e status. Cartão virtual exibe
“Compartilha limite e fatura com [principal]” e não soma limite separado.

**Detalhe:** resumo, fatura atual, próximas faturas, compras recentes, parcelas futuras,
assinaturas básicas, limite e histórico. Ações: registrar compra, editar, projeção e pagamento de
fatura; importação é fase seguinte. Bloquear/cancelar exige explicação sobre compras/faturas.

```text
[Topo] Cartões | Cadastrar cartão
[Resumo] Limite saudável total | Comprometido | Faturas abertas
[Cards] Cartão | Titular | Principal/Virtual | Limites | Próxima fatura | Status

[Detalhe do cartão]
[Resumo e limites] [Ações: Compra | Projeção | Pagamento]
[Fatura atual] [Próximas faturas]
[Compras] [Parcelas] [Assinaturas] [Histórico]
```

## 7. Compras no crédito — MVP

Filtros: cartão, pessoa, categoria, período, estabelecimento, residencial/terceiro, parcelada,
estornada e cancelada. Cada item mostra valor total da decisão, impacto mensal/parcela atual,
quantidade de parcelas, fatura, categoria, pessoa, terceiro e status.

Detalhe separa compra, parcelas, faturas, estornos e comprovantes. Alterar a compra informa se a
mudança afeta só uma parcela ou a série conforme a regra aplicável. Compra para terceiro usa selo
textual e não entra em consumo residencial.

```text
[Topo] Compras no crédito | Registrar compra
[Filtros] Cartão | Pessoa | Categoria | Período | Residencial/Terceiro
[Resumo] Valor total decidido | Impacto mensal | Parcelas futuras
[Lista] Compra | Total | Parcela atual/n | Fatura | Pessoa | Status
[Detalhe] Dados da decisão | Cronograma de parcelas | Estornos | Anexos
```

## 8. Faturas e detalhe — MVP

**Lista:** cartão, competência, fechamento, vencimento, valor, mínimo, saldo, status e risco.
Ações: abrir, pagar, anexar comprovante e ver composição; importar/conciliar são fase seguinte.

**Detalhe:** compras à vista, parcelas, assinaturas, encargos, estornos, pagamentos e saldo
financiado. Cabeçalho compara total calculado e informado pelo banco, diferença, valor pago,
saldo, situação e origem. Pagamento é movimento de caixa, não nova despesa. Pagamento menor que o
saldo exige confirmação de pagamento parcial e explica saldo/encargos; excedente exige classificar
como juros, multa, adiantamento ou não identificado.

```text
[Topo] Faturas | Competência | Cartão
[Lista] Cartão | Fechamento | Vencimento | Valor | Saldo | Risco | Abrir/Pagar

[Detalhe]
[Calculado | Informado | Diferença | Pago | Saldo | Status]
[Composição] À vista | Parcelas | Assinaturas | Encargos | Estornos
[Pagamentos — saída de caixa, não consumo]
[Ações] Registrar pagamento | Anexar comprovante
```

## 9. Parcelas — MVP

Visões por mês, cartão e compra; filtros futuras, pagas, atrasadas, estornadas e canceladas.
Mostra compra original, número/total, valor, competência, fatura e status. O total da compra fica
acessível mesmo na visão mensal. Antecipação seleciona parcelas, informa valor pago e desconto
manual, sem cálculo contratual complexo.

```text
[Topo] Parcelas | Por mês / cartão / compra
[Indicadores] Próximas 3 competências | Total futuro
[Filtros] Status | Cartão | Pessoa
[Lista] Compra | Parcela n/total | Valor | Competência | Fatura | Status
```

## 10. Orçamento — MVP

Mostra limite geral, limites por categoria, limite saudável dos cartões, realizado, comprometido,
disponível e percentual. Ações: definir orçamento, criar limite, mudar competência e copiar mês
anterior. O sistema alerta, nunca bloqueia. Copiar exibe o que será replicado e não copia valores
realizados.

```text
[Topo] Orçamento | Competência | Copiar mês anterior
[Geral] Limite | Realizado | Comprometido | Disponível | Percentual
[Categorias] Categoria | Limite | Realizado | Comprometido | Disponível | Estado
[Cartões] Limite bancário × saudável × comprometido
[Ação] Definir/ajustar limites
```

## 11. Meta de economia — MVP

Campos: percentual ou valor, competência, regra padrão e exceção mensal. Mostra alvo, projetado,
realizado, situação e histórico. Estados: atingida, saudável, em risco e não atingida. Explica que
meta projetada não transfere dinheiro; eventual reserva real é uma transferência entre contas.

```text
[Topo] Meta de economia | Competência | Configurar
[Alvo] Percentual/valor | Valor alvo | Projetado | Realizado | Situação
[Explicação] Componentes da disponibilidade segura
[Histórico mensal]
[Nota] Nenhuma transferência automática é realizada
```

## 12. Simulador de Gastos — MVP

### Entrada

Tipo de operação, valor, data, categoria, débito/crédito, conta/cartão, parcelas, terceiro,
recorrência e observação. Tipos: débito, crédito à vista, parcelada, assinatura, conta recorrente,
empréstimo concedido e compra para terceiro. No mobile, etapas: operação; forma/detalhes; revisão.

### Resultado

Valor total da decisão; impacto no mês, próxima fatura e limite; horizontes 3/6/12 meses; meta,
orçamento, saldo projetado, risco e recomendação. Toda projeção informa dados usados, ausências e
hipóteses. Classificação alerta, nunca bloqueia.

### Comparação e ações

Compara débito, crédito à vista, parcelamentos, aguardar fechamento e adiar compra. Diferenças
monetárias e temporais acompanham a recomendação. Ações: ajustar, descartar e salvar como real.
Salvar exige revisão e revalidação dos dados atuais para não persistir cenário desatualizado.

```text
[Topo] Simulador de Gastos | Dados usados/atualização
[Entrada] Tipo | Valor | Data | Categoria | Forma | Conta/cartão | Parcelas
[Detalhes] Terceiro | Recorrência | Observação
[Resultado] Total | Mês | Fatura | Limite | 3/6/12 meses
[Impacto] Orçamento | Meta | Saldo projetado | Nível de risco
[Comparador] Débito | Crédito | Parcelamentos | Aguardar/adiar
[Por que recomendamos] Números, hipóteses e consequência
[Ações] Ajustar | Descartar | Salvar como lançamento real
```

## 13. Projeções — MVP no Simulador / tela dedicada na fase seguinte

Tabela mensal e gráficos de saldo/comprometimento com parcelas, receitas, despesas e reserva
projetadas. Séries usam rótulos e padrões: `Real`, `Previsto`, `Simulado`; tabela equivalente é
obrigatória. O usuário liga/desliga o cenário simulado e vê premissas. Recebível previsto não é
tratado como garantido.

```text
[Horizonte] 3 | 6 | 12 meses [Cenário simulado ligado/desligado]
[Resumo textual das hipóteses]
[Gráfico de saldo] [Gráfico de comprometimento]
[Tabela] Competência | Real | Previsto | Simulado | Saldo | Reserva
```

## 14. Empréstimos concedidos — MVP

Lista: devedor, original, saldo, parcelas, juros, atraso, próximo vencimento e situação. Ações:
cadastrar, receber, renegociar, anexar e simular; gerar mensagem é fase seguinte. O desembolso não
é consumo e o saldo a receber não integra saldo disponível.

```text
[Topo] Empréstimos concedidos | Cadastrar
[Indicadores] Saldo a receber | Vencido | Próximo mês
[Lista] Devedor | Original | Saldo | Próximo vencimento | Atraso | Status
[Detalhe] Condições | Cronograma | Recebimentos | Renegociações | Anexos
```

## 15. Compromissos a pagar — MVP

Lista: credor, origem, valor, saldo, parcela, vencimento, juros, atraso e status. Ações: pagar,
pagar parcialmente, antecipar, renegociar e anexar. Fica separado do consumo residencial; o
pagamento afeta caixa. Renegociação preserva condições anteriores no histórico.

```text
[Topo] Compromissos a pagar | Cadastrar
[Indicadores] Saldo | Vencido | Próximo vencimento
[Lista] Credor | Origem | Saldo | Parcela | Vencimento | Status | Pagar
[Detalhe] Condições | Pagamentos | Renegociações | Anexos
```

## 16. Compras para terceiros — MVP

Mostra terceiro, compra, cartão, valor total, parcelas da fatura, parcelas a receber, recebido,
exposição e atraso. Ações: receber, ver diferença pagamento/reembolso e renegociar; cobrança
configurável fica para fase seguinte. O painel compara obrigação da família com reembolso sem
compensá-los silenciosamente.

```text
[Topo] Compras para terceiros | Registrar compra
[Indicadores] Exposição atual | A receber | Vencido
[Lista] Terceiro | Compra | Cartão | Total | Pago na fatura | Recebido | Saldo
[Detalhe] Parcelas da fatura × parcelas a receber | Recebimentos | Renegociação
```

## 17. Valores a receber — MVP

Consolida empréstimos, compras para terceiros, reembolsos e outros recebíveis. Filtros: devedor,
origem, vencimento, status e atraso. Indicadores: total, vencido, previsto no mês, recebido no mês
e exposição. Recebimento acima do saldo abre classificação obrigatória do excedente.

```text
[Topo] Valores a receber
[Indicadores] Total | Vencido | Previsto no mês | Recebido | Exposição
[Filtros] Devedor | Origem | Vencimento | Status
[Lista] Devedor | Origem | Vencimento | Saldo | Status | Registrar recebimento
```

## 18. Categorias — MVP

Nome, grupo, tipo, ativa, limite de orçamento e quantidade de usos. Criar, editar e desativar;
categoria usada não é apagada e permanece no histórico. Alteração de tipo com registros exige
tratamento explícito, nunca recategorização silenciosa.

```text
[Topo] Categorias | Cadastrar
[Filtros] Grupo | Tipo | Situação
[Lista] Nome | Grupo | Tipo | Orçamento | Usos | Situação | Ações
```

## 19. Pessoas da residência — MVP

Nome, usuário vinculado, contas, cartões e situação. Os dois usuários têm o mesmo acesso no
Financeiro LeS; pessoa organiza titularidade e filtro, sem duplicar autenticação. A interface
distingue “Pessoa da residência” de “Usuário Criati”.

```text
[Topo] Pessoas da residência | Adicionar pessoa
[Cards] Nome | Usuário vinculado | Contas | Cartões | Situação | Editar
[Explicação] Pessoa organiza dados; login e acesso pertencem à Criati
```

## 20. Anexos e comprovantes — MVP contextual

No MVP aparecem no detalhe do registro. Lista nome original seguro, tipo, tamanho, envio, autor e
ações visualizar/baixar/excluir. Upload mostra progresso, valida extensão/conteúdo/tamanho e não
expõe caminho interno. Download exige autorização e contexto da empresa. Central é fase seguinte.

```text
[Seção Comprovantes]
[Enviar arquivo] Formatos e tamanho permitidos
[Lista] Nome | Tipo | Tamanho | Enviado por/em | Estado | Visualizar/Baixar/Excluir
```

## 21. Histórico básico — MVP

Filtros: usuário, período, entidade e ação. Exibe quem, quando, entidade, campo, valor anterior,
novo e ação. Dados sensíveis não aparecem. No detalhe, timeline local; na tela geral, tabela
pesquisável. Não gera notificação entre os dois usuários.

```text
[Topo] Histórico
[Filtros] Usuário | Período | Entidade | Ação
[Timeline/tabela] Data | Usuário | Entidade | Alteração | Antes | Depois
```

## 22. Lixeira — MVP

Item, tipo, data de exclusão, usuário e origem. Ações restaurar e excluir definitivamente. Itens
não participam de cálculos. Restaurar valida conflitos e vínculos; exclusão definitiva nomeia o
item, explica irreversibilidade e exige confirmação reforçada.

```text
[Topo] Lixeira
[Aviso] Itens aqui não participam dos cálculos
[Filtros] Tipo | Usuário | Período
[Lista] Item | Tipo | Excluído em/por | Origem | Restaurar | Excluir definitivamente
```

## 23. Conciliação — fase seguinte

Etapas: selecionar conta/cartão; importar; validar; listar transações; sugerir; confirmar;
classificar novas; revisar. Cada linha mostra descrição original, data, valor, candidato,
confiança textual e ação. Ações: conciliar, criar lançamento, duplicado, ignorar e criar regra.
Baixa confiança nunca é confirmada automaticamente; massa irreversível exige revisão e resumo.

```text
[Etapas 1–8 e progresso]
[Arquivo e validação]
[Transações] Original | Data | Valor | Candidato | Confiança | Ação
[Resumo] Conciliadas | Novas | Duplicadas | Ignoradas | Erros
[Revisar e confirmar]
```

## 24. Regras de categorização — fase seguinte

Texto, correspondência, categoria, conta, cartão, prioridade, ativa e usos. Permite cadastrar,
editar, desativar, testar e aplicar retroativamente após prévia. Conflitos mostram regras
concorrentes e critério de prioridade; aplicação retroativa nunca altera conciliado sem revisão.

## 25. Assinaturas e agenda mensal — fase seguinte

Assinaturas dedicadas mostram custo mensal/anual, cartão/conta, reajustes e próxima cobrança.
Agenda mensal alterna calendário e lista, mantendo alternativa acessível. No MVP, assinaturas são
recorrências e agenda é lista contextual.

## Documentos relacionados

- `MAPA-DE-TELAS.md` — inventário e fases.
- `JORNADAS-DO-USUARIO.md` — uso encadeado destas telas.
- `ESTADOS-DE-INTERFACE.md` — estados vazios, erros e confirmações.
- `REGRAS-DE-NEGOCIO.md`, `ESTADOS-E-TRANSICOES.md` e `CALCULOS-E-INDICADORES.md` — regras que a
  futura implementação deverá aplicar.
