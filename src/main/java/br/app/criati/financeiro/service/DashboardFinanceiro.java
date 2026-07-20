package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import br.app.criati.financeiro.model.LancamentoFinanceiro;

public record DashboardFinanceiro(
		YearMonth competencia,
		BigDecimal saldoInicialConsolidado,
		BigDecimal receitasPagas,
		BigDecimal despesasPagas,
		BigDecimal resultadoMes,
		BigDecimal totalPendenteReceber,
		BigDecimal totalPendentePagar,
		BigDecimal saldoAtualConsolidado,
		long quantidadeContasAtivas,
		long quantidadeLancamentosPeriodo,
		List<ResumoCategoriaFinanceira> resumoPorCategoria,
		List<LancamentoFinanceiro> ultimosLancamentos) {
}
