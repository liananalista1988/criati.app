package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.app.criati.financeiro.model.ParcelaCompraCartao;
import br.app.criati.shared.enums.StatusParcelaCartao;

public record ParcelaFaturaCartaoResponse(
		UUID id,
		UUID compraId,
		UUID cartaoUtilizadoId,
		int numero,
		int totalParcelas,
		BigDecimal valor,
		LocalDate competencia,
		StatusParcelaCartao status) {

	static ParcelaFaturaCartaoResponse from(ParcelaCompraCartao parcela) {
		return new ParcelaFaturaCartaoResponse(parcela.getId(), parcela.getCompra().getId(),
				parcela.getCompra().getCartao().getId(), parcela.getNumero(), parcela.getTotalParcelas(),
				parcela.getValor(), parcela.getCompetencia(), parcela.getStatus());
	}
}
