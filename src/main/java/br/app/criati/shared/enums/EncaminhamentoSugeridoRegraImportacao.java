package br.app.criati.shared.enums;

/**
 * Rotulo informativo de encaminhamento sugerido por uma regra de
 * classificacao de importacao - nunca uma chave estrangeira para contrato,
 * parcela ou fatura especifica. FATURA_CARTAO orienta o fluxo de
 * confirmacao a oferecer a selecao manual de uma fatura em aberto
 * (ConfirmacaoImportacaoBancariaService), nunca escolhe uma automaticamente.
 * Os valores de emprestimo apenas orientam o usuario a usar o fluxo de
 * Emprestimos ja existente - nenhum contrato/parcela e criado a partir daqui.
 */
public enum EncaminhamentoSugeridoRegraImportacao {
	FATURA_CARTAO,
	EMPRESTIMO_RECEBIDO,
	EMPRESTIMO_CONCEDIDO,
	EMPRESTIMO_RECEBIMENTO_PARCELA,
	EMPRESTIMO_PAGAMENTO_PARCELA
}
