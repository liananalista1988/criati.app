# Invariantes — Financeiro LeS

Regras estruturais que o modelo de dados e a arquitetura funcional devem sempre garantir,
independentemente de qualquer fluxo específico. Uma violação de qualquer invariante aqui listada
deve ser tratada como um defeito do sistema, nunca como um comportamento aceitável em um caso
extremo. Complementa (não substitui) as regras de negócio já registradas em
`REGRAS-DE-NEGOCIO.md`, que são específicas de processo.

## Lista obrigatória (mínima, conforme solicitado nesta tarefa)

1. **Isolamento entre empresas** — nenhuma entidade financeira de uma empresa se relaciona com
   entidade financeira de outra empresa; toda consulta busca por `id + empresa_id` do contexto
   ativo, nunca por `id` isolado (`ARQUITETURA-FUNCIONAL.md`, seção 3).
2. **Pagamento de fatura não é novo consumo** — um `PagamentoFatura` gera, no máximo, um
   `LancamentoFinanceiro` de natureza `PAGAMENTO_FATURA`, que nunca soma no cálculo de gasto real
   (`MODELO-DE-DADOS.md`, seção 1).
3. **Transferência própria não altera o resultado consolidado** — um `LancamentoFinanceiro` de
   natureza `TRANSFERENCIA_SAIDA`/`TRANSFERENCIA_ENTRADA` nunca é somado como receita nem despesa
   no cálculo de gasto real ou de renda consolidada.
4. **Compra para terceiro não é consumo residencial** — uma `CompraCredito` com
   `e_para_terceiro=true` é sempre excluída do cálculo de gasto real/consumo residencial,
   independentemente de já ter sido reembolsada ou não.
5. **Empréstimo concedido não é consumo** — um `EmprestimoConcedido` nunca é somado a nenhum
   total de despesa ou gasto real; ele reduz apenas o saldo consolidado da conta de origem no
   momento em que é feito.
6. **Empréstimo recebido não é renda** — um `CompromissoAPagar` de origem
   `EMPRESTIMO_RECEBIDO` nunca é somado à renda do mês nem ao cálculo de receita consolidada.
7. **Recebível não é saldo disponível** — o saldo de qualquer `Recebivel` em aberto nunca é
   somado ao saldo consolidado ou à disponibilidade segura como se já estivesse recebido; é
   sempre um indicador separado (exposição financeira a terceiros).
8. **Parcela pertence a uma única compra** — toda `ParcelaCompra` tem exatamente um
   `compra_id`; não existe parcela órfã de forma válida (uma parcela sem compra original
   identificada, quando ocorrer por falha de importação, fica em um estado de revisão manual, sem
   compor nenhum total até ser associada).
9. **Parcela ativa pertence a no máximo uma fatura** — uma `ParcelaCompra` que não esteja
   cancelada, estornada ou na lixeira nunca aparece simultaneamente em duas faturas.
10. **Cartão virtual compartilha fatura e limite com o principal** — um `CartaoCredito` com
    `fisico_ou_virtual=VIRTUAL` nunca tem `Fatura` ou limite ocupado próprios; toda compra feita
    nele é resolvida para o `cartao_principal_id` antes de qualquer cálculo de limite ou fatura.
11. **Categoria histórica não desaparece após desativação** — desativar uma `CategoriaFinanceira`
    (`status=INATIVO`) nunca remove nem oculta a categoria de lançamentos, compras ou parcelas já
    existentes; apenas impede que ela seja usada em **novos** registros.
12. **Item na lixeira não entra nos cálculos** — nenhuma entidade com exclusão lógica ativa
    (na lixeira) participa de saldo, orçamento, dashboard, projeção ou Simulador, mesmo que seus
    dados originais continuem fisicamente presentes no banco.
13. **Transação importada não pode ter duas conciliações ativas** — uma `TransacaoImportada` tem,
    no máximo, um `VinculoConciliacao` com status ativo por vez; o mesmo vale para o
    `LancamentoFinanceiro`/`ParcelaCompra` do outro lado do vínculo.
14. **Simulação não altera dados reais** — nenhum cálculo do Simulador de Gastos grava, atualiza
    ou remove qualquer entidade persistida até uma confirmação explícita e separada
    (`ARQUITETURA-FUNCIONAL.md`, seção 5).
