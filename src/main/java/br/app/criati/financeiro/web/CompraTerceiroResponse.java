package br.app.criati.financeiro.web;

import java.util.List;

import br.app.criati.financeiro.model.CompraCartao;
import br.app.criati.financeiro.model.ValorAReceberParcelaCartao;
import br.app.criati.financeiro.service.CompraTerceiroService.DetalheCompraTerceiro;

public record CompraTerceiroResponse(CompraCartaoResponse compra, List<ValorAReceberParcelaCartaoResponse> valoresAReceber) {

	public static CompraTerceiroResponse from(CompraCartao compra, List<ValorAReceberParcelaCartao> valores) {
		return new CompraTerceiroResponse(CompraCartaoResponse.from(compra),
				valores.stream().map(ValorAReceberParcelaCartaoResponse::from).toList());
	}

	public static CompraTerceiroResponse from(DetalheCompraTerceiro detalhe) {
		return from(detalhe.compra(), detalhe.valoresAReceber());
	}
}
