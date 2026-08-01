package br.app.criati.financeiro.web;

import br.app.criati.financeiro.service.ResumoImportacaoBancaria;

public record ResumoImportacaoBancariaResponse(
		long pendentes,
		long confirmadas,
		long ignoradas,
		long duplicadas) {

	public static ResumoImportacaoBancariaResponse from(ResumoImportacaoBancaria resumo) {
		return new ResumoImportacaoBancariaResponse(
				resumo.pendentes(), resumo.confirmadas(), resumo.ignoradas(), resumo.duplicadas());
	}
}
