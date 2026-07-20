package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LancamentoFinanceiroEdicaoRequest(
		@NotNull(message = "Conta e obrigatoria") UUID contaId,
		@NotNull(message = "Categoria e obrigatoria") UUID categoriaId,
		@NotBlank(message = "Descricao e obrigatoria") String descricao,
		@NotNull(message = "Valor e obrigatorio") BigDecimal valor,
		@NotNull(message = "Data de competencia e obrigatoria") LocalDate dataCompetencia,
		String observacao) {
}
