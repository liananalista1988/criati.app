package br.app.criati.financeiro.web;

import java.math.BigDecimal;

import br.app.criati.shared.enums.TipoContaFinanceira;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContaFinanceiraRequest(
		@NotBlank(message = "Nome e obrigatorio") String nome,
		@NotNull(message = "Tipo e obrigatorio") TipoContaFinanceira tipo,
		@NotNull(message = "Saldo inicial e obrigatorio") BigDecimal saldoInicial) {
}
