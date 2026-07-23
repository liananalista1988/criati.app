package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.EmprestimoConcedido;
import br.app.criati.shared.enums.FormaPagamentoEmprestimo;
import br.app.criati.shared.enums.StatusEmprestimoConcedido;
import br.app.criati.shared.enums.TipoCobrancaEmprestimo;

public record EmprestimoConcedidoResponse(UUID id, UUID parteFinanceiraId, String parteFinanceiraNome,
		UUID categoriaId, String categoriaNome, String descricao, BigDecimal valorPrincipal, LocalDate dataConcessao,
		TipoCobrancaEmprestimo tipoCobranca, BigDecimal percentualJuros, BigDecimal percentualMulta,
		FormaPagamentoEmprestimo formaPagamento, int quantidadeParcelas, StatusEmprestimoConcedido status,
		String motivoCancelamento, OffsetDateTime canceladoEm, UUID canceladoPorUsuarioId, OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm, UUID criadoPorUsuarioId, UUID atualizadoPorUsuarioId) {

	public static EmprestimoConcedidoResponse from(EmprestimoConcedido e) {
		return new EmprestimoConcedidoResponse(e.getId(), e.getParteFinanceira().getId(), e.getParteFinanceira().getNome(),
				e.getCategoria().getId(), e.getCategoria().getNome(), e.getDescricao(), e.getValorPrincipal(),
				e.getDataConcessao(), e.getTipoCobranca(), e.getPercentualJuros(), e.getPercentualMulta(),
				e.getFormaPagamento(), e.getQuantidadeParcelas(), e.getStatus(), e.getMotivoCancelamento(),
				e.getCanceladoEm(), e.getCanceladoPor() == null ? null : e.getCanceladoPor().getId(), e.getCriadoEm(),
				e.getAtualizadoEm(), e.getCriadoPor() == null ? null : e.getCriadoPor().getId(),
				e.getAtualizadoPor() == null ? null : e.getAtualizadoPor().getId());
	}
}