15. **Anexos respeitam o tenant** — todo `Anexo` possui `empresa_id` obrigatório e só é acessível
    a usuários com vínculo ativo na mesma empresa da entidade a que está vinculado.
16. **Valores monetários não podem usar ponto flutuante binário** — todo valor monetário do
    Financeiro LeS é representado com um tipo decimal exato (`BigDecimal` na camada de aplicação,
    `NUMERIC(19,2)` no banco, mesmo padrão já usado pelo módulo `FINANCEIRO` genérico, ver
    `docs/FINANCEIRO.md`, "Tratamento monetário") — nunca `float`/`double`.
17. **Datas de competência e liquidação não são equivalentes** — toda entidade que tenha as duas
    (ex.: `LancamentoFinanceiro`, `OcorrenciaContaAPagar`) trata `data_competencia` (a quem/quando
    o valor pertence, para fins de gasto real e orçamento) e `data_pagamento`/`data_liquidacao` (
    quando o dinheiro de fato mudou de mão, para fins de saída efetiva) como campos distintos,
    nunca inferidos um a partir do outro.
18. **Toda alteração financeira relevante deve ser auditável** — criação, alteração de valor/
    categoria/vencimento/status, exclusão (para a lixeira), restauração, conciliação e
    desconciliação de qualquer entidade financeira geram um registro de auditoria com usuário,
    data/hora, entidade, ação, valor anterior e valor novo (`docs/BANCO_DE_DADOS.md`, "Auditoria";
    `ARQUITETURA-FUNCIONAL.md`, seção 10).

## Invariantes adicionais (detalhadas por entidade em `MODELO-DE-DADOS.md`, consolidadas aqui)

19. **Cartão não aponta para cartão principal de outra empresa** — `CartaoCredito.cartao_principal_id`
    é sempre validado contra a mesma `empresa_id` do cartão virtual (caso específico da invariante 1).
20. **Fechamento e vencimento do cartão devem ser válidos** — `dia_fechamento` e `dia_vencimento`
    são sempre dias do mês válidos (1–31, com o mesmo critério de "último dia válido do mês" já
    definido para recorrências em `MODELO-DE-DADOS.md`, seção "Recorrência").
21. **Limite saudável maior que o bancário gera alerta, nunca bloqueio** — quando
    `limite_saudavel > limite_bancario`, o cadastro é aceito, mas sinalizado como inconsistente;
    o sistema nunca impede o cadastro nem o uso do cartão por causa disso.
22. **Cartão cancelado preserva histórico** — cancelar um `CartaoCredito` (`status`) nunca afeta
    compras, parcelas ou faturas já existentes; apenas impede novo uso.
23. **Número da parcela nunca excede o total** — `ParcelaCompra.numero` é sempre menor ou igual a
    `ParcelaCompra.total_parcelas` da mesma compra.
24. **Soma das parcelas corresponde ao valor financiado** — a soma de `ParcelaCompra.valor` de
    todas as parcelas de uma `CompraCredito` é sempre igual ao `valor_total` da compra, com
    qualquer diferença de arredondamento ajustada na última parcela (nunca distribuída de forma
    que a soma final divirja do valor total).
25. **Parcela não existe duas vezes para a mesma compra e número** — unicidade de
    `compra_id + numero` em `ParcelaCompra`.
26. **Uma parcela ativa não pertence a duas faturas** — reafirmação específica da invariante 9,
    aplicada ao momento de vincular uma parcela a uma fatura.
27. **Fechamento de fatura não apaga compras** — fechar uma `Fatura` (transição de estado) nunca
    remove nem oculta as compras ou parcelas já vinculadas a ela; apenas impede que novas compras
    entrem naquele ciclo.
28. **Fatura paga só é ajustada com trilha de auditoria** — qualquer correção em uma `Fatura` já
    com status `Paga` exige um registro de auditoria explícito (invariante 18), nunca uma edição
    silenciosa de valor.

## Documentos relacionados

- `MODELO-DE-DADOS.md` — entidades e campos onde cada invariante se aplica.
- `REGRAS-DE-NEGOCIO.md`, `ESTADOS-E-TRANSICOES.md`, `CALCULOS-E-INDICADORES.md` (LES-F1-002).
- `ARQUITETURA-FUNCIONAL.md` — decisões que garantem estas invariantes na prática.
