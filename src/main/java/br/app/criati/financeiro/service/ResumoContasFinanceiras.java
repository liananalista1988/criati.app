package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.util.List;

public record ResumoContasFinanceiras(
		long quantidadeContasAtivas,
		BigDecimal saldoInicialConsolidado,
		List<SaldoInicialPorTitular> saldosPorTitular) {
}
