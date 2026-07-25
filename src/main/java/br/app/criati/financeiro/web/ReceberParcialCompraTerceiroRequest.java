package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.app.criati.shared.enums.FormaPagamentoLancamento;
import jakarta.validation.constraints.NotNull;

public record ReceberParcialCompraTerceiroRequest(
		@NotNull(message = "Conta e obrigatoria") UUID contaId,
		@NotNull(message = "Valor e obrigatorio") BigDecimal valor,
		@NotNull(message = "Data de ressarcimento e obrigatoria") LocalDate dataRessarcimento,
		FormaPagamentoLancamento formaPagamento,
		String observacao) {
}
