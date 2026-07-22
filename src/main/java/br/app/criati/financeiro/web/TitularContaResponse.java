package br.app.criati.financeiro.web;

import java.util.UUID;

import br.app.criati.financeiro.shared.model.PessoaFinanceira;

public record TitularContaResponse(UUID id, String nome) {

	public static TitularContaResponse from(PessoaFinanceira pessoa) {
		return new TitularContaResponse(pessoa.getId(), pessoa.getNome());
	}
}
