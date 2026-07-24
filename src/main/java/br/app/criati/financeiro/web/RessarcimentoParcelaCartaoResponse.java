package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.RessarcimentoParcelaCartao;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.StatusRessarcimentoParcelaCartao;

public record RessarcimentoParcelaCartaoResponse(UUID id, UUID valorAReceberId, UUID contaId, String contaNome,
		BigDecimal valor, LocalDate dataRessarcimento, FormaPagamentoLancamento formaPagamento, String observacao,
		UUID lancamentoFinanceiroId, StatusRessarcimentoParcelaCartao status, OffsetDateTime estornadoEm,
		UUID estornadoPorUsuarioId, String motivoEstorno, OffsetDateTime criadoEm, UUID criadoPorUsuarioId) {

	public static RessarcimentoParcelaCartaoResponse from(RessarcimentoParcelaCartao r) {
		return new RessarcimentoParcelaCartaoResponse(r.getId(), r.getValorAReceber().getId(), r.getConta().getId(),
				r.getConta().getNome(), r.getValor(), r.getDataRessarcimento(), r.getFormaPagamento(), r.getObservacao(),
				r.getLancamentoFinanceiro().getId(), r.getStatus(), r.getEstornadoEm(),
				r.getEstornadoPor() == null ? null : r.getEstornadoPor().getId(), r.getMotivoEstorno(), r.getCriadoEm(),
				r.getCriadoPor().getId());
	}
}
