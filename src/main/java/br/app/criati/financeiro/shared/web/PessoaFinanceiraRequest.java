package br.app.criati.financeiro.shared.web;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PessoaFinanceiraRequest(
		@NotBlank(message = "Nome e obrigatorio")
		@Size(max = 150, message = "Nome deve possuir no maximo 150 caracteres")
		String nome,
		@Size(max = 100, message = "Apelido deve possuir no maximo 100 caracteres")
		String apelido,
		UUID usuarioId) {
}
