package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.app.criati.financeiro.model.FaturaCartao;
import br.app.criati.financeiro.service.FaturaCartaoService.ResultadoFatura;
import br.app.criati.shared.enums.StatusFaturaCartao;

public record FaturaCartaoResponse(
		UUID id,
		UUID cartaoPrincipalId,
		LocalDate competencia,
		LocalDate periodoInicial,
		LocalDate periodoFinal,
		LocalDate dataFechamento,
		LocalDate dataVencimento,
		BigDecimal valorTotal,
		BigDecimal saldoFinanciadoAnterior,
		BigDecimal juros,
		BigDecimal multa,
		BigDecimal valorDevido,
		BigDecimal valorPago,
		BigDecimal saldoDevido,
		StatusFaturaCartao status,
		OffsetDateTime fechadoEm,
		long versao,
		List<ParcelaFaturaCartaoResponse> parcelas) {

	// LES-F3-005: valorPago vem sempre de uma consulta a parte (soma de
	// PagamentoFaturaCartao ativos), nunca de um campo persistido em
	// FaturaCartao - por isso todo chamador precisa informa-lo explicitamente
	// (nunca zero por omissao, sob risco de exibir saldo devido incorreto).
	public static FaturaCartaoResponse from(ResultadoFatura resultado, BigDecimal valorPago) {
		FaturaCartao fatura = resultado.fatura();
		BigDecimal valorDevido = fatura.getValorDevido();
		BigDecimal saldoDevido = valorDevido.subtract(valorPago);
		return new FaturaCartaoResponse(fatura.getId(), fatura.getCartaoPrincipal().getId(),
				fatura.getCompetencia(), fatura.getPeriodoInicial(), fatura.getPeriodoFinal(),
				fatura.getDataFechamento(), fatura.getDataVencimento(), fatura.getValorTotal(),
				fatura.getSaldoFinanciadoAnterior(), fatura.getJuros(), fatura.getMulta(), valorDevido, valorPago,
				saldoDevido, fatura.getStatus(), fatura.getFechadoEm(), fatura.getVersao(),
				resultado.parcelas().stream().map(ParcelaFaturaCartaoResponse::from).toList());
	}
}
