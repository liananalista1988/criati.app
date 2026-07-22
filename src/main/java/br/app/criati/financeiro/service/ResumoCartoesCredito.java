package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ResumoCartoesCredito(long quantidadeAtivos, long quantidadeFisicos, long quantidadeVirtuais,
		long quantidadeBloqueados, BigDecimal limiteTotalConsolidado, BigDecimal limiteSaudavelConsolidado,
		BigDecimal limiteDisponivelConsolidado, List<CartoesPorTitular> porTitular,
		List<CartoesPorInstituicao> porInstituicao) {

	public record CartoesPorTitular(UUID titularId, String titularNome, long quantidade) {
	}

	public record CartoesPorInstituicao(UUID instituicaoId, String instituicaoNome, long quantidade) {
	}
}
