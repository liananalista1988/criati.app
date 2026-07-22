package br.app.criati.financeiro.service;

import java.math.BigDecimal;

public record ResumoOcorrenciasContaPagar(BigDecimal totalPrevisto, BigDecimal totalPrincipal, BigDecimal totalJuros,
		BigDecimal totalMultas, BigDecimal totalDescontos, BigDecimal totalPago, BigDecimal saldoPendente,
		long quantidadePendente, long quantidadeParcialmentePaga, long quantidadePaga, long quantidadeVencida,
		long quantidadeVencendoEmBreve) {
}
