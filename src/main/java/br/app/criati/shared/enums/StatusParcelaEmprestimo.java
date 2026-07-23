package br.app.criati.shared.enums;

/**
 * Status persistido de uma ParcelaEmprestimo. Nao inclui ATRASADO: atraso e
 * sempre derivado (parcela.estaAtrasada(hoje)/getSituacao(hoje)), a partir do
 * vencimento e do saldo pendente, nunca armazenado como transicao propria —
 * mesmo principio ja usado por StatusOcorrenciaCompromisso/estaVencida.
 */
public enum StatusParcelaEmprestimo {
	PENDENTE,
	PARCIALMENTE_PAGO,
	PAGO,
	CANCELADO
}
