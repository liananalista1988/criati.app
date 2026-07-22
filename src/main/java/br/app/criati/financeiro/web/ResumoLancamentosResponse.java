package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import br.app.criati.financeiro.service.ResumoLancamentosFinanceiros;
import br.app.criati.financeiro.service.SaldoContaFinanceira;

public record ResumoLancamentosResponse(YearMonth competencia, BigDecimal receitasLiquidadas,
		BigDecimal despesasLiquidadas, BigDecimal resultadoLiquidado, BigDecimal receitasPendentes,
		BigDecimal despesasPendentes, long quantidadeVencidos, List<SaldoContaFinanceira> saldosPorConta,
		BigDecimal saldoConsolidado) {
	public static ResumoLancamentosResponse from(ResumoLancamentosFinanceiros r) {
		return new ResumoLancamentosResponse(r.competencia(), r.receitasLiquidadas(), r.despesasLiquidadas(),
				r.resultadoLiquidado(), r.receitasPendentes(), r.despesasPendentes(), r.quantidadeVencidos(),
				r.saldosPorConta(), r.saldoConsolidado());
	}
}
