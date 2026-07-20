package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.util.List;

import br.app.criati.financeiro.service.DashboardFinanceiro;

public record DashboardFinanceiroResponse(
		String competencia,
		BigDecimal saldoInicialConsolidado,
		BigDecimal receitasPagas,
		BigDecimal despesasPagas,
		BigDecimal resultadoMes,
		BigDecimal totalPendenteReceber,
		BigDecimal totalPendentePagar,
		BigDecimal saldoAtualConsolidado,
		long quantidadeContasAtivas,
		long quantidadeLancamentosPeriodo,
		List<ResumoCategoriaResponse> resumoPorCategoria,
		List<LancamentoFinanceiroResponse> ultimosLancamentos) {

	public static DashboardFinanceiroResponse from(DashboardFinanceiro dashboard) {
		return new DashboardFinanceiroResponse(
				dashboard.competencia().toString(),
				dashboard.saldoInicialConsolidado(),
				dashboard.receitasPagas(),
				dashboard.despesasPagas(),
				dashboard.resultadoMes(),
				dashboard.totalPendenteReceber(),
				dashboard.totalPendentePagar(),
				dashboard.saldoAtualConsolidado(),
				dashboard.quantidadeContasAtivas(),
				dashboard.quantidadeLancamentosPeriodo(),
				dashboard.resumoPorCategoria().stream().map(ResumoCategoriaResponse::from).toList(),
				dashboard.ultimosLancamentos().stream().map(LancamentoFinanceiroResponse::from).toList());
	}
}
