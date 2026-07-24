package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.util.List;

import br.app.criati.financeiro.service.CompraTerceiroService.ResultadoCompraTerceiro;

public record ResultadoCompraTerceiroResponse(CompraCartaoResponse compra, BigDecimal limiteAntes,
		BigDecimal limiteDepois, BigDecimal limiteDisponivel, boolean alertaLimiteSaudavel,
		List<ValorAReceberParcelaCartaoResponse> valoresAReceber) {

	public static ResultadoCompraTerceiroResponse from(ResultadoCompraTerceiro resultado) {
		var r = resultado.resultadoCompra();
		return new ResultadoCompraTerceiroResponse(CompraCartaoResponse.from(r.compra()), r.limiteAntes(),
				r.limiteDepois(), r.limiteDisponivel(), r.alertaLimiteSaudavel(),
				resultado.valoresAReceber().stream().map(ValorAReceberParcelaCartaoResponse::from).toList());
	}
}
