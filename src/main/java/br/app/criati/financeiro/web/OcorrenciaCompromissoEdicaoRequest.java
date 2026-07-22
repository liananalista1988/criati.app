package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OcorrenciaCompromissoEdicaoRequest(
		@NotBlank(message = "Descricao e obrigatoria") String descricao,
		@NotNull(message = "Categoria e obrigatoria") UUID categoriaId,
		@NotNull(message = "Pessoa financeira e obrigatoria") UUID pessoaFinanceiraId,
		UUID parteFinanceiraId,
		UUID contaPrevistaId,
		BigDecimal valorPrevisto,
		@NotNull(message = "Valor principal e obrigatorio") BigDecimal valorPrincipal,
		@NotNull(message = "Vencimento e obrigatorio") LocalDate vencimento,
		LocalDate dataRecebimentoCobranca,
		BigDecimal juros,
		BigDecimal multa,
		BigDecimal desconto,
		String observacao) {
}
