package br.app.criati.empresa.web;

import java.util.UUID;

import br.app.criati.shared.enums.StatusCadastro;

public record EmpresaResponse(
		UUID id,
		String nome,
		String nomeFantasia,
		String cnpj,
		StatusCadastro status) {
}
