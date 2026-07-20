package br.app.criati.financeiro.web;

import br.app.criati.shared.enums.TipoFinanceiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoriaFinanceiraRequest(
		@NotBlank(message = "Nome e obrigatorio") String nome,
		@NotNull(message = "Tipo e obrigatorio") TipoFinanceiro tipo) {
}
