package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.shared.enums.StatusLancamentoFinanceiro;
import br.app.criati.shared.enums.TipoFinanceiro;

public record LancamentoFinanceiroResponse(
		UUID id,
		UUID contaId,
		String contaNome,
		UUID categoriaId,
		String categoriaNome,
		TipoFinanceiro tipo,
		String descricao,
		BigDecimal valor,
		LocalDate dataCompetencia,
		LocalDate dataPagamento,
		StatusLancamentoFinanceiro status,
		String observacao) {

	public static LancamentoFinanceiroResponse from(LancamentoFinanceiro lancamento) {
		return new LancamentoFinanceiroResponse(
				lancamento.getId(),
				lancamento.getConta().getId(),
				lancamento.getConta().getNome(),
				lancamento.getCategoria().getId(),
				lancamento.getCategoria().getNome(),
				lancamento.getTipo(),
				lancamento.getDescricao(),
				lancamento.getValor(),
				lancamento.getDataCompetencia(),
				lancamento.getDataPagamento(),
				lancamento.getStatus(),
				lancamento.getObservacao());
	}
}
