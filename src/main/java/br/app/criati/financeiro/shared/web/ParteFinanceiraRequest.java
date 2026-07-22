package br.app.criati.financeiro.shared.web;

import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ParteFinanceiraRequest(
		@NotBlank(message = "Nome e obrigatorio")
		@Size(max = 150, message = "Nome deve possuir no maximo 150 caracteres")
		String nome,
		@NotNull(message = "Tipo e obrigatorio") TipoParteFinanceira tipo,
		@Size(max = 40, message = "Documento informado e muito longo") String documento,
		@Size(max = 100, message = "Apelido deve possuir no maximo 100 caracteres") String apelido,
		@Size(max = 500, message = "Observacao deve possuir no maximo 500 caracteres") String observacao) {
}
