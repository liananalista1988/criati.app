package br.app.criati.financeiro.web;

import java.math.BigDecimal;

import br.app.criati.financeiro.service.ParcelaEmprestimoService.ResumoEmprestimosConcedidos;

public record ResumoEmprestimosConcedidosResponse(BigDecimal totalPrincipal, BigDecimal totalJuros,
		BigDecimal totalMultas, BigDecimal totalRecebido, BigDecimal saldoAReceber, long quantidadePendente,
		long quantidadeParcialmentePaga, long quantidadePaga, long quantidadeVencida, long quantidadeVencendoEmBreve) {

	public static ResumoEmprestimosConcedidosResponse from(ResumoEmprestimosConcedidos r) {
		return new ResumoEmprestimosConcedidosResponse(r.totalPrincipal(), r.totalJuros(), r.totalMultas(),
				r.totalRecebido(), r.saldoAReceber(), r.quantidadePendente(), r.quantidadeParcialmentePaga(),
				r.quantidadePaga(), r.quantidadeVencida(), r.quantidadeVencendoEmBreve());
	}
}
