package br.app.criati.financeiro.web;

import java.util.List;

import br.app.criati.financeiro.service.PreviaImportacaoBancaria;

public record PreviaImportacaoBancariaResponse(
		LoteImportacaoBancariaResponse lote,
		List<TransacaoBancariaImportadaResponse> transacoes) {

	public static PreviaImportacaoBancariaResponse from(PreviaImportacaoBancaria previa) {
		return new PreviaImportacaoBancariaResponse(LoteImportacaoBancariaResponse.from(previa.lote()),
				previa.transacoes().stream().map(TransacaoBancariaImportadaResponse::from).toList());
	}

	@Override
	public String toString() {
		return "PreviaImportacaoBancariaResponse[loteId=" + lote.id()
				+ ", quantidadeTransacoes=" + transacoes.size() + "]";
	}
}
