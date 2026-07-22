package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.app.criati.financeiro.service.ResumoCartoesCredito;

public record ResumoCartoesCreditoResponse(long quantidadeAtivos, long quantidadeFisicos, long quantidadeVirtuais,
		long quantidadeBloqueados, BigDecimal limiteTotalConsolidado, BigDecimal limiteSaudavelConsolidado,
		BigDecimal limiteDisponivelConsolidado, List<CartoesPorTitularResponse> porTitular,
		List<CartoesPorInstituicaoResponse> porInstituicao) {

	public record CartoesPorTitularResponse(UUID titularId, String titularNome, long quantidade) {
	}

	public record CartoesPorInstituicaoResponse(UUID instituicaoId, String instituicaoNome, long quantidade) {
	}

	public static ResumoCartoesCreditoResponse from(ResumoCartoesCredito r) {
		return new ResumoCartoesCreditoResponse(r.quantidadeAtivos(), r.quantidadeFisicos(), r.quantidadeVirtuais(),
				r.quantidadeBloqueados(), r.limiteTotalConsolidado(), r.limiteSaudavelConsolidado(),
				r.limiteDisponivelConsolidado(),
				r.porTitular().stream()
						.map(t -> new CartoesPorTitularResponse(t.titularId(), t.titularNome(), t.quantidade()))
						.toList(),
				r.porInstituicao().stream()
						.map(i -> new CartoesPorInstituicaoResponse(i.instituicaoId(), i.instituicaoNome(), i.quantidade()))
						.toList());
	}
}
