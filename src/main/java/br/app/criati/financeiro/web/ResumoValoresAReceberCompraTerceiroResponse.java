package br.app.criati.financeiro.web;

import java.math.BigDecimal;

import br.app.criati.financeiro.service.ValorAReceberParcelaCartaoService.ResumoValoresAReceberCompraTerceiro;

public record ResumoValoresAReceberCompraTerceiroResponse(BigDecimal totalPrincipal, BigDecimal totalRessarcido,
		BigDecimal saldoAReceber, long quantidadePendente, long quantidadeParcialmenteRessarcida,
		long quantidadeRessarcida, long quantidadeVencida, long quantidadeVencendoEmBreve) {

	public static ResumoValoresAReceberCompraTerceiroResponse from(ResumoValoresAReceberCompraTerceiro r) {
		return new ResumoValoresAReceberCompraTerceiroResponse(r.totalPrincipal(), r.totalRessarcido(),
				r.saldoAReceber(), r.quantidadePendente(), r.quantidadeParcialmenteRessarcida(),
				r.quantidadeRessarcida(), r.quantidadeVencida(), r.quantidadeVencendoEmBreve());
	}
}
