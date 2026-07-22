# Estados de Interface — Financeiro LeS

Mensagens citam objeto, impacto e ação possível, sem expor SQL, stack trace, caminho local,
credencial ou existência de dados de outra empresa.

## Estados comuns

| Estado | Apresentação | Ação |
| --- | --- | --- |
| Carregando | skeleton rotulado, sem números falsos | aguardar/cancelar se seguro |
| Vazio inicial | benefício e primeiro cadastro | criar |
| Vazio por filtro | filtros e competência ativos | limpar/ajustar |
| Erro recuperável | ação não concluída, sem detalhe interno | tentar/voltar |
| Sem permissão | mensagem genérica | voltar ao Dashboard |
| Dados parciais | fontes ausentes e efeito | completar/ver detalhes |
| Dados estimados | selo, hipótese e atualização | revisar premissas |
| Processamento | progresso/etapa; sem reenvio | acompanhar |
| Sucesso | objeto, valor e consequência | abrir/continuar |
| Atenção/risco | valor, prazo, causa e nível | revisar/simular |
| Offline futuro | dados possivelmente desatualizados | reconectar |

Falha de um bloco não derruba o Dashboard inteiro quando os demais dados são confiáveis. Dado
indisponível nunca aparece como zero.

## Estados vazios específicos

| Tela | Mensagem | Ação |
| --- | --- | --- |
| Dashboard | “Sua visão familiar começa com contas, pessoas e categorias.” | Iniciar configuração |
| Contas | “Nenhuma conta cadastrada. Cadastre uma para acompanhar saldo e movimentos.” | Cadastrar |
| Lançamentos | “Nenhum lançamento nesta competência.” | Lançar/mudar competência |
| Contas a pagar | “Nenhuma conta a pagar neste período.” | Cadastrar |
| Agenda | “Nenhum compromisso nos próximos dias.” | Ver 30 dias |
| Cartões | “Nenhum cartão cadastrado. Cadastre o primeiro para acompanhar faturas e parcelas.” | Cadastrar |
| Compras | “Nenhuma compra encontrada neste recorte.” | Registrar/limpar filtros |
| Faturas | “Ainda não há faturas para este cartão e competência.” | Mudar competência |
| Parcelas | “Nenhuma parcela futura encontrada.” | Ver compras |
| Orçamento | “Defina o orçamento para acompanhar o disponível.” | Definir |
| Meta | “Configure uma meta para acompanhar a economia projetada.” | Configurar |
| Simulador | “Informe uma despesa para comparar o impacto antes de decidir.” | Simular |
| Empréstimos | “Nenhum empréstimo concedido registrado.” | Cadastrar |
| Compromissos | “Nenhum compromisso com terceiros registrado.” | Cadastrar |
| Compras para terceiros | “Nenhuma compra para terceiro registrada.” | Registrar |
| Recebíveis | “Nenhum valor a receber neste recorte.” | Ver períodos |
| Categorias | “Nenhuma categoria disponível.” | Cadastrar |
| Pessoas | “Nenhuma pessoa da residência configurada.” | Adicionar |
| Anexos | “Nenhum comprovante anexado a este registro.” | Enviar |
| Histórico | “Nenhuma alteração registrada neste período.” | Ajustar período |
| Lixeira | “A lixeira está vazia.” | Voltar |
| Conciliação | “Nenhuma transação aguardando análise.” | Importar |
| Regras | “Nenhuma regra de categorização criada.” | Criar |

## Níveis de alerta

| Nível | Uso | Conteúdo obrigatório |
| --- | --- | --- |
| Informativo | contexto, estimativa, variação neutra | ícone, texto e fonte |
| Atenção | aproximação de limite/atraso moderado | valor e ação |
| Alto risco | meta/fatura provavelmente comprometida | causa e projeção |
| Crítico | vencimento/insuficiência imediata | prioridade e ação direta |

Cor apenas reforça ícone, nível e título. Alertas repetidos são agrupados. Dispensar visualmente
não significa resolver a causa.

## Confirmações obrigatórias

### Lixeira e exclusão

- **Enviar:** “`{item}` deixará de participar do saldo, orçamento, Dashboard e Simulador. Você
  poderá restaurá-lo após validação de conflitos.”
- **Definitiva:** “Excluir `{item}` definitivamente? Esta ação é irreversível.” A ação segura é
  `Manter na lixeira`; exclusão nunca recebe foco padrão.

### Pagamentos

- **Parcial:** “Você pagará R$ {pago} de R$ {saldo}. Restarão R$ {restante}, que podem gerar
  encargos. Registrar pagamento parcial?”
- **Fatura menor:** “A fatura ficará Parcialmente paga e o saldo continuará nos alertas.”
- **Excedente:** “O valor excede o saldo em R$ {excedente}. Classifique como juros, multa,
  adiantamento ou não identificado.” Não oferecer `Receita comum` automática.

### Parcelas e terceiros

- **Parcelas:** “R$ {total} ocuparão o limite total e criarão {n} parcelas a partir de
  {competencia}; impacto mensal inicial R$ {parcela}.”
- **Terceiro:** “A compra ficará separada do consumo residencial. A obrigação da fatura continua
  com a família até o pagamento.”

### Duplicidade e conciliação

- **Duplicidade:** “Encontramos {n} registros semelhantes por valor, data e origem. Compare antes
  de continuar.” Ações `Comparar`, `Voltar`, `Registrar mesmo assim`.
- **Conciliar:** “A transação será vinculada ao candidato e não será contada duas vezes.”
- **Desconciliar:** “A transação voltará para análise; o lançamento não será excluído.”

### Limite e meta

- **Limite saudável:** “O comprometimento é {percentual}% de R$ {saudavel}. O limite bancário é
  R$ {bancario}; o registro não será bloqueado.”
- **Meta:** “A economia projetada é R$ {projetado}, abaixo do alvo em R$ {diferenca}. Simule
  alternativas.”

### Alterações não salvas

“Há alterações não salvas. Descartar agora?” A ação padrão é `Continuar editando`.

## Feedback

- “Lançamento de R$ {valor} registrado em {data}.”
- “Pagamento de R$ {valor} registrado. Saldo restante: R$ {saldo}.”
- “{Item} restaurado e incluído novamente nos cálculos.”
- “Não foi possível concluir {ação}. Seus dados foram preservados. Tente novamente.”
- “Este registro mudou desde que você o abriu. Revise os valores atualizados.”
- “Sua sessão expirou. Entre novamente para continuar com segurança.”

Consequência financeira importante permanece refletida na tela; não depende apenas de toast.

## Arquivos

Estados: Aguardando envio, Enviando, Processando, Disponível, Inválido, Tamanho excedido, Falha e
Excluído. Erro informa formatos/limite, não o caminho interno. Falha não cria anexo parcial.

## Natureza dos dados

- **Real:** confirmado.
- **Previsto:** esperado, ainda não confirmado.
- **Simulado:** existe somente no cenário.
- **Informado:** veio de entrada/fonte identificada.
- **Calculado:** deriva de registros e abre composição.
- **Estimado:** depende de hipótese visível.

Gráficos usam rótulos/padrões além de cor, resumo textual e tabela equivalente.

## Offline futuro

O MVP não promete operação offline. Em perda de conexão, campos podem ser preservados somente se
seguro; pagamento, recebimento e conciliação não são confirmados sem resposta do servidor.

## Documentos relacionados

- `EXPERIENCIA-DO-USUARIO.md`, `ESTADOS-E-TRANSICOES.md` e `JORNADAS-DO-USUARIO.md`.
