package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ResumoCartoesCredito(long quantidadeAtivos, long quantidadeFisicos, long quantidadeVirtuais,
		long quantidadeBloqueados, BigDecimal limiteTotalConsolidado, BigDecimal limiteSaudavelConsolidado,
		BigDecimal limiteDisponivelConsolidado,
		// CRIATI-FIN-015: quebra informativa de quanto do limite comprometido total
		// (limiteTotalConsolidado - limiteDisponivelConsolidado) pertence a compras da
		// residencia vs. compras para terceiros. Compra para terceiro continua
		// consumindo limite normalmente — esta quebra e so para relatorio, nunca
		// remove terceiros do calculo de limite disponivel.
		BigDecimal limiteComprometidoResidencia, BigDecimal limiteComprometidoTerceiros,
		List<CartoesPorTitular> porTitular, List<CartoesPorInstituicao> porInstituicao) {

	public record CartoesPorTitular(UUID titularId, String titularNome, long quantidade) {
	}

	public record CartoesPorInstituicao(UUID instituicaoId, String instituicaoNome, long quantidade) {
	}
}
