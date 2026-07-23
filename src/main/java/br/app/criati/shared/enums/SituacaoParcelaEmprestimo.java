package br.app.criati.shared.enums;

/**
 * Situacao efetiva de uma ParcelaEmprestimo para leitura (resposta HTTP,
 * consultas de vencidas/proximas do vencimento): igual ao StatusParcelaEmprestimo
 * persistido, exceto quando o vencimento ja passou e ainda ha saldo pendente —
 * nesse caso ATRASADO substitui PENDENTE/PARCIALMENTE_PAGO. Ver
 * ParcelaEmprestimo#getSituacao(LocalDate).
 */
public enum SituacaoParcelaEmprestimo {
	PENDENTE,
	PARCIALMENTE_PAGO,
	ATRASADO,
	PAGO,
	CANCELADO
}
