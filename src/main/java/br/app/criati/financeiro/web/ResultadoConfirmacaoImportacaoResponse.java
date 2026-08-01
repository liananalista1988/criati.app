package br.app.criati.financeiro.web;

import java.util.List;

import br.app.criati.financeiro.service.ResultadoConfirmacaoImportacao;

public record ResultadoConfirmacaoImportacaoResponse(
		List<TransacaoBancariaImportadaResponse> transacoes,
		ResumoImportacaoBancariaResponse resumo) {

	public static ResultadoConfirmacaoImportacaoResponse from(ResultadoConfirmacaoImportacao resultado) {
		return new ResultadoConfirmacaoImportacaoResponse(
				resultado.transacoes().stream().map(TransacaoBancariaImportadaResponse::from).toList(),
				ResumoImportacaoBancariaResponse.from(resultado.resumo()));
	}
}
