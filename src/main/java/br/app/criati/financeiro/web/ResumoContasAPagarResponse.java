package br.app.criati.financeiro.web;

import java.math.BigDecimal;

import br.app.criati.financeiro.service.ResumoOcorrenciasContaPagar;

public record ResumoContasAPagarResponse(BigDecimal totalPrevisto, BigDecimal totalPrincipal, BigDecimal totalJuros,
		BigDecimal totalMultas, BigDecimal totalDescontos, BigDecimal totalPago, BigDecimal saldoPendente,
		long quantidadePendente, long quantidadeParcialmentePaga, long quantidadePaga, long quantidadeVencida,
		long quantidadeVencendoEmBreve) {

	public static ResumoContasAPagarResponse from(ResumoOcorrenciasContaPagar r) {
		return new ResumoContasAPagarResponse(r.totalPrevisto(), r.totalPrincipal(), r.totalJuros(), r.totalMultas(),
				r.totalDescontos(), r.totalPago(), r.saldoPendente(), r.quantidadePendente(),
				r.quantidadeParcialmentePaga(), r.quantidadePaga(), r.quantidadeVencida(),
				r.quantidadeVencendoEmBreve());
	}
}
