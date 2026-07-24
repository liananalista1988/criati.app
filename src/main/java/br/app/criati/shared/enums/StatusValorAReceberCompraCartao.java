package br.app.criati.shared.enums;

/**
 * Status persistido de um ValorAReceberParcelaCartao. Nao inclui ATRASADA:
 * atraso e sempre derivado (comparando o vencimento da parcela de origem com
 * a data de referencia), nunca armazenado como transicao propria — mesmo
 * principio ja usado por StatusParcelaEmprestimo/estaAtrasada.
 */
public enum StatusValorAReceberCompraCartao {
	PENDENTE,
	PARCIALMENTE_RESSARCIDA,
	RESSARCIDA,
	CANCELADA
}
