package br.app.criati.empresa.web;

import jakarta.validation.constraints.NotBlank;

public record CadastrarEmpresaRequest(
		@NotBlank(message = "Nome e obrigatorio") String nome,
		String nomeFantasia,
		@NotBlank(message = "CNPJ e obrigatorio") String cnpj) {
}
