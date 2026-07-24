package br.app.criati.shared.enums;

/**
 * Situacao efetiva de um ValorAReceberParcelaCartao para leitura (resposta
 * HTTP, consultas de vencidas/proximas do vencimento): igual ao
 * StatusValorAReceberCompraCartao persistido, exceto quando o vencimento da
 * parcela de origem ja passou e ainda ha saldo pendente — nesse caso
 * ATRASADA substitui PENDENTE/PARCIALMENTE_RESSARCIDA. Ver
 * ValorAReceberParcelaCartao#getSituacao(LocalDate).
 */
public enum SituacaoValorAReceberCompraCartao {
	PENDENTE,
	PARCIALMENTE_RESSARCIDA,
	ATRASADA,
	RESSARCIDA,
	CANCELADA
}
