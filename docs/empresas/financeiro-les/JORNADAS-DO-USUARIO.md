# Jornadas do Usuário — Financeiro LeS

As jornadas consideram dois usuários com o mesmo acesso, empresa atual validada e proteção
multiempresa. Cada fluxo preserva dados digitados após erro seguro e impede submissão duplicada.

## 1. Primeiro acesso

- **Entrada:** aplicativo Financeiro LeS na Criati.
- **Passos:** validar sessão/empresa; abrir Dashboard; detectar configuração ausente; mostrar
  checklist de pessoas, contas, categorias, cartões, orçamento e meta.
- **Decisões:** iniciar configuração ou sair; não criar dados fictícios.
- **Mensagem:** “Vamos preparar a visão financeira da residência. Você poderá revisar tudo.”
- **Resultado:** checklist iniciado e contexto familiar explícito.
- **Erros:** aplicação/vínculo inativo ou sessão expirada levam a destino seguro, sem mostrar dados.

## 2. Cadastro inicial

- **Entrada:** checklist do primeiro acesso.
- **Passos:** confirmar pessoas; cadastrar contas e saldos com data; categorias; cartões e vínculo
  principal/virtual; orçamento; meta; revisar resumo.
- **Decisões:** pular item opcional ou corrigir dados antes de concluir.
- **Mensagem:** progresso e aviso do efeito de dados faltantes nas projeções.
- **Resultado:** Dashboard inicial, com estimativas e parcialidade rotuladas.
- **Erros:** duplicidade, limite saudável maior que bancário, virtual sem principal ou valor inválido.

## 3. Registrar gasto no débito

- **Entrada:** `Lançar` ou Lançamentos.
- **Passos:** escolher despesa; informar valor, data, conta, categoria, descrição e pessoa; comparar
  possível duplicidade; salvar.
- **Decisões:** previsto/realizado e residencial/terceiro quando aplicável.
- **Mensagem:** confirmação com valor/conta ou candidatos semelhantes.
- **Resultado:** gasto aparece em consumo e caixa conforme o estado.
- **Erros:** conta/categoria inativa, valor inválido, concorrência ou falha ao salvar.

## 4. Registrar compra parcelada

- **Entrada:** Cartões/Compras > `Registrar compra`.
- **Passos:** informar cartão, total, data, parcelas, categoria, pessoa, estabelecimento e
  residencial/terceiro; revisar parcela, primeira fatura e limite; confirmar.
- **Decisões:** cartão, quantidade e natureza da compra.
- **Mensagem:** “R$ X ocuparão o limite e serão N parcelas de R$ Y.”
- **Resultado:** compra e parcelas vinculadas às faturas sem duplicação.
- **Erros:** cartão inativo, fechamento indefinido, duplicidade ou falha atômica na geração.

## 5. Pagar fatura integralmente

- **Entrada:** alerta, agenda, lista ou detalhe da fatura.
- **Passos:** revisar composição; informar conta, data e valor total; anexar comprovante opcional;
  confirmar.
- **Decisões:** resolver diferença entre total calculado e informado antes de pagar.
- **Mensagem:** “O pagamento reduz o caixa e não cria nova despesa de consumo.”
- **Resultado:** pagamento registrado, fatura paga e caixa atualizado.
- **Erros:** fatura já paga, conta inativa, divergência ou atualização concorrente.

## 6. Pagar fatura parcialmente

- **Entrada:** detalhe da fatura > `Registrar pagamento`.
- **Passos:** informar valor menor; ver saldo e possíveis encargos; confirmar; registrar.
- **Decisões:** voltar ao integral ou aceitar parcial; excedente exige classificação.
- **Mensagem:** “Restarão R$ X, sujeitos a juros ou encargos.”
- **Resultado:** status Parcialmente paga e risco atualizado.
- **Erros:** valor zero, excedente sem classificação ou mudança concorrente.

## 7. Simular compra

- **Entrada:** `Simular nova despesa`.
- **Passos:** informar operação/valor; forma, data, categoria e parcelas; calcular; comparar cenários;
  abrir justificativa.
- **Decisões:** débito, crédito, parcelamento, aguardar ou adiar; salvar ou descartar.
- **Mensagem:** recomendação numérica, nível e hipóteses; base incompleta é declarada.
- **Resultado:** decisão informada ou registro real após nova revisão.
- **Erros:** dados ausentes/desatualizados ou cálculo indisponível impedem conclusão categórica.

## 8. Cadastrar conta recorrente

- **Entrada:** Contas a pagar > `Cadastrar`.
- **Passos:** informar favorecido, categoria, valor fixo/variável, periodicidade e datas; visualizar
  ocorrências; confirmar.
- **Decisões:** frequência, valor e geração inicial.
- **Mensagem:** resumo; variável é previsão a confirmar.
- **Resultado:** recorrência e ocorrências futuras sem duplicação.
- **Erros:** periodicidade incompleta, intervalo inválido ou ocorrência coincidente.

## 9. Registrar empréstimo concedido

