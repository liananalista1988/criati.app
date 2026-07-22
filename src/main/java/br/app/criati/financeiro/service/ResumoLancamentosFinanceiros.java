package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record ResumoLancamentosFinanceiros(YearMonth competencia, BigDecimal receitasLiquidadas,
		BigDecimal despesasLiquidadas, BigDecimal resultadoLiquidado, BigDecimal receitasPendentes,
		BigDecimal despesasPendentes, long quantidadeVencidos, List<SaldoContaFinanceira> saldosPorConta,
		BigDecimal saldoConsolidado) { }
