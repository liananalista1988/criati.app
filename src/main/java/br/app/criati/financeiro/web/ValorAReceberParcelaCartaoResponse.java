package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.ValorAReceberParcelaCartao;
import br.app.criati.shared.enums.SituacaoValorAReceberCompraCartao;
import br.app.criati.shared.enums.StatusValorAReceberCompraCartao;

public record ValorAReceberParcelaCartaoResponse(UUID id, UUID compraId, UUID parcelaId, int numero, int totalParcelas,
		BigDecimal valorTotal, LocalDate vencimento, LocalDate dataPrometida, LocalDate dataEfetivaRecebimento,
		BigDecimal valorRecebido, BigDecimal saldoPendente, StatusValorAReceberCompraCartao status,
		SituacaoValorAReceberCompraCartao situacao, boolean atrasada, long diasEmAtraso, OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm) {

	public static ValorAReceberParcelaCartaoResponse from(ValorAReceberParcelaCartao v) {
		LocalDate hoje = LocalDate.now();
		return new ValorAReceberParcelaCartaoResponse(v.getId(), v.getParcela().getCompra().getId(),
				v.getParcela().getId(), v.getParcela().getNumero(), v.getParcela().getTotalParcelas(),
				v.getValorTotal(), v.getVencimento(), v.getDataPrometida(), v.getDataEfetivaRecebimento(),
				v.getValorRecebido(), v.getSaldoPendente(), v.getStatus(), v.getSituacao(hoje), v.estaAtrasada(hoje),
				v.diasEmAtraso(hoje), v.getCriadoEm(), v.getAtualizadoEm());
	}
}
