package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.RecebimentoParcelaEmprestimo;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.StatusRecebimentoParcelaEmprestimo;

public record RecebimentoParcelaEmprestimoResponse(UUID id, UUID parcelaId, UUID contaId, String contaNome,
		BigDecimal valor, LocalDate dataRecebimento, FormaPagamentoLancamento formaPagamento, String observacao,
		UUID lancamentoFinanceiroId, StatusRecebimentoParcelaEmprestimo status, OffsetDateTime estornadoEm,
		UUID estornadoPorUsuarioId, String motivoEstorno, OffsetDateTime criadoEm, UUID criadoPorUsuarioId) {

	public static RecebimentoParcelaEmprestimoResponse from(RecebimentoParcelaEmprestimo r) {
		return new RecebimentoParcelaEmprestimoResponse(r.getId(), r.getParcela().getId(), r.getConta().getId(),
				r.getConta().getNome(), r.getValor(), r.getDataRecebimento(), r.getFormaPagamento(), r.getObservacao(),
				r.getLancamentoFinanceiro().getId(), r.getStatus(), r.getEstornadoEm(),
				r.getEstornadoPor() == null ? null : r.getEstornadoPor().getId(), r.getMotivoEstorno(),
				r.getCriadoEm(), r.getCriadoPor().getId());
	}
}
