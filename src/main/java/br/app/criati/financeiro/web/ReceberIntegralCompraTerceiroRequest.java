package br.app.criati.financeiro.web;

import java.time.LocalDate;
import java.util.UUID;

import br.app.criati.shared.enums.FormaPagamentoLancamento;
import jakarta.validation.constraints.NotNull;

public record ReceberIntegralCompraTerceiroRequest(
		@NotNull(message = "Conta e obrigatoria") UUID contaId,
		@NotNull(message = "Data de ressarcimento e obrigatoria") LocalDate dataRessarcimento,
		FormaPagamentoLancamento formaPagamento,
		String observacao) {
}
