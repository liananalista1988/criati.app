package br.app.criati.financeiro.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstituicaoFinanceiraRequest(
		@NotBlank(message = "Nome e obrigatorio") @Size(max = 150) String nome,
		@Size(max = 20) String codigo) {
}
