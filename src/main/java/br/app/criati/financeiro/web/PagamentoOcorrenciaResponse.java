package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.PagamentoOcorrenciaCompromisso;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.StatusPagamentoOcorrencia;

public record PagamentoOcorrenciaResponse(UUID id, UUID ocorrenciaId, UUID contaId, String contaNome,
		BigDecimal valor, LocalDate dataPagamento, FormaPagamentoLancamento formaPagamento, String observacao,
		UUID lancamentoFinanceiroId, StatusPagamentoOcorrencia status, OffsetDateTime estornadoEm,
		UUID estornadoPorUsuarioId, String motivoEstorno, OffsetDateTime criadoEm, UUID criadoPorUsuarioId) {

	public static PagamentoOcorrenciaResponse from(PagamentoOcorrenciaCompromisso p) {
		return new PagamentoOcorrenciaResponse(p.getId(), p.getOcorrencia().getId(), p.getConta().getId(),
				p.getConta().getNome(), p.getValor(), p.getDataPagamento(), p.getFormaPagamento(), p.getObservacao(),
				p.getLancamentoFinanceiro().getId(), p.getStatus(), p.getEstornadoEm(),
				p.getEstornadoPor() == null ? null : p.getEstornadoPor().getId(), p.getMotivoEstorno(),
				p.getCriadoEm(), p.getCriadoPor().getId());
	}
}
