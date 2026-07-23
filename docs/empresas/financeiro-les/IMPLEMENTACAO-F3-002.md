# LES-F3-002 — Compras no cartão e parcelamento

## Escopo

Esta entrega registra compras à vista e parceladas em cartões físicos ou virtuais, preserva suas parcelas e prepara a referência futura à fatura. Fechamento, pagamento e liberação por quitação de fatura permanecem fora do escopo.

## Persistência e parcelamento

A migration `V13__criar_compras_e_parcelas_cartao.sql` cria `compra_cartao` e `parcela_compra_cartao`, ambas com `empresa_id`, auditoria e índices tenant-aware. A parcela é única por compra e número. Valores usam `BigDecimal`, escala 2. A parcela-base usa arredondamento para baixo; a última recebe `valor total - soma das anteriores`, garantindo igualdade exata. R$ 100,00 em três parcelas resulta em R$ 33,33, R$ 33,33 e R$ 33,34.

## Competência

A competência é a data de vencimento projetada. Calcula-se o fechamento no mês da compra; dia inexistente vira o último dia válido. Compra antes ou no fechamento entra no ciclo corrente; compra posterior entra no seguinte. O vencimento também é limitado ao último dia válido. Parcelas seguintes avançam mensalmente preservando esse dia, ou o último válido.

Exemplo: fechamento 5, vencimento 12. Compra em 5/7 tem primeira competência em 12/7; compra em 6/7, em 12/8.

## Limite, virtual, cancelamento e estorno

O limite fica comprometido pelo valor total de toda compra `ATIVA`, independentemente das parcelas. Mudança de competência não libera limite. Até a futura integração com pagamento da fatura, somente cancelamento ou estorno total neutralizam o valor.

Compra em cartão virtual preserva o cartão usado, mas impacta o principal efetivo. Consolidados somam apenas principais. Ultrapassar o limite saudável gera alerta sem bloqueio; ultrapassar o total é rejeitado. Cartão usado ou principal inativo/bloqueado é rejeitado. Um lock pessimista no principal protege inclusões concorrentes.

Cancelamento e estorno são lógicos: alteram compra e parcelas, registram motivo, usuário e instante e nunca excluem fisicamente. Estorno parcial não é suportado.

## Segurança, API e página

Os endpoints em `/api/contexto/financeiro/compras-cartao` exigem autenticação, empresa ativa e Financeiro habilitado; escritas exigem `ADMINISTRADOR`. Relacionamentos são buscados por `id + empresa_id`, categoria deve ser despesa e `empresaId` nunca vem do cliente. CSRF permanece ativo.

A página `/app/financeiro/compras-cartao` oferece cadastro, filtros, parcelas, cancelamento, estorno e impacto do limite. Fechamento e pagamento de faturas não fazem parte desta entrega.
