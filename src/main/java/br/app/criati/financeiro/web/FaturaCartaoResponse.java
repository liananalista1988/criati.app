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
		StatusFaturaCartao status,
		OffsetDateTime fechadoEm,
		long versao,
		List<ParcelaFaturaCartaoResponse> parcelas) {

	public static FaturaCartaoResponse from(ResultadoFatura resultado) {
		FaturaCartao fatura = resultado.fatura();
		return new FaturaCartaoResponse(fatura.getId(), fatura.getCartaoPrincipal().getId(),
				fatura.getCompetencia(), fatura.getPeriodoInicial(), fatura.getPeriodoFinal(),
				fatura.getDataFechamento(), fatura.getDataVencimento(), fatura.getValorTotal(),
				fatura.getStatus(), fatura.getFechadoEm(), fatura.getVersao(),
				resultado.parcelas().stream().map(ParcelaFaturaCartaoResponse::from).toList());
	}
}
