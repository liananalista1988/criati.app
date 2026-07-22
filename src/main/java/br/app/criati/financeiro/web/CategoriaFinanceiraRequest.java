package br.app.criati.financeiro.web;

import java.util.UUID;

import br.app.criati.shared.enums.TipoFinanceiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoriaFinanceiraRequest(
		@NotBlank(message = "Nome e obrigatorio") @Size(max = 150) String nome,
		@Size(max = 500) String descricao,
		UUID categoriaPaiId,
		@NotNull(message = "Natureza e obrigatoria") TipoFinanceiro tipo,
		@PositiveOrZero(message = "Ordem deve ser maior ou igual a zero") Integer ordem,
		Boolean permiteOrcamento) {
}
