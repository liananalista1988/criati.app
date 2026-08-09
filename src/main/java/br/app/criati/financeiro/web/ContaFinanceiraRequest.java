package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.app.criati.shared.enums.TipoContaFinanceira;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContaFinanceiraRequest(
		@NotBlank(message = "Nome e obrigatorio") @Size(max = 150) String nome,
		@NotNull(message = "Titular e obrigatorio") UUID titularId,
		UUID instituicaoId,
		@NotNull(message = "Tipo e obrigatorio") TipoContaFinanceira tipo,
		@NotBlank(message = "Moeda e obrigatoria") @Pattern(regexp = "BRL", message = "Moeda deve ser BRL") String moeda,
		@NotNull(message = "Saldo inicial e obrigatorio") BigDecimal saldoInicial,
		@NotNull(message = "Data do saldo inicial e obrigatoria") LocalDate dataSaldoInicial,
		@NotNull(message = "Configuracao de conciliacao e obrigatoria") Boolean permiteConciliacao,
		@Size(max = 20) String agenciaBancaria,
		@Size(max = 30) String numeroContaBancaria,
		@Size(max = 5) String digitoContaBancaria) {
}
