package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompraTerceiroRequest(
		@NotNull UUID cartaoId,
		@NotNull UUID pessoaResponsavelId,
		@NotNull UUID categoriaId,
		@NotNull(message = "Pessoa responsavel pelo ressarcimento e obrigatoria") UUID parteFinanceiraId,
		@NotBlank @Size(max = 200) String descricao,
		@NotNull LocalDate dataCompra,
		@NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal valorTotal,
		@NotNull @Min(1) Integer quantidadeParcelas,
		@Size(max = 500) String observacao) {
}
