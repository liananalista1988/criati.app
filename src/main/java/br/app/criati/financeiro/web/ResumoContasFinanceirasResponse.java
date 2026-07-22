package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.app.criati.financeiro.service.ResumoContasFinanceiras;
import br.app.criati.financeiro.service.SaldoInicialPorTitular;

public record ResumoContasFinanceirasResponse(
		long quantidadeContasAtivas,
		BigDecimal saldoInicialConsolidado,
		List<SaldoInicialTitularResponse> saldosPorTitular) {

	public static ResumoContasFinanceirasResponse from(ResumoContasFinanceiras resumo) {
		return new ResumoContasFinanceirasResponse(
				resumo.quantidadeContasAtivas(),
				resumo.saldoInicialConsolidado(),
				resumo.saldosPorTitular().stream().map(SaldoInicialTitularResponse::from).toList());
	}

	public record SaldoInicialTitularResponse(UUID titularId, String titularNome, BigDecimal saldoInicial) {
		private static SaldoInicialTitularResponse from(SaldoInicialPorTitular saldo) {
			return new SaldoInicialTitularResponse(saldo.titularId(), saldo.titularNome(), saldo.saldoInicial());
		}
	}
}
