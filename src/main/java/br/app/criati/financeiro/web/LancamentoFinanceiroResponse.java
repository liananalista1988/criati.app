package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.OrigemLancamentoFinanceiro;
import br.app.criati.shared.enums.StatusLancamentoFinanceiro;
import br.app.criati.shared.enums.TipoFinanceiro;

public record LancamentoFinanceiroResponse(UUID id, UUID contaId, String contaNome,
		UUID categoriaId, String categoriaNome, UUID pessoaFinanceiraId, String pessoaFinanceiraNome,
		UUID parteFinanceiraId, String parteFinanceiraNome, TipoFinanceiro tipo, String descricao,
		BigDecimal valor, LocalDate dataCompetencia, LocalDate dataVencimento, LocalDate dataLiquidacao,
		LocalDate dataPagamento, StatusLancamentoFinanceiro status, boolean vencido,
		FormaPagamentoLancamento formaPagamento, OrigemLancamentoFinanceiro origem,
		BigDecimal impactoSaldo, String observacao, OffsetDateTime criadoEm, OffsetDateTime atualizadoEm,
		UUID criadoPorUsuarioId, UUID atualizadoPorUsuarioId) {

	public static LancamentoFinanceiroResponse from(LancamentoFinanceiro l) {
		BigDecimal impacto = l.compoeSaldoRealizado()
				? (l.getTipo() == TipoFinanceiro.RECEITA ? l.getValor() : l.getValor().negate()) : BigDecimal.ZERO;
		return new LancamentoFinanceiroResponse(l.getId(), l.getConta().getId(), l.getConta().getNome(),
				l.getCategoria().getId(), l.getCategoria().getNome(),
				l.getPessoaFinanceira() == null ? null : l.getPessoaFinanceira().getId(),
				l.getPessoaFinanceira() == null ? null : l.getPessoaFinanceira().getNome(),
				l.getParteFinanceira() == null ? null : l.getParteFinanceira().getId(),
				l.getParteFinanceira() == null ? null : l.getParteFinanceira().getNome(),
				l.getTipo(), l.getDescricao(), l.getValor(), l.getDataCompetencia(), l.getDataVencimento(),
				l.getDataLiquidacao(), l.getDataPagamento(), l.getStatus(), l.estaVencido(LocalDate.now()),
				l.getFormaPagamento(), l.getOrigem(), impacto, l.getObservacao(), l.getCriadoEm(), l.getAtualizadoEm(),
				l.getCriadoPor() == null ? null : l.getCriadoPor().getId(),
				l.getAtualizadoPor() == null ? null : l.getAtualizadoPor().getId());
	}
}