- **Entrada:** Empréstimos > `Cadastrar`.
- **Passos:** devedor, principal, conta, datas/parcelas, juros/multa e comprovante; revisar; confirmar.
- **Decisões:** novo/antigo em andamento e condições.
- **Mensagem:** “É exposição a terceiros, não consumo nem saldo disponível.”
- **Resultado:** empréstimo aberto e recebíveis criados.
- **Erros:** cronograma incoerente ou conta inválida.

## 10. Registrar compra para terceiro

- **Entrada:** Compras para terceiros ou compra no crédito.
- **Passos:** marcar terceiro; informar terceiro, cartão, compra e acordo de reembolso; comparar
  cronogramas; confirmar.
- **Decisões:** parcelas da fatura e do recebível iguais ou diferentes.
- **Mensagem:** obrigação do cartão e reembolso aparecem separados.
- **Resultado:** exposição criada e compra fora do consumo residencial.
- **Erros:** terceiro ausente, cronograma incoerente ou cartão inativo.

## 11. Receber parcela de terceiro

- **Entrada:** Valores a receber ou detalhe da origem.
- **Passos:** selecionar recebível; informar conta, data/valor; comparar saldo; anexar; salvar.
- **Decisões:** parcial/total; excedente como juros, multa, adiantamento ou não identificado.
- **Mensagem:** saldo anterior, recebido e posterior; excedente não vira receita comum.
- **Resultado:** recebível e caixa atualizados sem alterar consumo.
- **Erros:** valor inválido, recebível quitado, conta inativa ou concorrência.

## 12. Importar extrato — fase seguinte

- **Entrada:** conta/cartão > `Importar` ou Conciliação.
- **Passos:** escolher origem/arquivo; validar; pré-visualizar; identificar duplicidade; confirmar.
- **Decisões:** corrigir, ignorar linha inválida ou cancelar; não conciliar automaticamente.
- **Mensagem:** totais válidos, inválidos e duplicados; progresso do processamento.
- **Resultado:** transações Não analisadas.
- **Erros:** arquivo ilegível, origem/período errado, tamanho excedido ou parcialidade.

## 13. Conciliar lançamento — fase seguinte

- **Entrada:** importação ou Conciliação.
- **Passos:** comparar original e candidatos; escolher/criar lançamento; confirmar; revisar lote.
- **Decisões:** conciliar, criar, duplicado ou ignorar; baixa confiança exige escolha manual.
- **Mensagem:** critérios de confiança e resumo do lote.
- **Resultado:** vínculo auditável sem dupla contabilização.
- **Erros:** candidato já conciliado, valor alterado ou conflito de lote.

## 14. Corrigir categoria

- **Entrada:** detalhe/lista de lançamento ou compra.
- **Passos:** editar; mostrar alcance; escolher categoria; revisar orçamento/histórico; confirmar.
- **Decisões:** compra inteira herda parcelas; conciliado segue regra de ajuste/desconciliação.
- **Mensagem:** registros e competências afetados.
- **Resultado:** categoria atualizada e histórico registrado.
- **Erros:** categoria inativa, estado bloqueado ou concorrência.

## 15. Enviar item para lixeira

- **Entrada:** ações do registro.
- **Passos:** escolher; revisar item e cálculos afetados; confirmar.
- **Decisões:** cancelar ou enviar; vínculos exibem consequência.
- **Mensagem:** item deixa saldo, orçamento, Dashboard e Simulador.
- **Resultado:** item na lixeira com autor/data e cálculos refeitos.
- **Erros:** estado não excluível, dependência impeditiva ou item alterado.

## 16. Restaurar item

- **Entrada:** Lixeira.
- **Passos:** validar conta, categoria, fatura e vínculos; mostrar/resolver conflitos; confirmar.
- **Decisões:** restaurar, corrigir referência ou manter.
- **Mensagem:** impacto da volta aos cálculos.
- **Resultado:** item ativo e restauração auditada.
- **Erros:** vínculo excluído definitivamente, duplicidade ou estado incompatível.

## 17. Consultar risco financeiro

- **Entrada:** alerta/card no Dashboard, cartão ou Simulador.
- **Passos:** abrir nível/valor/período; ver composição; navegar à causa.
- **Decisões:** simular alternativa, agir no registro ou reconhecer.
- **Mensagem:** causa, hipótese e ação, sem alarmismo.
- **Resultado:** risco explicável e próximo passo disponível.
- **Erros:** dados parciais/desatualizados tornam o nível estimado.

## 18. Consultar gasto do mês

- **Entrada:** card `Gasto real`.
- **Passos:** abrir composição; ver débito/crédito sem pagamento duplicado; comparar; filtrar; abrir
  origem.
- **Decisões:** consolidado ou filtro de pessoa/categoria; residencial separado de terceiros.
- **Mensagem:** fórmula, filtros e itens excluídos.
- **Resultado:** total rastreável até cada registro.
- **Erros:** período vazio ou dados parciais oferecem mudança de competência/complementação.

## Regras comuns

- Alteração concorrente exige recarregar os valores antes de confirmar ação financeira.
- Recurso de outra empresa não é revelado; o usuário recebe resposta genérica e destino seguro.
- Histórico crítico não registra senha, token, cookie ou conteúdo integral de documento.

## Documentos relacionados

- `NAVEGACAO.md`, `ESPECIFICACAO-DAS-TELAS.md` e `PROCESSOS.md`.
