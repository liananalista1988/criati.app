package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.PagamentoFaturaCartao;
import br.app.criati.shared.enums.TipoPagamentoFaturaCartao;

public record PagamentoFaturaCartaoResponse(
		UUID id,
		UUID faturaId,
		UUID contaPagamentoId,
		String contaPagamentoNome,
		LocalDate dataPagamento,
		BigDecimal valor,
		TipoPagamentoFaturaCartao tipo,
		UUID lancamentoFinanceiroId,
		OffsetDateTime criadoEm,
		UUID criadoPorUsuarioId) {

	public static PagamentoFaturaCartaoResponse from(PagamentoFaturaCartao pagamento) {
		return new PagamentoFaturaCartaoResponse(pagamento.getId(), pagamento.getFatura().getId(),
				pagamento.getContaPagamento().getId(), pagamento.getContaPagamento().getNome(),
				pagamento.getDataPagamento(), pagamento.getValor(), pagamento.getTipo(),
				pagamento.getLancamentoFinanceiro().getId(), pagamento.getCriadoEm(),
				pagamento.getCriadoPor().getId());
	}
}
